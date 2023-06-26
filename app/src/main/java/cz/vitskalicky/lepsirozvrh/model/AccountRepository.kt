package cz.vitskalicky.lepsirozvrh.model

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import com.fasterxml.jackson.module.kotlin.readValue
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.SharedPrefs
import cz.vitskalicky.lepsirozvrh.activity.LoginActivity
import cz.vitskalicky.lepsirozvrh.activity.MainActivity
import cz.vitskalicky.lepsirozvrh.activity.WelcomeActivity
import cz.vitskalicky.lepsirozvrh.bakaAPI.login.*
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.RozvrhWebservice
import cz.vitskalicky.lepsirozvrh.database.RozvrhDatabase
import cz.vitskalicky.lepsirozvrh.model.AccountRepository.LoginResult.*
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.widget.WidgetProvider
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import kotlin.math.min
import kotlin.reflect.KClass


class AccountRepository(val app: MainApplication) {
    private val db: RozvrhDatabase = app.rozvrhDb
    private val dao = db.accountDao()

    private val accountLDs: LiveData<Map<Int, Account>> = dao.loadAllAccountsLDMap()
    // the accounts are automatically updated
    private val tokenAuthenticators: MutableMap<Int, TokenAuthenticator> = HashMap()
    private val retrofits: MutableMap<Int, Retrofit> = HashMap()

    private val webservices: MutableMap<Int, RozvrhWebservice> = HashMap()

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

    /** Returns an instance of retrofit (cached for each account) or `null` if it could not be created (likely because the URL is invalid, see [createRetrofit]) */
    fun getRetrofit(account: Account): Retrofit? {
        var retrofit = retrofits[account.id];
        if (retrofit == null){
            retrofit = createRetrofit(account)?.also { retrofits[account.id] = it }
        }
        return retrofit;
    }

    /** Returns an instance of webservice (cached for each account) or `null` if corresponding retrofit could not be created (see [getRetrofit])*/
    fun getWebservice(account: Account): RozvrhWebservice?{
        var webservice = webservices[account.id]
        if (webservice == null){
            webservice = getRetrofit(account)?.create(RozvrhWebservice::class.java)?.also { webservices[account.id] = it }
        }
        return webservice
    }

    fun getAccountsLD(): LiveData<List<Account>> = dao.loadAllAccountsLD()
    fun getAccountLD(id: Int): LiveData<Account?> = dao.loadAccountLD(id)

