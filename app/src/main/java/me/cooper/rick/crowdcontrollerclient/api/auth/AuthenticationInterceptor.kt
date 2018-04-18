package me.cooper.rick.crowdcontrollerclient.api.auth

import android.util.Log
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import okhttp3.*
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit.SECONDS
import javax.net.ssl.SSLHandshakeException

class AuthenticationInterceptor(private val authToken: String) : Interceptor {

    private var reconnectCounter = 0

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val builder = original.newBuilder()
                .header("Authorization", authToken)

        val request = builder.build()

        return tryWithRetry(request, chain)
    }

    private fun sendErrorResponse(e: Exception, request: Request): Response {
        val error = APIErrorDto()
        return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(error.status)
                .message(error.errorDescription)
                .body(ResponseBody.create(
                        MediaType.parse("application/json"),
                        error.toString()
                )).build()
    }

    private fun tryWithRetry(request: Request, chain: Interceptor.Chain): Response {
        return try {
            val response = chain.proceed(request)
            Log.i(TAG, "Reconnected after $reconnectCounter tries")
            response
        } catch (e: Exception) {
            return when (e) {
                is SSLHandshakeException, is UnknownHostException -> {
                    if (reconnectCounter <= MAX_RETRIES) {
                        reconnectCounter++
                        Thread.sleep(SECONDS.toMillis(5))
                        Log.w(TAG, "Connection failed - retrying")
                        tryWithRetry(request, chain)
                    } else {
                        Log.e(TAG, "Connection failed $MAX_RETRIES times - cancelling request")
                        reconnectCounter = 0
                        sendErrorResponse(e, request)
                    }
                }
                else -> throw e
            }
        }
    }

    companion object {
        private const val TAG = "AUTH_INTERCEPT"
        private const val MAX_RETRIES = 5
    }

}
