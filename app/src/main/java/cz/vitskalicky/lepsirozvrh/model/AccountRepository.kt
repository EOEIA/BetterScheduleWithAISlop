package cz.vitskalicky.lepsirozvrh.model

import android.util.Log
import androidx.lifecycle.LiveData
import com.fasterxml.jackson.module.kotlin.readValue
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.bakaAPI.login.*
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.RozvrhWebservice
import cz.vitskalicky.lepsirozvrh.database.RozvrhDatabase
import cz.vitskalicky.lepsirozvrh.model.AccountRepository.LoginResultStatus.*
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * All is thread-safe
 * */
class AccountRepository(val app: MainApplication) {
    private val db: RozvrhDatabase = app.rozvrhDb
    private val dao = db.accountDao()

    private val rozvrhWebservices: ConcurrentHashMap<Long, RozvrhWebservice> = ConcurrentHashMap()
    private val userWebservices: ConcurrentHashMap<Long, UserWebservice> = ConcurrentHashMap()

    /** Mutex which must be locked when updating account details to avoid pointless concurrent refreshed of it. It is
     * common to all accounts. */
    private val accountDetailsLock = Mutex()
    /** Whenever mutating an account, lock a corresponding lock to prevent race condition on refresh tokens */
    private val locks: ConcurrentHashMap<Long, Mutex> = ConcurrentHashMap()

    /**
     * Returns a new retrofit which does not inject login token.
     */
    private fun createRetrofitNoAuth(baseUrl: String): Retrofit {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(JacksonConverterFactory.create(MainApplication.objectMapper))
                .client(client)
                .build()

    }

