@file:Suppress("UNUSED")
package service

//import ktalex.dal.client.WorkClient
import common.QuerySetting
import common.Settings
import model.ProgressHandler

object OpenAlexClient : AGService {
    private const val TAG = "OAClient"
    override lateinit var progressHandler: ProgressHandler

    override suspend fun <T> getDataOrHandleExceptions(
        index: Int,
        size: Int,
        indicatorTitle: String?,
        getData: suspend () -> T
    ): Pair<T?, Boolean> {
//        val client = WorkClient(mailTo = Settings.map["OpenAlex-email"]?: "")
/*
        while (true) {
            try {
                val data = getData()
                return Pair(data, true)
            } catch (e: SocketTimeoutException) {
                Logger.i(TAG, "TIMEOUT")
                if (indicatorTitle != null
                    && !RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "TIMEOUT")
                )
                    return Pair(null, false)
            } catch (e: HttpException) {
                Logger.i(TAG, "ERROR ${e.code()}")
                if (indicatorTitle != null
                    && !RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "ERROR ${e.code()}")
                )
                    return Pair(null, false)
                when (e.code()) {
                    400, 404, 500 -> return Pair(null, true) // assume DOI defect or unknown
                    403 -> {
                        RootStore.setInformationalDialog("API returns 403, bailing out. Is the API key from Semantic Scholar expired?")
                        return Pair(null, false)
                    }
                    429 -> {
                        if (Settings.map["S2-API-key"].isNullOrEmpty()) {
                            RootStore.setInformationalDialog("API returns 429, bailing out. Suggest getting API key from Semantic Scholar.")
                            return Pair(null, false)
                        }
                        delay(strategy.delay(false))
                    }

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
*/
        return Pair(null, false)
    }

    // Full protocol for bulk download of paper details for a search
    override suspend fun getBulkPaperSearch(
        setting: QuerySetting,
        action: (S2Interface.PaperDetails) -> Unit
    ): Boolean {
/*
        val tag = "BULKSEARCH"
        strategy = DelayStrategy(BULK_QUERY_DELAY)
        val s2expr = OASearchExpression.from(setting)
        val indicatorTitle = "Downloading titles, TLDRs, and abstracts\nof matching papers"
        var pair = getDataOrHandleExceptions(1, 1, indicatorTitle) {
            S2Interface.getBulkPaperSearch(
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
                S2Interface.getBulkPaperSearch(
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
*/
        return true
    }

    // Full protocol for bulk download of paper details for a list of DOIs
    override suspend fun getPaperDetails(
        doiSet: List<String>,
        fields: String,
        action: (S2Interface.PaperDetails) -> Unit
    ): Boolean {
/*
        strategy = DelayStrategy(BULK_QUERY_DELAY)
        val size = doiSet.size
        val indicatorTitle = "Downloading missing titles, TLDRs,\nand abstracts"
        var index = 0
        doiSet.chunked(DETAILS_CHUNK_SIZE).forEach { ids ->
            val paperIds = ids.map { if (it.startsWith("S2:")) it.substring(3) else it }
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Interface.getBulkPaperDetails(
                    paperIds,
                    "paperId,externalIds,title,abstract,publicationTypes,tldr,publicationDate"
                )
            }
            if (!pair.second) return false
            delay(strategy.delay(true))
            pair.first?.filterNotNull()?.forEach(action) // DO NOT remove filterNotNull()
            index += paperIds.size
            if (!RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "$index/$size"))
                return false
        }
        RootStore.setProgressIndication()
*/
        return true
    }

    // Full protocol for non-bulk download of paper details for a list of DOIs
    private fun getSinglePaperDetails(
        doiSet: List<String>,
        action: (S2Interface.PaperDetails) -> Unit
    ): Boolean {
/*
        strategy = DelayStrategy(SINGLE_QUERY_DELAY)
        val size = doiSet.size
        val indicatorTitle = "Downloading missing titles, TLDRs,\nand abstracts"
        doiSet.forEachIndexed { index, it ->
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Interface.getSinglePaperDetails(
                    if (it.startsWith("S2:")) it.substring(3) else it,
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
*/
        return true
    }

    override suspend fun getRefs(
        doiSet: List<String>,
        action: suspend (String, S2Interface.PaperRefs) -> Unit
    ): Boolean {
        return if (Settings.map["OpenAlex-email"].isNullOrEmpty())
            getSinglePaperRefs(doiSet, action)
        else
            getBulkPaperRefs(doiSet, action)
    }

    // Full protocol for non-bulk download of paper refs for a list of DOIs
    private fun getSinglePaperRefs(
        doiSet: List<String>,
        action: suspend (String, S2Interface.PaperRefs) -> Unit
    ): Boolean {
/*
        strategy = DelayStrategy(SINGLE_QUERY_DELAY)
        val size = doiSet.size
        val indicatorTitle = "Downloading references and\ncitations for all accepted papers"
        doiSet.forEachIndexed { index, doi ->
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Interface.getPaperRefs(
                    if (doi.startsWith("S2:")) doi.substring(3) else doi,
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
*/
        return true
    }

    // Full protocol for bulk download of paper details for a list of DOIs
    private fun getBulkPaperRefs(
        doiSet: List<String>,
        action: suspend (String, S2Interface.PaperRefs) -> Unit
    ): Boolean {
/*
        strategy = DelayStrategy(BULK_QUERY_DELAY)
        val size = doiSet.size
        val indicatorTitle = "Downloading references and\n" +
                "citations for all accepted papers"
        var index = 0
        doiSet.chunked(DETAILS_CHUNK_SIZE).forEach { dois ->
            val paperIds = dois.map { if (it.startsWith("S2:")) it.substring(3) else it }
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Interface.getBulkPaperRefs(
                    paperIds,
                    "paperId,citations,citations.externalIds,references,references.externalIds"
                )
            }
            if (!pair.second) return false
            delay(strategy.delay(true))
            pair.first?.filterNotNull()
                ?.forEachIndexed { index, paperRefs -> action(dois[index], paperRefs) } // DO NOT remove filterNotNull()
            index += dois.size
            if (!RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "$index/$size"))
                return false
        }
        RootStore.setProgressIndication()
*/
        return true
    }
    override suspend fun getSimilarDetails(
        doiSet: List<String>,
        action: (S2Interface.PaperDetails) -> Unit
    ): Boolean {
/*
        val minPapers = 20
        val maxPapers = 500 // limit given by S2
        strategy = DelayStrategy(BULK_QUERY_DELAY)
        val size = doiSet.size
        val limit = if (size*2 < minPapers) minPapers else if (size*2 > maxPapers) maxPapers else size*2
        val indicatorTitle = "Downloading similar papers"
        var index = 0
        doiSet.chunked(DETAILS_CHUNK_SIZE).forEach { ids ->
            val paperIds = ids.map { if (it.startsWith("S2:")) it.substring(3) else it }
            val pair = getDataOrHandleExceptions(index, size, indicatorTitle) {
                S2Interface.getBulkRecommendedDetails(
                    paperIds,
                    "paperId,externalIds,title,abstract,publicationTypes,publicationDate",
                    limit
                )
            }
            if (!pair.second) return false
            delay(strategy.delay(true))
            pair.first?.filterNotNull()?.forEach(action) // DO NOT remove filterNotNull()
            index += paperIds.size
            if (!RootStore.setProgressIndication(indicatorTitle, (1f * index) / size, "$index/$size"))
                return false
        }
        RootStore.setProgressIndication()
*/
        return true
    }
}