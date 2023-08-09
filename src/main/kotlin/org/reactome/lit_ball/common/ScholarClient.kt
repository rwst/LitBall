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

    suspend fun getBulkPaperDetailsWithAbstract(doiSet: List<String>)
    : List<S2Service.PaperDetailsWithAbstract?>? {
        return S2Service.getBulkPaperDetailsWithAbstract(
            doiSet,
            "paperId,externalIds,title,abstract,publicationTypes,tldr"
        )
    }

    suspend fun getPaperDetails(
        doiSet: List<String>,
    ): List<S2Service.PaperDetails?>? {
        return S2Service.getBulkPaperDetails(
            doiSet,
            "paperId,externalIds,title,tldr"
        )
    }

    suspend fun getRefs(
        doiSet: List<String>,
    ): List<S2Service.PaperRefs?>? {
        return S2Service.getBulkPaperRefs(
            doiSet,
            "paperId,citations,citations.externalIds,references,references.externalIds"
        )
    }
}