    /**
     * Does what its name suggest.
     *
     * If [refreshTokens] is `true`, [Account.accessToken] and [Account.refreshToken] will be refreshed if expired (and
     * if internet connection available) */
    suspend fun getAccount(id: Int, refreshTokens: Boolean = false): Account?{
        if (refreshTokens){
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

    suspend fun handleException(e: Exception, whichAPI: String, url: String = "", isUrlManual: Boolean = false): LoginResult {
        when (e) {
            is HttpException -> {
                //probably could not parse the response
                //parse error body
                var parseException: IOException? = null
                var rawBody: String? = null;
                val errorBody: Map<String, Any>? = e.response()?.errorBody()?.let {
                    @Suppress("BlockingMethodInNonBlockingContext")
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

    /** Token will be updated in database */
    suspend fun refreshToken(id: Int, force: Boolean = true): LoginResult {
        val account = dao.loadAccount(id) ?: return WRONG_LOGIN;
        if (!force && !account.isAccessExpired()){
            return SUCCESS;
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
            val updatedAccount = account.copy(
                refreshToken = response.refresh_token,
                accessToken = response.access_token,
                accessExpires = DateTime.now().plusSeconds(response.expires_in)
            )
            dao.updateAccount(updatedAccount)

            //check if user info should be refreshed
            if (account.semesterEnd == null || account.semesterEnd.isBeforeNow){
                refreshUserInfo(account.id)
            }

            return SUCCESS
        }catch (e: HttpException){
            return handleException(e, "login")
        }catch (e: IOException){
            return handleException(e, "login")
        }//todo catch invalid url
    }

    suspend fun firstLogin(url: String, username: String, password: String, isUrlManual: Boolean): LoginResult {
        val url: String = unifyUrl(url)
        try {
            val webservice = getUnloggedRetrofit(url).create(LoginWebservice::class.java)

            val response: LoginResponse = webservice.firstLogin(username, password)
            //handle success

            sprefs.edit().apply {
                putString(SharedPrefs.REFRESH_TOKEN, response.refresh_token)
                putString(SharedPrefs.ACCEESS_TOKEN, response.access_token)
                putString(SharedPrefs.ACCESS_EXPIRES, LocalDateTime.now().plusSeconds(response.expires_in).toString(ISODateTimeFormat.dateTime()))
                putString(SharedPrefs.URL, url)
            }.apply()

            refreshUserInfo()

            return SUCCESS
        }catch (e: HttpException){
            return handleException(e, "login", url, isUrlManual)
        }catch (e: IOException){
            return handleException(e, "login", url, isUrlManual)
        }catch (e: IllegalArgumentException){
            return handleException(e, "login", url, isUrlManual)
        }
    }

    suspend fun refreshUserInfo(accountId: Int): LoginResult {

        val userWebservice: UserWebservice = app.retrofit?.create(UserWebservice::class.java)!!
        try {
            val user: UserResponse = userWebservice.getUser()

            sprefs.edit().apply {
                putString(SharedPrefs.NAME, user.fullName ?: "")
                putString(SharedPrefs.TYPE, user.userType ?: "")
                putString(SharedPrefs.TYPE_TEXT, user.userTypeText ?: "")
                val semesterEnd: DateTime? = user.settingModules?.common?.actualSemester?.to?.let {
                    try {
                        ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(it)
                    }catch (e: IllegalArgumentException){
                        e.printStackTrace()
                        null
                    }
                }
                putString(SharedPrefs.SEMESTER_END, if (semesterEnd == null) "" else ISODateTimeFormat.dateTime().print(semesterEnd))
            }.apply()
            return SUCCESS
        }catch (e: HttpException){
            return handleException(e, "user")
        }catch (e: IOException){
            return handleException(e, "user")
        }
    }

    /**
     * Logs out user (deletes credentials)
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun logout() {
        sprefs.edit().apply {
            remove(SharedPrefs.REFRESH_TOKEN)
            remove(SharedPrefs.ACCEESS_TOKEN)
            remove(SharedPrefs.ACCESS_EXPIRES)
            remove(SharedPrefs.NAME)
            remove(SharedPrefs.TYPE)
            remove(SharedPrefs.TYPE_TEXT)
            remove(SharedPrefs.SEMESTER_END)
        }.apply()
        GlobalScope.launch {
            app.rozvrhDb.clearAllTables()
        }
        app.rozvrhStatusStore.clear()
        app.clearObjects()
        app.notificationState.offset = 0
        PermanentNotification.update(null, 0, app)
        WidgetProvider.updateAll(null, app)
    }

    fun isLoggedIn(): Boolean {
        return ! sprefs.getString(SharedPrefs.REFRESH_TOKEN, "").isNullOrBlank()
    }

    /**
     * Whether to show teacher's or students rozvrh (each is fetched and displayed slightly differently)
     * @return `true` if the user logged in is a teacher or `false` if not (then it is a student or a parent)
     */
    fun isTeacher(): Boolean {
        val type = sprefs.getString(SharedPrefs.TYPE, "")
        return type == "teacher"
    }

    /**
     * Checks if user is logged in or has seen the welcome screen (where crash reports are
     * enabled/disabled), the starts the corresponding activity (if it isn't already started).
     * `finish()` **won't** be called on the current activity.
     *
     * @return An activity which is being started or `null` if no activity will be started.
     */
    fun checkLogin(currentActivity: Activity): KClass<out Activity>? {
        val ctx = currentActivity
        val seenWelcome = SharedPrefs.containsPreference(app, R.string.PREFS_SEND_CRASH_REPORTS)
        if (!seenWelcome && currentActivity !is WelcomeActivity) {
            val intent = Intent(ctx, WelcomeActivity::class.java)
            ctx.startActivity(intent)
            return WelcomeActivity::class
        }
        if (!isLoggedIn() && currentActivity !is LoginActivity) {
            val intent = Intent(ctx, LoginActivity::class.java)
            ctx.startActivity(intent)
            return LoginActivity::class
        }
        if (currentActivity !is MainActivity) {
            val intent = Intent(ctx, MainActivity::class.java)
            ctx.startActivity(intent)
            return MainActivity::class
        }
        return null
    }

    private fun getTokenAuthenticator(account: Account): TokenAuthenticator {
//        var account: Account? = tokenAuthenticators[accountId]?.account
//        if (account == null){ //token authenticator does not exist
//            account = getAccount(accountId) ?: return null
//            // function above is suspending, tokenAuthenticators mich have changed
//        }
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

    /** Creates new instance of Retrofit with the account's URL and login credentials (automatically updated according to database). If the URL is invalid, `null` is returned.*/
    private fun createRetrofit(account: Account): Retrofit? {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val tokenAuthenticator = getTokenAuthenticator(account) ?: return null
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

    enum class LoginResult{
        SUCCESS,
        UNREACHABLE,
        WRONG_LOGIN,
        UNEXPECTED_RESPONSE
    }
}

public open class LoginException(message: String?): RuntimeException(message)
public class LoginRequiredException(): LoginException("You need to log in first to perform this action")

