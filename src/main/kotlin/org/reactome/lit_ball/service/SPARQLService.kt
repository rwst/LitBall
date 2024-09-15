package org.reactome.lit_ball.service

import org.apache.jena.sparql.exec.http.QueryExecutionHTTPBuilder

object WDQSService {
    private const val URI = "https://query-scholarly.wikidata.org/sparql"
    private val qeb: QueryExecutionHTTPBuilder = QueryExecutionHTTPBuilder
        .service(URI)

    fun query(queryString: String): Set<Map<String, String>> {
        val x = qeb.query(queryString).build()
        val rs = x.execSelect()
        val rv = rs.resultVars
        val res = setOf<Map<String, String>>().toMutableSet()
        rs.forEach { sol ->
            val map = emptyMap<String, String>().toMutableMap()
            rv.forEach { name ->
                map[name] = sol[name].toString()
            }
            res += map
        }
        return res
    }
}