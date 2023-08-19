package org.reactome.lit_ball.common

import kotlinx.coroutines.delay
import org.reactome.lit_ball.util.handleException
import java.net.SocketTimeoutException

interface ScholarClient

object S2Client : ScholarClient {
    private const val DETAILS_CHUNK_SIZE = 30
    private const val SINGLE_QUERY_DELAY = 500L
    private const val BULK_QUERY_DELAY = 5000L
    private const val TAG = "S2Client"

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
        val size = doiSet.size
        doiSet.forEachIndexed { index, it ->
            var paper: S2Service.PaperDetailsWithAbstract? = null
            do {
                paper = try {
                    S2Service.getSinglePaperDetailsWithAbstract(
                        "DOI:$it",
                        "paperId,externalIds,title,abstract,publicationTypes,tldr"
                    )
                }
                catch (e: SocketTimeoutException) {
                    Logger.i(TAG, "TIMEOUT")
                    RootStore.setProgressIndication(
                        Pair((1f * index) / size, "TIMEOUT"))
                    continue
                }
                delay(SINGLE_QUERY_DELAY)
                if (paper != null)
                    break
            } while (true)
            paper?.also (action)
            RootStore.setProgressIndication(Pair((1f*index)/size, "$index/$size"))
        }
        RootStore.setProgressIndication(null)
        return true
    }
    suspend fun getRefs(
        doiSet: List<String>,
        action: (S2Service.PaperRefs) -> Unit
    ): Boolean {
        val size = doiSet.size
        doiSet.forEachIndexed { index, it ->
            var refs: S2Service.PaperRefs?
            do {
                refs = try {
                    S2Service.getPaperRefs(
                        "DOI:$it",
                        "paperId,citations,citations.externalIds,references,references.externalIds"
                    )
                } catch (e: SocketTimeoutException) {
                    Logger.i(TAG, "TIMEOUT")
                    RootStore.setProgressIndication(Pair((1f*index)/size, "TIMEOUT"))
                    continue
                }
                delay(SINGLE_QUERY_DELAY)
                if (refs != null) {
                    break
                }
            } while (true)
            refs?.also(action)
            RootStore.setProgressIndication(Pair((1f*index)/size, "$index/$size"))
        }
        RootStore.setProgressIndication(null)
        return true
    }
}
