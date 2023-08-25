package org.reactome.lit_ball.service

import kotlinx.coroutines.delay
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.util.Logger
import org.reactome.lit_ball.util.handleException
import retrofit2.HttpException
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
        val strategy = DelayStrategy(SINGLE_QUERY_DELAY)
        val indicatorTitle = "Downloading titles, TLDRs,\nand abstracts for automatic filtering"
        doiSet.forEachIndexed { index, it ->
            var paper: S2Service.PaperDetailsWithAbstract?
            do {
                paper = try {
                    S2Service.getSinglePaperDetailsWithAbstract(
                        "DOI:$it",
                        "paperId,externalIds,title,abstract,publicationTypes,tldr"
                    )
                }
                catch (e: SocketTimeoutException) {
                    Logger.i(TAG, "TIMEOUT")
                    if (RootStore.setProgressIndication(indicatorTitle,(1f * index) / size, "TIMEOUT"))
                        continue
                    else return false
                } catch (e: HttpException) {
                    paper = null
                    if (!RootStore.setProgressIndication(indicatorTitle, (1f*index)/size,  "ERROR ${e.code()}"))
                        return false
                    when (e.code()) {
                        400, 404 -> break // assume DOI defect or unknown
                        429 -> {          // API says too fast, so delay and repeat
                            delay(strategy.delay(false))
                            continue
                        }
                        else -> throw (e)
                    }
                }
                break
            } while (true)
            delay(strategy.delay(true))
            paper?.also (action)
            if (!RootStore.setProgressIndication(indicatorTitle, (1f*index)/size, "$index/$size"))
                return false
        }
        RootStore.setProgressIndication()
        return true
    }
    suspend fun getRefs(
        doiSet: List<String>,
        action: (S2Service.PaperRefs) -> Unit
    ): Boolean {
        val size = doiSet.size
        val strategy = DelayStrategy(SINGLE_QUERY_DELAY)
        val indicatorTitle = "Downloading references and\ncitations for all accepted papers"
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
                    if (RootStore.setProgressIndication(indicatorTitle, (1f*index)/size, "TIMEOUT"))
                        continue
                    else return false
                } catch (e: HttpException) {
                    refs = null
                    if (!RootStore.setProgressIndication(indicatorTitle, (1f*index)/size, "ERROR ${e.code()}"))
                        return false
                    when (e.code()) {
                        400, 404 -> break // assume DOI defect or unknown
                        429 -> {          // API says too fast, so delay and repeat
                            delay(strategy.delay(false))
                            continue
                        }
                        else -> throw (e)
                    }
                }
                break
            } while (true)
            delay(strategy.delay(true))
            refs?.also(action)
            if (!RootStore.setProgressIndication(indicatorTitle, (1f*index)/size, "$index/$size"))
                return false
        }
        RootStore.setProgressIndication()
        return true
    }
}

internal class DelayStrategy(private val minDelay: Long) {
    fun delay(wasSuccessful: Boolean): Long {
        return if (wasSuccessful) {
            noFails = 0
            minDelay
        } else {
            noFails += 1
            val mul = if (noFails <= multiplier.size) multiplier[noFails] else multiplier.last()
            mul * minDelay
        }
    }
    companion object {
        var noFails = 0
        val multiplier = listOf(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024)
    }
}