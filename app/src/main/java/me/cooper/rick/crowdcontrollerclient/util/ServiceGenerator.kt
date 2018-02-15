package me.cooper.rick.crowdcontrollerclient.util

import android.text.TextUtils
import me.cooper.rick.crowdcontrollerclient.auth.AuthenticationInterceptor
import okhttp3.Credentials
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import okhttp3.OkHttpClient

object ServiceGenerator {

    // http://stb098.edu.csesalford.com
    private val API_BASE_URL = "http://2.102.15.226"

    private val httpClient = OkHttpClient.Builder()

    private val builder = Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())

    fun <S> createService(serviceClass: Class<S>): S {
        return createService(serviceClass, "")
    }

    fun <S> createService(
            serviceClass: Class<S>, username: String, password: String): S {
        val token = if (valid(username, password)) Credentials.basic(username, password) else ""

        return createService(serviceClass, token)
    }

    fun <S> createService(serviceClass: Class<S>, authToken: String): S {
        if (!TextUtils.isEmpty(authToken)) {
            val interceptor = AuthenticationInterceptor(authToken)

            if (!httpClient.interceptors().contains(interceptor)) {
                httpClient.addInterceptor(interceptor)
            }
        }

        builder.client(httpClient.build())
        val retrofit: Retrofit = builder.build()

        return retrofit.create(serviceClass)
    }

    private fun valid(username: String, password: String): Boolean =
        !TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)


}