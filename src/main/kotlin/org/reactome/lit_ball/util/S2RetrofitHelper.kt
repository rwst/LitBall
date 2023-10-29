package org.reactome.lit_ball.util

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.reactome.lit_ball.BuildConfig
import org.reactome.lit_ball.common.Settings
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object S2RetrofitHelper {
    private const val BASE_URL = "https://api.semanticscholar.org/"
    private const val USER_AGENT = "LitBall ${BuildConfig.APP_VERSION} (https://github.com/rwst/LitBall)"
    private var logging = HttpLoggingInterceptor()

    fun getInstance(headers: Map<String,String> = emptyMap(), timeout: Long = 30): Retrofit {
        logging.level = HttpLoggingInterceptor.Level.BASIC
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .addNetworkInterceptor {
                var requestBuilder = it.request()
                    .newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", USER_AGENT)

                headers.forEach { (name, value) -> requestBuilder = requestBuilder.addHeader(name, value) }
                it.proceed(requestBuilder.build())
            }
        return Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build()
    }

    fun getBulkInstance(): Retrofit {
        val apiKey = Settings.map["S2-API-key"]
        if (apiKey.isNullOrEmpty()) {
            throw Exception("You need a Semantic Scholar API key to access bulk service endpoints.")
        }
        return getInstance(mapOf("x-api-key" to apiKey), 60)
    }
}