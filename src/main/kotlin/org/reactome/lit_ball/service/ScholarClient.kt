package org.reactome.lit_ball.service

import kotlinx.coroutines.delay
import org.reactome.lit_ball.common.QuerySetting
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.util.Logger
import org.reactome.lit_ball.util.S2SearchExpression
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

interface ScholarClient

object S2Client : ScholarClient {
    private const val DETAILS_CHUNK_SIZE = 30
    const val SINGLE_QUERY_DELAY = 100L
    private const val BULK_QUERY_DELAY = 1000L
    private const val TAG = "S2Client"
    lateinit var strategy: DelayStrategy

    suspend fun <T> getDataOrHandleExceptions(
        index: Int,
        size: Int,
        indicatorTitle: String? = null,
        getData: suspend () -> T
    ): Pair<T?, Boolean> {
        while (true) {
            try {
                val data = getData()
                return Pair(data, true)
            } catch (e: SocketTimeoutException) {
                Logger.i(TAG, "TIMEOUT")
                if (indicatorTitle != null
                    && !RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "TIMEOUT"))
                    return Pair(null, false)
            } catch (e: HttpException) {
                Logger.i(TAG, "ERROR ${e.code()}")
                if (indicatorTitle != null
                    && !RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "ERROR ${e.code()}"))
                    return Pair(null, false)
                when (e.code()) {
                    400, 404, 429, 500 -> return Pair(null, true) // assume DOI defect or unknown
                    504 -> delay(strategy.delay(false))
                    // API says too fast, so delay and repeat
                    else -> throw e
                }
            } catch (e: UnknownHostException) {
                Logger.i(TAG, "ERROR ${e.message}")
                RootStore.setInformationalDialog("Could not get DNA record.\n\nPlease make sure you are connected to the internet.")
                return Pair(null, false)
            } catch (e: SSLException) { // Proxy glitch? Retry
                return Pair(null, false)
            }
        }
    }

    // Full protocol for bulk download of paper details for a search
    suspend fun getBulkPaperSearch(
        setting: QuerySetting,
        action: (S2Service.PaperDetails) -> Unit
    ): Boolean {
        val tag = "BULKSEARCH"
        strategy = DelayStrategy(BULK_QUERY_DELAY)
        val s2expr = S2SearchExpression.from(setting)
        val indicatorTitle = "Downloading titles, TLDRs, and abstracts\nof matching papers"
        var pair = getDataOrHandleExceptions(1, 1, indicatorTitle) {
            S2Service.getBulkPaperSearch(
                query = s2expr,
                fields = "paperId,externalIds,title,abstract,publicationTypes,publicationDate"
            )
        }
        if (!pair.second) return false
        if (pair.first == null) return true
        var (total, token, data) = pair.first!!
        data.forEach { action(it) }
        Logger.i(tag, "Received: $total total, ${data.size} data, token: $token")
        println("Received: $total total, ${data.size} data, token: $token")
        delay(strategy.delay(true))
        var numDone = data.size
        while (total > numDone) {
            pair = getDataOrHandleExceptions(numDone, total, indicatorTitle) {
                S2Service.getBulkPaperSearch(
                    query = s2expr,
                    fields = "paperId,externalIds,title,abstract,publicationTypes,publicationDate",
                    token,
                )
            }
            if (!pair.second) return false
            if (pair.first == null) return true
            val (_, token1, data1) = pair.first!!
            token = token1
            data1.forEach { action(it) }
            numDone += data1.size
            Logger.i(tag, "Received: $numDone data, token: $token")
            println("Received: $numDone/$total data, token: $token")
            delay(strategy.delay(true))
            if (!RootStore.setProgressIndication(indicatorTitle, (1f * numDone) / total, "$numDone/$total"))
                return false
        }
        RootStore.setProgressIndication()
        return true
    }

    suspend fun getPaperDetails(
        doiSet: List<String>,
        action: (S2Service.PaperDetails) -> Unit
    ): Boolean {
        return if (Settings.map["S2-API-key"].isNullOrEmpty())
            getSinglePaperDetails(doiSet, action)
        else
            getBulkPaperDetails(doiSet, action)
    }

    // Full protocol for bulk download of paper details for a list of DOIs
    private suspend fun getBulkPaperDetails(
        doiSet: List<String>,
        action: (S2Service.PaperDetails) -> Unit
    ): Boolean {
        strategy = DelayStrategy(BULK_QUERY_DELAY)
        val size = doiSet.size
        val indicatorTitle = "Downloading missing titles, TLDRs,\nand abstracts"
        var index = 0
        doiSet.chunked(DETAILS_CHUNK_SIZE).forEach {
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Service.getBulkPaperDetails(
                    it,
                    "paperId,externalIds,title,abstract,publicationTypes,tldr,publicationDate"
                )
            }
            if (!pair.second) return false
            delay(strategy.delay(true))
            pair.first?.filterNotNull()?.forEach(action) // DO NOT remove filterNotNull()
            index += it.size
            if (!RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "$index/$size"))
                return false
        }
        RootStore.setProgressIndication()
        return true
    }

    // Full protocol for non-bulk download of paper details for a list of DOIs
    private suspend fun getSinglePaperDetails(
        doiSet: List<String>,
        action: (S2Service.PaperDetails) -> Unit
    ): Boolean {
        strategy = DelayStrategy(SINGLE_QUERY_DELAY)
        val size = doiSet.size
        val indicatorTitle = "Downloading missing titles, TLDRs,\nand abstracts"
        doiSet.forEachIndexed { index, it ->
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Service.getSinglePaperDetails(
                    it,
                    "paperId,externalIds,title,abstract,publicationTypes,tldr,publicationDate"
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
        return if (Settings.map["S2-API-key"].isNullOrEmpty())
            getSinglePaperRefs(doiSet, action)
        else
            getBulkPaperRefs(doiSet, action)
    }

    // Full protocol for non-bulk download of paper refs for a list of DOIs
    private suspend fun getSinglePaperRefs(
        doiSet: List<String>,
        action: (String, S2Service.PaperRefs) -> Unit
    ): Boolean {
        strategy = DelayStrategy(SINGLE_QUERY_DELAY)
        val size = doiSet.size
        val indicatorTitle = "Downloading references and\ncitations for all accepted papers"
        doiSet.forEachIndexed { index, doi ->
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Service.getPaperRefs(
                    doi,
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
    // Full protocol for bulk download of paper details for a list of DOIs
    private suspend fun getBulkPaperRefs(
        doiSet: List<String>,
        action: (String, S2Service.PaperRefs) -> Unit
    ): Boolean {
        strategy = DelayStrategy(BULK_QUERY_DELAY)
        val size = doiSet.size
        val indicatorTitle = "Downloading references and\n" +
                "citations for all accepted papers"
        var index = 0
        doiSet.chunked(DETAILS_CHUNK_SIZE).forEach { dois ->
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Service.getBulkPaperRefs(
                    dois,
                    "paperId,citations,citations.externalIds,references,references.externalIds"
                )
            }
            if (!pair.second) return false
            delay(strategy.delay(true))
            pair.first?.filterNotNull()?.forEachIndexed { index, paperRefs -> action(dois[index], paperRefs) } // DO NOT remove filterNotNull()
            index += dois.size
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