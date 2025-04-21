@file:Suppress("UNUSED")
package service

import kotlinx.serialization.Serializable
import util.Logger
import util.S2RetrofitHelper
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.*

object S2Interface {

    // Copyright 2023 Ralf Stephan
    private const val TAG = "S2Service"

    // Max supported fields: "paperId, externalIds, title, abstract, publicationTypes, tldr"
    @Serializable
    data class PaperDetails(
        val paperId: String? = "",
        var externalIds: Map<String, String>? = emptyMap(), // map keys: "doi", "pmid"
        var authors: List<String>? = emptyList(), // list of names
        val title: String? = "",
        val venue: String? = "",
        val journal: Map<String, String>? = emptyMap(), // map keys: "name", "issn", "volume", "pageFirst", "pageLast"
        val abstract: String? = "",
        var publicationTypes: List<String>? = emptyList(),
        var tldr: Map<String, String>? = emptyMap(),
        var publicationDate: String? = "",
    )

    @Serializable
    data class SearchResult(
        val total: Int,
        val token: String,
        var data: List<PaperDetails> = emptyList(),
    )
    @Serializable
    data class RecommendResult(
        var recommendedPapers: List<PaperDetails> = emptyList(),
    )

    @Serializable
    data class PaperFullId(
        val paperId: String? = "",
        var externalIds: Map<String, String>? = emptyMap(),
    )

    @Serializable
    data class PaperRefs(
        val paperId: String? = "",
        var citations: List<PaperFullId>? = null,
        var references: List<PaperFullId>? = null,
    )

    interface PaperRefsApi {
        @GET("/graph/v1/paper/{paper_id}")
        suspend fun get(
            @Path("paper_id") paperId: String,
            @Query("fields") fields: String,
        ): Response<PaperRefs>
    }

    @Serializable
    data class MockDetails(
        val status: String = "",
        val received: String = "",
        val requestLine: String = "",
        val headers: String = "",
    )

    interface BulkPaperApiBase {
        //suspend fun postRequest(map: Map<String, List<String>>, fields: String): Response<Any>?
    }

    interface MockDetailsApi : BulkPaperApiBase {
        @POST("/graph/v1/paper/batch")
        suspend fun postRequest(
            @Body ids: Map<String, @JvmSuppressWildcards Any>,
        ): Response<MockDetails>
    }

    interface BulkPaperDetailsApi : BulkPaperApiBase {
        @POST("/graph/v1/paper/batch")
        suspend fun postRequest(
            @QueryMap params: Map<String, String>,
            @Body ids: Map<String, @JvmSuppressWildcards Any>,
        ): Response<List<PaperDetails>>

    }

    suspend fun getBulkPaperDetails(
        ids: List<String>,
        fields: String
    ): List<PaperDetails>? {
        val api = S2RetrofitHelper.getBulkInstance().create(BulkPaperDetailsApi::class.java)
//        val api = S2RetrofitHelper.getBulkInstance().create(MockDetailsApi::class.java)
        val qmap = mapOf("fields" to fields)
        val map = mapOf("ids" to ids)
        val result = api.postRequest(qmap, map)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()// null
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        throw HttpException(result)
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
        val api = S2RetrofitHelper.getBulkInstance().create(BulkPaperRefsApi::class.java)
        val map = mapOf("ids" to ids)
        val result = api.postRequest(map, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        throw HttpException(result)
    }

    interface BulkPaperSearchApi : BulkPaperApiBase {
        @GET("/graph/v1/paper/search/bulk")
        suspend fun getRequest(
            @Query("query") query: String,
            @Query("fields") fields: String,
        ): Response<SearchResult>

        @GET("/graph/v1/paper/search/bulk")
        suspend fun getRequestWithToken(
            @Query("query") query: String,
            @Query("token") token: String,
            @Query("fields") fields: String,
        ): Response<SearchResult>
    }

    suspend fun getBulkPaperSearch(
        query: String,
        fields: String,
        token: String? = null,
    ): SearchResult? {
        val api = S2RetrofitHelper.getBulkInstance().create(BulkPaperSearchApi::class.java)
        val result = if (token != null)
            api.getRequestWithToken(query, token, fields)
        else
            api.getRequest(query, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        throw HttpException(result)
    }

    suspend fun getPaperRefs(paperId: String, fields: String): PaperRefs? {
        val singleRefApi = S2RetrofitHelper.getInstance().create(PaperRefsApi::class.java)
        val result = singleRefApi.get(paperId, fields)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        throw HttpException(result)
    }
    interface BulkRecommendedDetailsApi {
        @POST("/recommendations/v1/papers/")
        suspend fun postRequest(
            @Body positivePaperIds: Map<String, @JvmSuppressWildcards List<Any>>,
            @Query("fields") fields: String,
            @Query("limit") limit: Int
        ): Response<RecommendResult>
    }
    suspend fun getBulkRecommendedDetails(
        ids: List<String>,
        fields: String,
        limit: Int
    ): List<PaperDetails>? {
        val api = S2RetrofitHelper.getBulkInstance().create(BulkRecommendedDetailsApi::class.java)
        val map = mapOf("positivePaperIds" to ids, "negativePaperIds" to emptyList())
        val result = api.postRequest(map, fields, limit)
        if (result.isSuccessful) {
            Logger.i(TAG, result.body().toString())
            return result.body()?.recommendedPapers
        }
        Logger.i(TAG, "error code: ${result.code()}, msg: ${result.message()}")
        throw HttpException(result)
    }
}
