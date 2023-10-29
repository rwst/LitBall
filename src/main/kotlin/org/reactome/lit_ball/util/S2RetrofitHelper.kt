package org.reactome.lit_ball.util

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.reactome.lit_ball.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object S2RetrofitHelper {
    private const val BASE_URL = "https://api.semanticscholar.org/"
    private const val USER_AGENT = "LitBall ${BuildConfig.APP_VERSION} (https://github.com/rwst/LitBall)"
    private var logging = HttpLoggingInterceptor()

    fun getInstance(): Retrofit {
        logging.level = HttpLoggingInterceptor.Level.BASIC
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor {
                val request = it.request()
                    .newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", USER_AGENT)
                    .build()
                it.proceed(request)
            }
        return Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build()
    }
}