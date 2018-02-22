package me.cooper.rick.crowdcontrollerclient.util

import android.content.res.Resources
import android.text.TextUtils
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.auth.AuthenticationInterceptor
import okhttp3.Credentials
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY

object ServiceGenerator {

    private val API_BASE_URL = App.context?.getString(R.string.base_uri)
    private val httpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(BODY))


    private val builder = Retrofit.Builder()
            .baseUrl(API_BASE_URL?:"http://localhost:9098")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())

    fun <S> createService(serviceClass: Class<S>): S {
        return createService(serviceClass, null)
    }

    fun <S> createService(
            serviceClass: Class<S>, username: String, password: String): S {
        val token = if (valid(username, password)) Credentials.basic(username, password) else ""

        return createService(serviceClass, token)
    }

    fun <S> createService(serviceClass: Class<S>, authToken: String?): S {
        if (!TextUtils.isEmpty(authToken)) {
            val interceptor = AuthenticationInterceptor(authToken!!)

            if (!httpClient.interceptors().contains(interceptor)) {
                httpClient.interceptors().removeAll { it.javaClass == AuthenticationInterceptor::class.java }
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