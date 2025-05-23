package service

val clients = listOf(S2PMID2DOIClient(), WDQSPMID2DOIClient())

// Iterates through clients to get DOIs for PMIDs
suspend fun getDOIsforPMIDs(pmidList: List<String>): List<String?> {
    val map: MutableMap<String, String?> = pmidList.associateWith { null }.toMutableMap()
    var missing: List<String>
    clients.forEach { client ->
        missing = map.filterValues { it == null }.keys.toList()
        if (missing.isNotEmpty()) {
            val response = client.getPaperDOIfromPMIDs(missing)
            missing.forEachIndexed { index, s -> map[s] = response[index] }
        }
    }
    return pmidList.map { map[it] }
}

abstract class PMID2DOIClient {
    abstract suspend fun getPaperDOIfromPMIDs(pmidList: List<String>): List<String?>
}

class S2PMID2DOIClient : PMID2DOIClient() {
    override suspend fun getPaperDOIfromPMIDs(
        pmidList: List<String>,
    ): List<String?> {
        S2Client.strategy = S2Client.DelayStrategy(S2Client.SINGLE_QUERY_DELAY)
        val size = pmidList.size
        val list = MutableList<String?>(size) { null }
        val ids = pmidList.map { "PMID:$it" }
        val map = emptyMap<String, String>().toMutableMap()
        S2Client.getPaperDetails(
            ids,
            "externalIds"
        )
        {
            val pmid = it.externalIds?.get("PMID") ?: ""
            val doi = it.externalIds?.get("DOI")
            val id = doi ?: "S2:${it.paperId}"
            map[pmid] = id
        }
        pmidList.forEachIndexed { index, it ->
            map[it]?.let { list[index] = it }
        }
        return list
    }
}

class WDQSPMID2DOIClient : PMID2DOIClient() {
    override suspend fun getPaperDOIfromPMIDs(pmidList: List<String>): List<String?> {
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