    /**
     * Creates new instance of Retrofit with the account's URL and login credentials. If the URL is invalid, `null` is returned.
     *
     * Not thread-safe
     * */
    private fun createRetrofit(account: Account): Retrofit? {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val tokenAuthenticator = TokenAuthenticator({invalidateToken: String? -> this.refreshAccount(account.id, invalidateToken) })
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(tokenAuthenticator)
            .authenticator(tokenAuthenticator)
            .build()
        return try {
            Retrofit.Builder()
                .baseUrl(account.serverUrl)
                .addConverterFactory(JacksonConverterFactory.create(MainApplication.objectMapper))
                .client(client)
                .build()
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Returns an instance of webservice (cached for each account) or `null` if it could not be created.
     *
     * thread-safe
     * */
    fun getRozvrhWebservice(account: Account): RozvrhWebservice?{
        return rozvrhWebservices[account.id] ?: // try to simply get it first. if that fails, use getOrPut
            rozvrhWebservices.safeGetOrPut(account.id, createRetrofit(account)?.create(RozvrhWebservice::class.java) ?: return null)
    }

    /** Returns an instance of webservice (cached for each account) or `null` if it could not be created.
     * Thread-safe
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getUserWebservice(account: Account): UserWebservice?{
        return userWebservices[account.id] ?: // try to simply get it first. if that fails, use getOrPut
            userWebservices.safeGetOrPut(account.id, createRetrofit(account)?.create(UserWebservice::class.java) ?: return null)
    }

    fun getAccountsLD(): LiveData<List<Account>> = dao.loadAllAccountsLD()
    fun getAccountLD(id: Long): LiveData<Account?> = dao.loadAccountLD(id)

    /**
     * Return account for the supplied [id]. If the access token is expired it will be refreshed. Returns `null` if account is not found.
     */
    suspend fun getAccount(id: Long): Account? {
        return refreshAccount(id).account ?: dao.loadAccount(id)
    }

    private suspend fun handleException(e: Exception, whichAPI: String, url: String = "", isUrlManual: Boolean = false): LoginResultStatus {
        when (e) {
            is HttpException -> {
                //probably could not parse the response
                //parse error body
                var parseException: IOException? = null
                var rawBody: String? = null
                val errorBody: Map<String, Any>? = e.response()?.errorBody()?.let {
                    withContext(Dispatchers.IO) {
                        try {
                            val str = it.string()
                            rawBody = str
                            MainApplication.objectMapper.readValue(str)
                        } catch (e: IOException) {
                            parseException = e
                            null
                        }
                    }
                }
                if (e.code() == 400 && errorBody?.get("error") == "invalid_grant") {
                    //wrong password username or refresh token
                    return WRONG_LOGIN
                }
                //TODO Expired refresh token
                if (isUrlManual){
                    // do not report if user has entered the url manually
                    return UNEXPECTED_RESPONSE
                }
                //avoid reporting 404s with html response
                if (e.code() == 404 && rawBody?.substring(0, min(100, rawBody?.length ?: 0))?.lowercase()?.contains("html") == true){
                    return UNREACHABLE
                }
                //avoid reporting Internal server errors
                if (e.code() in 500..599 && rawBody?.substring(0, min(100, rawBody?.length ?: 0))?.lowercase()?.contains("html") == true){
                    return UNREACHABLE
                }
                //unexpected - report
                app.sendReport(IOException("Unexpected $whichAPI API response. Url: \'$url\'. Raw response: \'${rawBody}\'. Response code: \'${e.code()}\'. Message of exception while parsing (which is also set as cause of this exception): \'${parseException?.message}\'", parseException))
                return UNEXPECTED_RESPONSE
            }
            is IOException ->
                return UNREACHABLE
            is IllegalArgumentException -> {
                //malformed url
                return UNREACHABLE
            }
            else -> {
                throw e
            }
        }
    }

    /**
     * Refresh access token and account info if needed and return a fresh account. All is loaded from db and result is saved there. It is thread-safe.
     * Note that if refresh fails (e.g. no internet), an error is returned. But in that case you can still get
     * the known-to-be-not-fresh account from db.
     *
     * If access token is expired or is equal to [invalidateToken], it gets refreshed.
     * */
    private suspend fun refreshAccount(id: Long, invalidateToken: String? = null): LoginResult{
        val lock = locks.safeGetOrPut(id, Mutex())
        lock.withLock {
            Log.d(TAG, "Refreshing account $id")
            var account = dao.loadAccount(id) ?: return WRONG_LOGIN.fail()
            Log.d(TAG, "Refreshing account $id: Current access token has length ${account.accessToken.length} and refresh token has length ${account.refreshToken.length}")
            if (account.isAccessExpired() || account.accessToken == invalidateToken){
                Log.d(TAG, "Refreshing account $id: access token is expired")
                val res = doRefresh(account)
                Log.d(TAG, "Refreshing account $id: access token refresh is ${res.status}. New access token has length ${account.accessToken.length} and refresh token has length ${account.refreshToken.length}")
                when (res.status){
                    SUCCESS -> {
                        account = res.account!!
                        dao.updateAccount(account)
                    }
                    WRONG_LOGIN -> {
                        account = account.copy(accessToken = "", refreshToken = "", accessExpires = DateTime.now().minusDays(1)) // blank the tokens since we know they are invalid
                        dao.updateAccount(account)
                    }
                    else -> {}
                }
                return res
            }else{
                Log.d(TAG, "Refreshing account $id: access token not expired")
                return SUCCESS.ok(account)
            }
        }
    }

    /** Get a new access token for the given account. (even if it is not expired yet)
     * */
    private suspend fun doRefresh(account: Account): LoginResult {
        if (account.refreshToken.isBlank()){
            return WRONG_LOGIN.fail()
        }

        try {
            val retrofit: Retrofit = createRetrofitNoAuth(account.serverUrl)
            val webservice: LoginWebservice = retrofit.create(LoginWebservice::class.java)

            val response: LoginResponse = webservice.refreshLogin(account.refreshToken)

            val updatedAccount = account.copy(
                refreshToken = response.refresh_token,
                accessToken = response.access_token,
                accessExpires = DateTime.now().plusSeconds(response.expires_in)
            )

            return SUCCESS.ok(updatedAccount)
        }catch (e: HttpException){
            val res = handleException(e, "refresh").fail()
            return res
        }catch (e: IOException){
            return handleException(e, "refresh").fail()
        }
    }

    private suspend fun doFirstLogin(url: String, username: String, password: String, isUrlManual: Boolean): LoginResult {
        @Suppress("NAME_SHADOWING")
        val url: String = unifyUrl(url)
        try{
            val webservice = createRetrofitNoAuth(url).create(LoginWebservice::class.java)

            val response: LoginResponse = webservice.firstLogin(username, password) //throws on invalid credentials
            //handle success
            val partialAccount = Account(
                url, username, response.access_token, response.refresh_token,
                accessExpires = DateTime.now().plusSeconds(response.expires_in),
                schoolName = "",
                fullName = "",
                userType = "",
                userTypeText = "",
                semesterEnd = null,
                userUID = "",
                clazz = Class("", "", ""),
                requireRefresh = true
            )

            return SUCCESS.ok(partialAccount)
        }catch (e: HttpException){
            return handleException(e, "login", url, isUrlManual).fail()
        }catch (e: IOException){
            return handleException(e, "login", url, isUrlManual).fail()
        }catch (e: IllegalArgumentException){
            return handleException(e, "login", url, isUrlManual).fail()
        }
    }

    /** Updates details for an account, saves the updated info into db and also returns it. Returns old account data
     * in case of a failure or null if id is invalid. Thread-safe, but do not call with [locks] locked. */
    suspend fun refreshAccountDetails(accountId: Long, force: Boolean = false): Account? {
        accountDetailsLock.withLock {
            // if you ever change this logic, don't forget to change in migration code too
            val oldAccount: Account = dao.loadAccount(accountId) ?: return null
            if (!(force || oldAccount.requireRefresh || oldAccount.semesterEnd == null || oldAccount.semesterEnd.isBeforeNow)){ // check if refresh is needed
                return oldAccount
            }
            try {
                val userWebservice = getUserWebservice(oldAccount)
                val userResponse = userWebservice?.getUser() ?: return oldAccount // this throws on error

                val semesterEnd: DateTime? = userResponse.settingModules?.common?.actualSemester?.to?.let {
                    try {
                        ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(it)
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                        null
                    }
                }

                // Also lock account lock to prevent overriding refresh tokens
                val newAccount: Account = locks.safeGetOrPut(accountId, Mutex()).withLock {
                    @Suppress("NAME_SHADOWING")
                    val oldAccount = dao.loadAccount(accountId) ?: return null // refresh tokens might have changed
                    val newAccount = oldAccount.copy(
                        schoolName = userResponse.schoolOrganizationName ?: "",
                        fullName = userResponse.fullName ?: "",
                        userType = userResponse.userType ?: "",
                        userTypeText = userResponse.userTypeText ?: "",
                        semesterEnd = semesterEnd,
                        userUID = userResponse.userUID ?: "",
                        clazz = userResponse.clazz?.run {
                            Class(
                                id ?: "",
                                abbrev ?: "",
                                name ?: ""
                            )
                        } ?: Class("", "", ""),
                        requireRefresh = false,
                    )
                    dao.updateAccount(newAccount)
                    newAccount
                }
                return newAccount
            }catch (e: HttpException){
                return oldAccount
            }catch (e: IOException){
                return oldAccount
            }catch (e: IllegalArgumentException){
                return oldAccount
            }
        }
    }

    /** Adds an account and returns it. */
    suspend fun addAccount(url: String, username: String, password: String, isUrlManual: Boolean): LoginResult {
        @Suppress("NAME_SHADOWING")
        val url: String = unifyUrl(url)
        try {
            val loginResponse = doFirstLogin(url, username, password, isUrlManual)
            val partialAccount: Account = loginResponse.account ?: return loginResponse

            val accountId = dao.insertAccount(partialAccount)
            val partialAccount2 = dao.loadAccount(accountId)
                ?: throw RuntimeException("hmm, the id did not match")

            // this loads other details for the account
            val account: Account = refreshAccountDetails(accountId) ?: partialAccount2 // actually, it should never be null, since we just added the account to db

            // if you ever change this logic, don't forget to change in migration code too
            // If previous notification account has been logged out or no account has been logged in before, enable them for the account
            //try to enable notification
            if ((
                    app.prefs.long(PrefsConsts.NOTIFICATION_ACCOUNT) == PermanentNotification.ACCOUNT_NOTIFICATION_LOGGED_OUT //logged out
                    || !app.prefs.contains(PrefsConsts.NOTIFICATION_ACCOUNT // or never logged in
                ))
                && PermanentNotification.areNotificationEnabled(app) // and we have permission to show notification
                ){
                app.prefs.putOne(PrefsConsts.NOTIFICATION_ACCOUNT, accountId) //enable notification for the newly added account
            }

            return SUCCESS.ok(account)
        }catch (e: HttpException){
            return handleException(e, "login", url, isUrlManual).fail()
        }catch (e: IOException){
            return handleException(e, "login", url, isUrlManual).fail()
        }catch (e: IllegalArgumentException){
            return handleException(e, "login", url, isUrlManual).fail()
        }
    }

    /**
     * Logs out user (deletes credentials)
     */
    suspend fun logout(accountId: Long) {
        withContext(NonCancellable) {
            locks.safeGetOrPut(accountId, Mutex()).withLock {
                dao.deleteAccountById(accountId)
                app.rozvrhStatusStore.clear()
                rozvrhWebservices.remove(accountId)
                userWebservices.remove(accountId)

                if (app.prefs.long(PrefsConsts.ACTIVE_ACCOUNT_ID) == accountId){
                    app.prefs.edit { remove(PrefsConsts.ACTIVE_ACCOUNT_ID)}
                }
                if (app.prefs.long(PrefsConsts.NOTIFICATION_ACCOUNT) == accountId) {
                    app.prefs.edit { putLong(PrefsConsts.NOTIFICATION_ACCOUNT, PermanentNotification.ACCOUNT_NOTIFICATION_LOGGED_OUT) }
                    PermanentNotification.update(app, null, false, null,0)
                }

                //todo notification and widget cleanup
//            app.notificationState.offset = 0
//            PermanentNotification.update(null, 0, app)
//            WidgetProvider.updateAll(null, app)
            }
        }
    }

    suspend fun switchToAccount(accountId: Long){
        if (dao.accountExists(accountId)){
            app.prefs.edit { putLong(PrefsConsts.ACTIVE_ACCOUNT_ID, accountId) }
        }else{
            app.prefs.edit { remove(PrefsConsts.ACTIVE_ACCOUNT_ID) }
        }
    }

    companion object{
        private val TAG = AccountRepository::class.simpleName
        /**
         * Removes /next/login.aspx
         */
        private fun unifyUrl(url: String): String {
            @Suppress("NAME_SHADOWING")
            var url = url
            if (url.endsWith(".aspx")) url = url.substring(0, url.length - 5)
            if (url.endsWith("login")) {
                url = url.substring(0, url.length - 5)
                if (url.endsWith("next/")) url = url.substring(0, url.length - 5)
            }
            if (!url.endsWith("/")) url += "/"
            @Suppress("HttpUrlsUsage")
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                url = "https://$url"
            }
            return url
        }

        private fun<K: Any, V: Any> ConcurrentHashMap<K,V>.safeGetOrPut(key: K, value: V): V {
            return putIfAbsent(key, value) ?: value
        }
    }

    enum class LoginResultStatus{
        SUCCESS,
        UNREACHABLE,
        WRONG_LOGIN,
        UNEXPECTED_RESPONSE;
        fun ok(account: Account) = LoginResult(this, account)
        fun fail() = LoginResult(this, null)
    }
    data class LoginResult(
        val status: LoginResultStatus,
        val account: Account?
    )
}

open class LoginException(message: String?): RuntimeException(message)

