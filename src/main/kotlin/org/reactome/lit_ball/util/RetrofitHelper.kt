@file:Suppress("UNUSED")
package util

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.reactome.lit_ball.BuildConfig
import common.Settings
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object S2RetrofitHelper {
    private const val BASE_URL = "https://api.semanticscholar.org/"
//    private const val BASE_URL = "http://localhost:8000/"
    private const val USER_AGENT = "LitBall"
    private var logging = HttpLoggingInterceptor()

    fun getInstance(headers: Map<String, String> = emptyMap(), timeout: Long = 30): Retrofit {
        logging.level = if (Settings.map["Retrofit-logging"] == "BASIC") HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
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

object EntrezRetrofitHelper {
    private const val BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/"
    private const val USER_AGENT = "LitBall ${BuildConfig.APP_VERSION} (https://github.com/rwst/LitBall)"
    private var logging = HttpLoggingInterceptor()

    fun getInstance(headers: Map<String, String> = emptyMap(), timeout: Long = 30): Retrofit {
        logging.level = if (Settings.map["Retrofit-logging"] == "BASIC") HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .addNetworkInterceptor {
                var requestBuilder = it.request()
                    .newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", USER_AGENT)


                requestBuilder = requestBuilder.addHeader("db", "pubmed")
                requestBuilder = requestBuilder.addHeader("tool", "LitBall")
                requestBuilder = requestBuilder.addHeader("email", "gtrwst9@gmail.com")
                Settings.map["Entrez-API-key"]?.also { apiKey ->
                    if (apiKey.isNotEmpty()) {
                        requestBuilder = requestBuilder.addHeader("api_key", apiKey)
                    }
                }
                headers.forEach { (name, value) -> requestBuilder = requestBuilder.addHeader(name, value) }
                it.proceed(requestBuilder.build())
            }

        return Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build()
    }

//    fun getBulkInstance(): Retrofit {
//        val apiKey = Settings.map["S2-API-key"]
//        if (apiKey.isNullOrEmpty()) {
//            throw Exception("You need a Semantic Scholar API key to access bulk service endpoints.")
//        }
//        return getInstance(mapOf("x-api-key" to apiKey), 60)
//    }
}