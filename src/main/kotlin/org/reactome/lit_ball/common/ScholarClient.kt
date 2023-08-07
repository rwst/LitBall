package org.reactome.lit_ball.common

interface ScholarClient {
//    fun getAbstractFor(doi: String): String?
}

object S2client : ScholarClient {
    suspend fun getDataFor(doi: String)
    : S2Service.PaperDetailsWithAbstract? {
        return S2Service.getPaperDetails(
            "DOI:$doi",
            "paperId,externalIds,title,abstract,publicationTypes,tldr"
        )
    }

    suspend fun getDataWithAbstractFor(doiSet: List<String>)
    : List<S2Service.PaperDetailsWithAbstract?>? {
        return S2Service.getBulkPaperDetails(
            S2Service.BulkPaperWithAbstractApi::class.java,
            doiSet,
            "paperId,externalIds,title,abstract,publicationTypes,tldr"
        )
    }

    suspend fun getPaperDetails(
        doiSet: List<String>,
        type: String
    ): List<S2Service.PaperDetails?>? {
        return S2Service.getBulkPaperDetails(
            S2Service.BulkPaperApi::class.java,
            doiSet,
            "paperId,externalIds,title,tldr"
        )
    }

    suspend fun getRefs(
        doiSet: List<String>,
    ): List<S2Service.PaperRefs?>? {
        return S2Service.getBulkPaperDetails(
            S2Service.BulkPaperRefsApi::class.java,
            doiSet,
            "paperId,citations,references"
        )
    }
}
