package org.reactome.lit_ball.service

import kotlinx.coroutines.delay
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.util.Logger
import org.reactome.lit_ball.util.handleException
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface ScholarClient

object S2Client : ScholarClient {
    private const val DETAILS_CHUNK_SIZE = 30
    private const val SINGLE_QUERY_DELAY = 500L
    private const val BULK_QUERY_DELAY = 5000L
    private const val TAG = "S2Client"
    private val strategy = DelayStrategy(SINGLE_QUERY_DELAY)

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
                } catch (e: SocketTimeoutException) {
                    Logger.i(TAG, "TIMEOUT")
                    null
                }
                if (papers != null) {
                    if (papers.isNotEmpty()) break
                }
                delay(BULK_QUERY_DELAY)
            } while (papers.isNullOrEmpty())
            papers?.filterNotNull()?.forEach(action)
        }
        return true
    }

    private suspend fun <T> getDataOrHandleExceptions(
        doi: String,
        index: Int,
        size: Int,
        indicatorTitle: String,
        getData: suspend (doi: String) -> T
    ): Pair<T?, Boolean> {
        while (true) {
            try {
                val data = getData("DOI:$doi")
                return Pair(data, true)
            } catch (e: SocketTimeoutException) {
                Logger.i(TAG, "TIMEOUT")
                if (!RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "TIMEOUT"))
                    return Pair(null, false)
            } catch (e: HttpException) {
                Logger.i(TAG, "ERROR ${e.code()}")
                if (!RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "ERROR ${e.code()}"))
                    return Pair(null, false)
                when (e.code()) {
                    400, 404, 500 -> return Pair(null, true) // assume DOI defect or unknown
                    429 -> delay(strategy.delay(false))
                    // API says too fast, so delay and repeat
                    else -> throw e
                }
            } catch (e: UnknownHostException) {
                Logger.i(TAG, "ERROR ${e.message}")
                RootStore.setInformationalDialog("Could not get DNA record.\n\nPlease make sure you are connected to the internet.")
                return Pair(null, false)
            }
        }
    }

    suspend fun getPaperDetailsWithAbstract(
        doiSet: List<String>,
        action: (S2Service.PaperDetailsWithAbstract) -> Unit
    ): Boolean {
        val size = doiSet.size
        val indicatorTitle = "Downloading missing titles, TLDRs,\nand abstracts"
        doiSet.forEachIndexed { index, it ->
            val pair = getDataOrHandleExceptions(it, index, size, indicatorTitle) {
                S2Service.getSinglePaperDetailsWithAbstract(
                    it,
                    "paperId,externalIds,title,abstract,publicationTypes,tldr"
                )
            }
            if (!pair.second) return false
            delay(strategy.delay(true))
            pair.first?.also(action)
            if (!RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "$index/$size"))
                return false
        }
        RootStore.setProgressIndication()
        return true
    }

    suspend fun getRefs(
        doiSet: List<String>,
        action: (String, S2Service.PaperRefs) -> Unit
    ): Boolean {
        val size = doiSet.size
        val indicatorTitle = "Downloading references and\ncitations for all accepted papers"
        doiSet.forEachIndexed { index, doi ->
            val pair = getDataOrHandleExceptions(doi, index, size, indicatorTitle) {
                S2Service.getPaperRefs(
                    it,
                    "paperId,citations,citations.externalIds,references,references.externalIds"
                )
            }
            if (!pair.second) return false
            delay(strategy.delay(true))
            pair.first?.also { (action)(doi, it) }
            if (!RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "$index/$size"))
                return false
        }
        RootStore.setProgressIndication()
        return true
    }
}

class DelayStrategy(private val minDelay: Long) {
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