package cz.vitskalicky.lepsirozvrh.bakaAPI.login

import android.util.Log
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.AccountRepository
import cz.vitskalicky.lepsirozvrh.model.AccountRepository.LoginResultStatus.*
import cz.vitskalicky.lepsirozvrh.model.LoginRequiredException
import kotlinx.coroutines.runBlocking
import okhttp3.*

/**
 * Inserts authentication headers to requests
 *
 * - [account] - if null, user has been logged out
 * - [connectDb] - Whether to refresh tokens automatically if they expire and save them to database. If `true`, but the
 *      account isn't in database, things might break.
 */
class TokenAuthenticator(val app: MainApplication, var account: Account?, val connectDb: Boolean = true) : Authenticator, Interceptor {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (account == null) return null
        val origRequest: Request = response.request
        val retried: Int = origRequest.tag(Retried::class.java)?.count ?: 0
        if (retried > 1) {
            return null
        }
        val usedAccessToken: String? = origRequest.header("Authorization")?.removePrefix("Bearer ")
        synchronized(app) {
            var currentAccessToken: String = account?.accessToken ?: return null
            if (usedAccessToken == currentAccessToken) {
                val refreshResult: AccountRepository.LoginResult = runBlocking {
                    if (account == null) return@runBlocking WRONG_LOGIN.fail()
                    if (connectDb) app.accountRepository.refreshToken(account!!.id) else SUCCESS.ok(account!!)
                }
                when (refreshResult.status) {
                    WRONG_LOGIN -> {
                        return null
                    }
                    UNREACHABLE, UNEXPECTED_RESPONSE -> {
                        return origRequest.newBuilder()
                                .tag(Retried::class.java, Retried(retried + 1))
                                .build()
                    }
                    SUCCESS -> {
                        account = refreshResult.account
                        currentAccessToken = account?.accessToken ?: return null
                    }
                }
            }
            return origRequest.newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer $currentAccessToken")
                    .tag(Retried::class.java, Retried(retried + 1))
                    .build()
        }
    }

    data class Retried(val count: Int)

    override fun intercept(chain: Interceptor.Chain): Response {
        val token: String? = synchronized(app){
            runBlocking {
                if (account == null) return@runBlocking null
                try {
                    if (connectDb) account = app.accountRepository.tryRefresh(account!!) // ensure token is valid
                    account?.accessToken
                } catch (_: LoginRequiredException) {
                    null
                }
            }
        }
        if (!token.isNullOrBlank()) {
            val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            return chain.proceed(newRequest)
        }else{
            Log.w(TokenAuthenticator::class.simpleName, "Interceptor could not insert authentication header! access token is blank or empty")
        }
        return chain.proceed(chain.request())
    }
}