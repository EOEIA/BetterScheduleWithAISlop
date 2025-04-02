package cz.vitskalicky.lepsirozvrh.bakaAPI.login

import android.util.Log
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.AccountRepository
import cz.vitskalicky.lepsirozvrh.model.AccountRepository.LoginResultStatus.*
import cz.vitskalicky.lepsirozvrh.model.LoginRequiredException
import io.sentry.Sentry
import kotlinx.coroutines.runBlocking
import okhttp3.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Inserts authentication headers to requests
 *
 * - [account] - if null, user has been logged out
 * - [connectDb] - Whether to refresh tokens automatically if they expire and save them to database. If `true`, but the
 *      account isn't in database, things might break.
 */
class TokenAuthenticator(val app: MainApplication, var account: Account?, val connectDb: Boolean = true) : Authenticator, Interceptor {

    companion object{
        val TAG = TokenAuthenticator::class.simpleName
        var logids: AtomicInteger = AtomicInteger(0);
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val logid = logids.getAndIncrement()
        Sentry.addBreadcrumb("[$logid] Authentication requested, response code: ${response.code}")
        Log.d(TAG, "[$logid] Authenticator for account ${account?.id} (connectDb: $connectDb) is authentication because of response ${response.code} \"${response.body}\" for request to ${response.request.url}.")
        if (account == null) return null
        val origRequest: Request = response.request
        val retried: Int = origRequest.tag(Retried::class.java)?.count ?: 0
        if (retried > 1) {
            Sentry.addBreadcrumb("[$logid] Authentication already retried $retried times - aborting request");
            Log.d(TAG, "[$logid] Already retried $retried times - aborting.")
            return null
        }
        val usedAccessToken: String? = origRequest.header("Authorization")?.removePrefix("Bearer ")
        Log.d(TAG, "[$logid] Current access token has length ${account?.accessToken?.length}")
        Sentry.addBreadcrumb("[$logid] Current access token has length ${account?.accessToken?.length}")
        var currentAccessToken: String = account?.accessToken ?: return null
        if (usedAccessToken == currentAccessToken) {
            Log.d(TAG, "[$logid] access tokens are equal.")
            val refreshResult: AccountRepository.LoginResult = runBlocking {
                if (account == null) return@runBlocking WRONG_LOGIN.fail()
                if (connectDb) app.accountRepository.refreshToken(account!!.id) else SUCCESS.ok(account!!)
            }
            Log.d(TAG, "[$logid] refreshed with status ${refreshResult.status}.")
            when (refreshResult.status) {
                WRONG_LOGIN -> {
                    Sentry.addBreadcrumb("[$logid] Authentication: token refresh failed");
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
        Sentry.addBreadcrumb("[$logid] Authentication: trying again with ${if(currentAccessToken == usedAccessToken) "the same" else "different"} token");
        Log.d(TAG, "[$logid] Authentication: trying again with ${if(currentAccessToken == usedAccessToken) "the same" else "different"} token");
        return origRequest.newBuilder()
                .removeHeader("Authorization")
                .addHeader("Authorization", "Bearer $currentAccessToken")
                .tag(Retried::class.java, Retried(retried + 1))
                .build()
    }

    data class Retried(val count: Int)

    override fun intercept(chain: Interceptor.Chain): Response {
        val logid = logids.getAndIncrement()
        Log.d(TAG, "[$logid] Authenticator for account ${account?.id} (connectDb: $connectDb) is interception request ${chain.request().url}.")
        val token: String? = runBlocking {
            if (account == null) return@runBlocking null
            try {
                if (connectDb) account = app.accountRepository.tryRefresh(account!!) // ensure token is valid
                account?.accessToken
            } catch (_: LoginRequiredException) {
                null
            }
        }
        Log.d(TAG, "[$logid] Token length is: ${token?.length}.")
        if (!token.isNullOrBlank()) {
            val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            Log.d(TAG, "[$logid] Token added.")
            return chain.proceed(newRequest)
        }else{
            Log.w(TAG, "[$logid] Interceptor could not insert authentication header! access token is blank or empty")
            Sentry.addBreadcrumb("[$logid] Interceptor could not insert authentication header! access token is blank or empty")
        }
        return chain.proceed(chain.request())
    }
}