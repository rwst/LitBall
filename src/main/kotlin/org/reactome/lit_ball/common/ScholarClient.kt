package org.reactome.lit_ball.common

import kotlinx.coroutines.delay
import org.reactome.lit_ball.util.handleException
import java.net.SocketTimeoutException

interface ScholarClient

object S2Client : ScholarClient {
    const val REFS_CHUNK_SIZE = 5
    private const val DETAILS_CHUNK_SIZE = 30
    private const val SINGLE_QUERY_DELAY = 500L
    private const val BULK_QUERY_DELAY = 5000L
    private const val TAG = "S2Client"
    suspend fun getDataFor(doi: String)
    : S2Service.PaperDetailsWithAbstract? {
        return S2Service.getPaperDetails(
            "DOI:$doi",
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

    suspend fun getBulkPaperDetailsWithAbstract(
        doiSet: List<String>,
        action: (S2Service.PaperDetailsWithAbstract) -> Unit
    ): Boolean {
        doiSet.chunked(DETAILS_CHUNK_SIZE).forEach {
            var papers: List<S2Service.PaperDetailsWithAbstract?>?
            do {
                papers = try {
                    S2Service.getBulkPaperDetailsWithAbstract(
                        it,
                        "paperId,externalIds,title,abstract,publicationTypes,tldr"
                    )
                } catch (e: Exception) {
                    handleException(e)
                    return false
                }
                catch (e: SocketTimeoutException) {
                    Logger.i(TAG, "TIMEOUT")
                    null
                }
                if (papers != null) {
                    if (papers.isNotEmpty()) break
                }
                delay(BULK_QUERY_DELAY)
            }
            while (papers.isNullOrEmpty())
            papers?.filterNotNull()?.forEach(action)
        }
        return true
    }

    suspend fun getPaperDetailsWithAbstract(
        doiSet: List<String>,
        action: (S2Service.PaperDetailsWithAbstract) -> Unit
    ): Boolean {
        doiSet.forEach {
            var paper: S2Service.PaperDetailsWithAbstract?
            do {
                paper = try {
                    S2Service.getSinglePaperDetailsWithAbstract(
                        "DOI:$it",
                        "paperId,externalIds,title,abstract,publicationTypes,tldr"
                    )
                } catch (e: Exception) {
                    handleException(e)
                    return false
                }
                catch (e: SocketTimeoutException) {
                    Logger.i(TAG, "TIMEOUT")
                    null
                }
                if (paper != null)
                    break
                delay(SINGLE_QUERY_DELAY)
            }
            while (true)
            paper?.also (action)
        }
        return true
    }
    suspend fun getRefs(
        doiSet: List<String>,
        action: (S2Service.PaperRefs) -> Unit
    ): Boolean {
        doiSet.forEach {
            var refs: S2Service.PaperRefs?
            do {
                refs = try {
                    S2Service.getPaperRefs(
                        "DOI:$it",
                        "paperId,citations,citations.externalIds,references,references.externalIds"
                    )
                } catch (e: SocketTimeoutException) {
                    Logger.i(TAG, "TIMEOUT")
                    continue
                }
                delay(SINGLE_QUERY_DELAY)
                if (refs != null)
                    break
            } while (true)
            refs?.also(action)
        }
        return true
    }
}
