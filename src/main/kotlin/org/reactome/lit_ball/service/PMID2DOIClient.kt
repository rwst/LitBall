package org.reactome.lit_ball.service

import kotlinx.coroutines.delay

val clients = listOf(S2PMID2DOIClient(), WDQSPMID2DOIClient())

// Iterates through clients to get DOIs for PMIDs
suspend fun getDOIsforPMIDs(pmidList: List<String>): List<String?> {
    val map: MutableMap<String, String?> = pmidList.associateWith { null }.toMutableMap()
    var missing: List<String>
    clients.forEach { client ->
        missing = map.filterValues { it == null }.keys.toList()
        if (missing.isNotEmpty()) {
            val response = client.getSinglePaperDOIfromPMIDs(missing)
            missing.forEachIndexed { index, s -> map[s] = response[index] }
        }
    }
    return pmidList.map { map[it] }
}

abstract class PMID2DOIClient {
    abstract suspend fun getSinglePaperDOIfromPMIDs(pmidList: List<String>): List<String?>
}

class S2PMID2DOIClient : PMID2DOIClient() {
    override suspend fun getSinglePaperDOIfromPMIDs(
        pmidList: List<String>,
    ): List<String?> {
        S2Client.strategy = DelayStrategy(S2Client.SINGLE_QUERY_DELAY)
        val size = pmidList.size
        val list = MutableList<String?>(size) { null }
        pmidList.forEachIndexed { index, it ->
            var pair: Pair<S2Service.PaperDetails?, Boolean>
            do {
                pair = S2Client.getDataOrHandleExceptions(index, size, null) {
                    S2Service.getSinglePaperDetails(
                        "PMID:$it",
                        "externalIds"
                    )
                }
                delay(S2Client.strategy.delay(false))
            } while (!pair.second)
            delay(S2Client.strategy.delay(true))
            pair.first?.also {
                list[index] = it.externalIds?.get("DOI")
            }
        }
        return list
    }
}

class WDQSPMID2DOIClient : PMID2DOIClient() {
    override suspend fun getSinglePaperDOIfromPMIDs(pmidList: List<String>): List<String?> {
        val queryString = """
            PREFIX wdt: <http://www.wikidata.org/prop/direct/>
            SELECT DISTINCT ?pmid ?doi
            WHERE
                {
                  VALUES ?pmid ${pmidList.joinToString("\" \"", "{ \"", "\" }")}
                  ?item wdt:P698 ?pmid.
                  ?item wdt:P356 ?doi.
                }
        """.trimIndent()
        val indexMap: Map<String, Int> = pmidList.mapIndexed { index, s -> s to index }.toMap()
        val list = MutableList<String?>(pmidList.size) { null }
        try {
            WDQSService.query(queryString).forEach { sol ->
                indexMap[sol["pmid"]]?.let { list[it] = sol["doi"] }
            }
        } catch (e: Exception) {
            throw e
        }
        return list
    }
}
