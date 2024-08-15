package org.reactome.lit_ball.service

import org.reactome.lit_ball.common.QuerySetting

interface AGService {
    // Full protocol for bulk download of paper details for a search
    suspend fun getBulkPaperSearch(
        setting: QuerySetting,
        action: (S2Interface.PaperDetails) -> Unit
    ): Boolean

    suspend fun getPaperDetails(
        doiSet: List<String>,
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
}