package cz.vitskalicky.lepsirozvrh.bakaAPI.login

import android.util.Log
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.AccountRepository
import cz.vitskalicky.lepsirozvrh.model.AccountRepository.LoginResultStatus.*
import io.sentry.Sentry
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.atomic.AtomicInteger

/**
 * Inserts authentication headers to requests
 * - [getAccountCallback] is called to get an up-to-date account information. Remember that this may be called from
 *   different threads. If `invalidateToken` is supplied, such token mus be considered invalid refresh may be needed.
 *  */
class TokenAuthenticator(var getAccountCallback: (suspend (invalidateToken: String?) -> AccountRepository.LoginResult)?) : Authenticator, Interceptor {

    companion object{
        val TAG = TokenAuthenticator::class.simpleName
        var logids: AtomicInteger = AtomicInteger(0)
    }

    private suspend fun getAccount(invalidateToken: String? = null): AccountRepository.LoginResult{
        return getAccountCallback?.invoke(invalidateToken) ?: WRONG_LOGIN.fail()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val logid = logids.getAndIncrement()
        Sentry.addBreadcrumb("[$logid] Authentication requested, response code: ${response.code}")
        Log.d(TAG, "[$logid] Authenticator is authenticating because of response ${response.code} \"${response.body}\" for request to ${response.request.url}.")
        return runBlocking {
            val accountRes = getAccount()
            var account: Account = accountRes.account ?: return@runBlocking null
            val origRequest: Request = response.request
            val retried: Int = origRequest.tag(Retried::class.java)?.count ?: 0
            if (retried > 1) {
                Sentry.addBreadcrumb("[$logid] Authentication already retried $retried times - aborting request")
                Log.d(TAG, "[$logid] Already retried $retried times - aborting.")
                return@runBlocking null
            }
            val usedAccessToken: String? = origRequest.header("Authorization")?.removePrefix("Bearer ")
            Log.d(TAG, "[$logid] Current access token has length ${account.accessToken.length}")
            Sentry.addBreadcrumb("[$logid] Current access token has length ${account.accessToken.length}")
            var currentAccessToken: String = account.accessToken
            if (usedAccessToken == currentAccessToken) {
                Log.d(TAG, "[$logid] access tokens are equal.")
                val refreshResult = getAccount(usedAccessToken)
                Log.d(TAG, "[$logid] refreshed with status ${refreshResult.status}.")
                when (refreshResult.status) {
                    WRONG_LOGIN -> {
                        Sentry.addBreadcrumb("[$logid] Authentication: token refresh failed")
                        Log.d(TAG, "[$logid] Authentication: token refresh failed")
                        return@runBlocking null
                    }

                    UNREACHABLE, UNEXPECTED_RESPONSE -> {
                        return@runBlocking origRequest.newBuilder()
                            .tag(Retried::class.java, Retried(retried + 1))
                            .build()
                    }

                    SUCCESS -> {
                        account = refreshResult.account!!
                        currentAccessToken = account.accessToken
                    }
                }
            }
            Sentry.addBreadcrumb("[$logid] Authentication: trying again with ${if (currentAccessToken == usedAccessToken) "the same" else "different"} token")
            Log.d(
                TAG,
                "[$logid] Authentication: trying again with ${if (currentAccessToken == usedAccessToken) "the same" else "different"} token"
            )
            return@runBlocking origRequest.newBuilder()
                .removeHeader("Authorization")
                .addHeader("Authorization", "Bearer $currentAccessToken")
                .tag(Retried::class.java, Retried(retried + 1))
                .build()
        }
    }

    data class Retried(val count: Int)

    override fun intercept(chain: Interceptor.Chain): Response {
        val logid = logids.getAndIncrement()
        Log.d(TAG, "[$logid] Authenticator is interception request ${chain.request().url}.")
        val accountRes: AccountRepository.LoginResult = runBlocking { getAccount() }
        val token: String? = when (accountRes.status){
            WRONG_LOGIN -> {
                Log.d(TAG, "[$logid] Refreshed failed, terminating request with 401.")
                return Response.Builder()
                    .code(401) //unauthorized
                    .body("Failed to refresh token".toResponseBody(null))
                    .protocol(Protocol.HTTP_2)
                    .message("Failed to refresh token")
                    .request(chain.request())
                    .build()
            }
            else -> accountRes.account?.accessToken
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
            return chain.proceed(chain.request())
        }
    }
}