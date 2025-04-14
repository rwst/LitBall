package org.reactome.lit_ball.service

import common.QuerySetting
import common.Settings

interface AGService {
    suspend fun getBulkPaperSearch(
        setting: QuerySetting,
        action: (S2Interface.PaperDetails) -> Unit
    ): Boolean

    suspend fun getPaperDetails(
        doiSet: List<String>,
        fields: String,
        action: (S2Interface.PaperDetails) -> Unit
    ): Boolean

    suspend fun getRefs(
        doiSet: List<String>,
        action: (String, S2Interface.PaperRefs) -> Unit
    ): Boolean

    suspend fun getSimilarDetails(
        doiSet: List<String>,
        action: (S2Interface.PaperDetails) -> Unit
    ): Boolean

    suspend fun <T> getDataOrHandleExceptions(
        index: Int,
        size: Int,
        indicatorTitle: String? = null,
        getData: suspend () -> T
    ): Pair<T?, Boolean>
}

fun getAGService(): AGService {
    return mapOf("S2" to S2Client, "OpenAlex" to OpenAlexClient)[Settings.map["AG-service"]] ?: S2Client
}
