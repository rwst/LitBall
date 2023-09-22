package org.reactome.lit_ball.service

import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.reactome.`lit-ball`.BuildConfig
import org.reactome.lit_ball.util.Logger
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

object S2Service {

    // Copyright 2023 Ralf Stephan
    private const val TAG = "S2Service"

    object RetrofitHelper {

        private const val BASE_URL = "https://api.semanticscholar.org/"
        private const val USER_AGENT = "LitBall ${BuildConfig.APP_VERSION} (https://github.com/rwst/LitBall)"
        private var logging = HttpLoggingInterceptor()

        fun getInstance(): Retrofit {
            logging.level = Level.BASIC
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

    // Max supported fields: "paperId, externalIds, title, abstract, publicationTypes, tldr"
    @Serializable
    data class PaperDetailsWithAbstract(
        val paperId: String? = "",
        var externalIds: Map<String, String>? = emptyMap(),
        val title: String? = "",
        val abstract: String? = "",
        var publicationTypes: List<String>? = emptyList(),
        var tldr: Map<String, String>? = emptyMap(),
        var publicationDate: String? = "",
    )

    @Serializable
    data class PaperDetails(
        val paperId: String? = "",
        var externalIds: Map<String, String>? = emptyMap(),
        val title: String? = "",
        var publicationTypes: List<String>? = emptyList(),
        var tldr: Map<String, String>? = emptyMap(),
        var publicationDate: String? = "",
    )

    @Serializable
    data class Citations(
        val paperId: String? = "",
        var externalIds: Map<String, String>? = emptyMap(),
    )

    @Serializable
    data class References(
        val paperId: String? = "",
        var externalIds: Map<String, String>? = emptyMap(),
    )

    @Serializable
    data class PaperRefs(
        val paperId: String? = "",
        var citations: List<Citations>? = null,
        var references: List<References>? = null,
    )

    interface PaperRefsApi {
        @GET("/graph/v1/paper/{paper_id}")
        suspend fun get(
            @Path("paper_id") paperId: String,
            @Query("fields") fields: String,
        ): Response<PaperRefs>
    }

    interface SinglePaperApi {
        @GET("/graph/v1/paper/{paper_id}")
        suspend fun get(
            @Path("paper_id") paperId: String,
            @Query("fields") fields: String,
        ): Response<PaperDetailsWithAbstract>
    }

    interface BulkPaperApiBase {
        //suspend fun postRequest(map: Map<String, List<String>>, fields: String): Response<Any>?
    }

    interface BulkPaperWithAbstractApi : BulkPaperApiBase {
        @POST("/graph/v1/paper/batch")
        suspend fun postRequest(
            @Body ids: Map<String, @JvmSuppressWildcards List<Any>>,
            @Query("fields") fields: String,
        ): Response<List<PaperDetailsWithAbstract>>
    }

    suspend fun getBulkPaperDetailsWithAbstract(
        ids: List<String>,
        fields: String
    ): List<PaperDetailsWithAbstract>? {
        val api = RetrofitHelper.getInstance().create(BulkPaperWithAbstractApi::class.java)
        val map = mapOf("ids" to ids)
        val result = api.postRequest(map, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        return null
    }

    interface BulkPaperApi : BulkPaperApiBase {
        @POST("/graph/v1/paper/batch")
        suspend fun postRequest(
            @Body ids: Map<String, @JvmSuppressWildcards List<Any>>,
            @Query("fields") fields: String,
        ): Response<List<PaperDetails>>
    }

    suspend fun getBulkPaperDetails(
        ids: List<String>,
        fields: String
    ): List<PaperDetails>? {
        val api = RetrofitHelper.getInstance().create(BulkPaperApi::class.java)
        val map = mapOf("ids" to ids)
        val result = api.postRequest(map, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        return null
    }

    interface BulkPaperRefsApi : BulkPaperApiBase {
        @POST("/graph/v1/paper/batch")
        suspend fun postRequest(
            @Body ids: Map<String, @JvmSuppressWildcards List<Any>>,
            @Query("fields") fields: String,
        ): Response<List<PaperRefs>>
    }

    suspend fun getBulkPaperRefs(
        ids: List<String>,
        fields: String
    ): List<PaperRefs>? {
        val api = RetrofitHelper.getInstance().create(BulkPaperRefsApi::class.java)
        val map = mapOf("ids" to ids)
        val result = api.postRequest(map, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        return null
    }

    suspend fun getSinglePaperDetailsWithAbstract(paperId: String, fields: String): PaperDetailsWithAbstract? {
        val singlePaperApi = RetrofitHelper.getInstance().create(SinglePaperApi::class.java)
        val result = singlePaperApi.get(paperId, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        throw HttpException(result)
    }

    suspend fun getPaperRefs(paperId: String, fields: String): PaperRefs? {
        val singleRefApi = RetrofitHelper.getInstance().create(PaperRefsApi::class.java)
        val result = singleRefApi.get(paperId, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        throw HttpException(result)
    }

    suspend fun getPaperDetails(paperId: String, fields: String): PaperDetailsWithAbstract? {
        val singlePaperApi = RetrofitHelper.getInstance().create(SinglePaperApi::class.java)
        val result = singlePaperApi.get(paperId, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        return null
    }
}
