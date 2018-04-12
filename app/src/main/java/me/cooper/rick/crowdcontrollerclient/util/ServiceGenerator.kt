package me.cooper.rick.crowdcontrollerclient.util

import android.text.TextUtils
import me.cooper.rick.crowdcontrollerclient.api.auth.AuthenticationInterceptor
import me.cooper.rick.crowdcontrollerclient.constants.BASE_URL
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import kotlin.reflect.KClass

object ServiceGenerator {

    private val API_BASE_URL = "http://$BASE_URL"
//    private val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//            .tlsVersions(TlsVersion.TLS_1_2)
//            .cipherSuites(
//                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
//                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
//                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
//            .build()
    private val httpClient = OkHttpClient.Builder()
//            .connectionSpecs(listOf(spec))
            .addInterceptor(HttpLoggingInterceptor().setLevel(BODY))

    private val builder = Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(JacksonConverterFactory.create())
            .client(httpClient.build())

    fun <S : Any> createService(serviceClass: KClass<S>): S {
        return createService(serviceClass, null)
    }

    fun <S : Any> createService(serviceClass: KClass<S>, username: String, password: String): S {
        val token = if (valid(username, password)) Credentials.basic(username, password) else ""

        return createService(serviceClass, token)
    }

    fun <S : Any> createService(serviceClass: KClass<S>, authToken: String?): S {
        if (!TextUtils.isEmpty(authToken)) {
            val interceptor = AuthenticationInterceptor(authToken!!)

            if (!httpClient.interceptors().contains(interceptor)) {
                httpClient.interceptors().removeAll { it.javaClass == AuthenticationInterceptor::class.java }
                httpClient.addInterceptor(interceptor)
            }
        }

        builder.client(httpClient.build())
        val retrofit: Retrofit = builder.build()

        return retrofit.create(serviceClass.java)
    }

    fun retrofit(): Retrofit = builder.build()

    private fun valid(username: String, password: String): Boolean =
            !TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)


}