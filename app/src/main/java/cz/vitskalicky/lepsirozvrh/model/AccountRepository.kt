package cz.vitskalicky.lepsirozvrh.model

import androidx.lifecycle.LiveData
import com.fasterxml.jackson.module.kotlin.readValue
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.SharedPrefsKt
import cz.vitskalicky.lepsirozvrh.bakaAPI.login.*
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.RozvrhWebservice
import cz.vitskalicky.lepsirozvrh.database.RozvrhDatabase
import cz.vitskalicky.lepsirozvrh.model.AccountRepository.LoginResultStatus.*
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import kotlin.math.min


class AccountRepository(val app: MainApplication) {
    private val db: RozvrhDatabase = app.rozvrhDb
    private val dao = db.accountDao()

    private val accountLDs: LiveData<Map<Long, Account>> = dao.loadAllAccountsLDMap()
    // the accounts are automatically updated
    private val tokenAuthenticators: MutableMap<Long, TokenAuthenticator> = HashMap()
    private val retrofits: MutableMap<Long, Retrofit> = HashMap()

    private val rozvrhWebservices: MutableMap<Long, RozvrhWebservice> = HashMap()
    private val userWebservices: MutableMap<Long, UserWebservice> = HashMap()

    init {
        accountLDs.observe(app){
            val toRemove = HashSet(tokenAuthenticators.keys);
            for (account in it.values){
                toRemove.remove(account.id)
                tokenAuthenticators.getOrPut(account.id){TokenAuthenticator(app, account)}
                    .account = account
            }
            for (id in toRemove){
                tokenAuthenticators[id]?.account = null
                tokenAuthenticators.remove(id)
            }
        }
    }
    /**
     * Returns a new retrofit which does not inject login token.
     */
    fun getUnloggedRetrofit(baseUrl: String): Retrofit {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(JacksonConverterFactory.create(MainApplication.objectMapper))
                .client(client)
                .build()

    }

    /** Returns an instance of retrofit (cached for each account) or `null` if it could not be created (likely because the URL is invalid, see [createRetrofit]).
     * Also, the [account] must have a valid id and be stored in database, otherwise the credentials will not be renewed,
     * may be replaced by credentials of a different account with the same id or other bad things might happen
     * */
    fun getRetrofit(account: Account): Retrofit? {
        var retrofit = retrofits[account.id];
        if (retrofit == null){
            retrofit = createRetrofit(account)?.also { retrofits[account.id] = it }
        }
        return retrofit;
    }

    /** Returns an instance of webservice (cached for each account) or `null` if corresponding retrofit could not be created (see [getRetrofit])*/
    fun getRozvrhWebservice(account: Account): RozvrhWebservice?{
        var webservice = rozvrhWebservices[account.id]
        if (webservice == null){
            webservice = getRetrofit(account)?.create(RozvrhWebservice::class.java)?.also { rozvrhWebservices[account.id] = it }
        }
        return webservice
    }

    /** Returns an instance of webservice (cached for each account) or `null` if corresponding retrofit could not be created (see [getRetrofit])*/
    fun getUserWebservice(account: Account): UserWebservice?{
        var webservice = userWebservices[account.id]
        if (webservice == null){
            webservice = getRetrofit(account)?.create(UserWebservice::class.java)?.also { userWebservices[account.id] = it }
        }
        return webservice
    }

    fun getAccountsLD(): LiveData<List<Account>> = dao.loadAllAccountsLD()
    fun getAccountLD(id: Long): LiveData<Account?> = dao.loadAccountLD(id)

    /**
     * Does what its name suggest.
     *
     * If [refreshTokens] is `true`, [Account.accessToken] and [Account.refreshToken] will be refreshed if expired (and
     * if internet connection available) */
    suspend fun getAccount(id: Long, refreshTokens: Boolean = false): Account?{
        if (refreshTokens || dao.refreshRequired(id) == true){
            refreshToken(id, force = false)
        }
        return dao.loadAccount(id);
    }

    /**
     * If [account] has expired access token, refresh it and return [Account] with fresh tokens. If access token is not
     * expired, simply return [account].
     */
    suspend fun tryRefresh(account: Account): Account {
        return if (account.isAccessExpired()){
            getAccount(account.id, refreshTokens = true) ?: account /*the account may have been deleted from database*/
        }else{
            account
        }
    }

    private suspend fun handleException(e: Exception, whichAPI: String, url: String = "", isUrlManual: Boolean = false): LoginResultStatus {
        when (e) {
            is HttpException -> {
                //probably could not parse the response
                //parse error body
                var parseException: IOException? = null
                var rawBody: String? = null;
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

    /** Token will also be updated in database */
    suspend fun refreshToken(id: Long, force: Boolean = true): LoginResult {
        val account = dao.loadAccount(id) ?: return WRONG_LOGIN.fail();
        if (!force && !account.isAccessExpired() && !account.requireRefresh){
            return SUCCESS.ok(account);
        }
//        val refreshToken: String = sprefs.getString(SharedPrefs.REFRESH_TOKEN, null)?.takeUnless { it.isBlank() } ?: return WRONG_LOGIN

        try {
            val retrofit: Retrofit = createRetrofitNoAuth(account.serverUrl);
            val webservice: LoginWebservice = retrofit.create(LoginWebservice::class.java)

            val response: LoginResponse = webservice.refreshLogin(account.refreshToken)

//            sprefs.edit().apply {
//                putString(SharedPrefs.REFRESH_TOKEN, response.refresh_token)
//                putString(SharedPrefs.ACCEESS_TOKEN, response.access_token)
//                putString(SharedPrefs.ACCESS_EXPIRES, LocalDateTime.now().plusSeconds(response.expires_in).toString(ISODateTimeFormat.dateTime()))
//            }.apply()
            var updatedAccount = account.copy(
                refreshToken = response.refresh_token,
                accessToken = response.access_token,
                accessExpires = DateTime.now().plusSeconds(response.expires_in)
            )

            //check if user info should be refreshed
            if (account.semesterEnd == null || account.semesterEnd.isBeforeNow || account.requireRefresh){
                updatedAccount = refreshUserInfo(account).account ?: updatedAccount
            }
            dao.updateAccount(updatedAccount)

            return SUCCESS.ok(updatedAccount)
        }catch (e: HttpException){
            return handleException(e, "login").fail()
        }catch (e: IOException){
            return handleException(e, "login").fail()
        }//todo catch invalid url
    }

    suspend fun addAccount(url: String, username: String, password: String, isUrlManual: Boolean): LoginResult {
        val url: String = unifyUrl(url)
        try {
            val webservice = getUnloggedRetrofit(url).create(LoginWebservice::class.java)

            val response: LoginResponse = webservice.firstLogin(username, password)
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
                clazz = Class("","",""),
                requireRefresh = true
            )
            // if you ever change this logic, don't forget to change in migration code too
            val userWebservice = createRetrofit(partialAccount, connectDb = false)?.create(UserWebservice::class.java)
            val userResponse = userWebservice?.getUser()
            if (userResponse == null) {
                return UNEXPECTED_RESPONSE.fail()
            }
            val semesterEnd: DateTime? = userResponse.settingModules?.common?.actualSemester?.to?.let {
                try {
                    ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(it)
                }catch (e: IllegalArgumentException){
                    e.printStackTrace()
                    null
                }
            }
            val partialAccount2 = Account(
                url, username, response.access_token, response.refresh_token,
                accessExpires = DateTime.now().plusSeconds(response.expires_in),
                schoolName = userResponse.schoolOrganizationName ?: "",
                fullName = userResponse.fullName ?: "",
                userType = userResponse.userType ?: "",
                userTypeText = userResponse.userTypeText ?: "",
                semesterEnd = semesterEnd,
                userUID = userResponse.userUID ?: "",
                clazz = userResponse.clazz?.run { Class(id?:"", abbrev?:"", name?:"") } ?: Class("","",""),
                requireRefresh = false,
            )
            val accountId = dao.insertAccount(partialAccount2)
            val account = dao.loadAccount(accountId) //todo test if the id from insert account matches account's id

            if (account == null){
                throw RuntimeException("hmm, the id did not match")
                //todo resolve properly
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

    suspend fun refreshUserInfo(account: Account): LoginResult {

        val userWebservice: UserWebservice = getUserWebservice(account)?: return UNREACHABLE.fail()
        try {
            val userResponse: UserResponse = userWebservice.getUser()

            val semesterEnd: DateTime? = userResponse.settingModules?.common?.actualSemester?.to?.let {
                try {
                    ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(it)
                }catch (e: IllegalArgumentException){
                    e.printStackTrace()
                    null
                }
            }
            val modifiedAccount = account.copy(
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
                requireRefresh = false
            )
            return SUCCESS.ok(modifiedAccount)
        }catch (e: HttpException){
            return handleException(e, "user").fail()
        }catch (e: IOException){
            return handleException(e, "user").fail()
        }
    }

    /**
     * Logs out user (deletes credentials)
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun logout(accountId: Long) {
        withContext(NonCancellable) {
            dao.deleteAccountById(accountId)
            app.rozvrhStatusStore.clear()
            rozvrhWebservices.remove(accountId)
            userWebservices.remove(accountId)
            retrofits.remove(accountId)
            tokenAuthenticators[accountId]?.account = null
            tokenAuthenticators.remove(accountId)

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

    suspend fun switchToAccount(accountId: Long){
        if (dao.accountExists(accountId)){
            app.prefs.edit { putLong(PrefsConsts.ACTIVE_ACCOUNT_ID, accountId) }
        }else{
            app.prefs.edit { remove(PrefsConsts.ACTIVE_ACCOUNT_ID) }
        }
    }

//    /**
//     * Checks if user is logged in or has seen the welcome screen (where crash reports are
//     * enabled/disabled), the starts the corresponding activity (if it isn't already started).
//     * `finish()` **won't** be called on the current activity.
//     *
//     * @return An activity which is being started or `null` if no activity will be started.
//     */
//    fun checkLogin(currentActivity: Activity): KClass<out Activity>? { //todo
//        val ctx = currentActivity
//        val seenWelcome = SharedPrefs.containsPreference(app, R.string.PREFS_SEND_CRASH_REPORTS)
//        if (!seenWelcome && currentActivity !is WelcomeActivity) {
//            val intent = Intent(ctx, WelcomeActivity::class.java)
//            ctx.startActivity(intent)
//            return WelcomeActivity::class
//        }
//        if (false /*!isLoggedIn()*/ && currentActivity !is LoginActivity) {
//            val intent = Intent(ctx, LoginActivity::class.java)
//            ctx.startActivity(intent)
//            return LoginActivity::class
//        }
//        if (currentActivity !is MainActivity) {
//            val intent = Intent(ctx, MainActivity::class.java)
//            ctx.startActivity(intent)
//            return MainActivity::class
//        }
//        return null
//    }

    /**
     * Returns an instance of TokenAuthenticator for the given account. If [connectDb] is `true`, it is stored in a cache and its tokens are
     * automatically updated according to database. If [connectDb] is `false` the database will not be touched.
     */
    private fun getTokenAuthenticator(account: Account, connectDb: Boolean = true): TokenAuthenticator {
//        var account: Account? = tokenAuthenticators[accountId]?.account
//        if (account == null){ //token authenticator does not exist
//            account = getAccount(accountId) ?: return null
//            // function above is suspending, tokenAuthenticators mich have changed
//        }
        if (!connectDb) return TokenAuthenticator(app, account, connectDb = false)
        return tokenAuthenticators.getOrPut(account.id) {
            TokenAuthenticator(app, account)
        }
    }

    private fun createRetrofitNoAuth(url: String): Retrofit{
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
        return Retrofit.Builder()
            .baseUrl(url) //todo test invalid url
            .addConverterFactory(JacksonConverterFactory.create(MainApplication.objectMapper))
            .client(client)
            .build()
    }

    /**
     * Creates new instance of Retrofit with the account's URL and login credentials. If the URL is invalid, `null` is returned.
     *
     * If [connectDb] is `true` the credentials will automatically be renewed and stored in database, and if `false`, the DB will not be touched.
     * */
    private fun createRetrofit(account: Account, connectDb: Boolean = true): Retrofit? {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val tokenAuthenticator = getTokenAuthenticator(account, connectDb) ?: return null
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

    companion object{
        /**
         * Removes /next/login.aspx
         */
        private fun unifyUrl(url: String): String {
            var url = url
            if (url.endsWith(".aspx")) url = url.substring(0, url.length - 5)
            if (url.endsWith("login")) {
                url = url.substring(0, url.length - 5)
                if (url.endsWith("next/")) url = url.substring(0, url.length - 5)
            }
            if (!url.endsWith("/")) url += "/"
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                url = "https://$url"
            }
            return url
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

public open class LoginException(message: String?): RuntimeException(message)
public class LoginRequiredException(): LoginException("You need to log in first to perform this action")

