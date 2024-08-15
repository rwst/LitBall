package org.reactome.lit_ball.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/*
class EntrezApi(private val email: String, private val apiKey: String? = null) {
    private val baseUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/"

    suspend fun fetchPubMedArticle(pmid: String): String = withContext(Dispatchers.IO) {
        val url = "${baseUrl}efetch.fcgi?db=pubmed&id=$pmid&retmode=xml&rettype=abstract" +
                "&email=$email${apiKey?.let { "&api_key=$it" } ?: ""}"
        URL(url).readText()
    }

    suspend fun getCitingArticles(pmid: String): List<String> = withContext(Dispatchers.IO) {
        val url = "${baseUrl}elink.fcgi?dbfrom=pubmed&db=pubmed&linkname=pubmed_pubmed_citedin&id=$pmid" +
                "&email=$email${apiKey?.let { "&api_key=$it" } ?: ""}"
        val response = URL(url).readText()
        // Parse XML response and extract citing PMIDs
        // This is a simplified example; you'd need to implement XML parsing
        listOf()
    }

    // Add more methods for other E-utilities as needed
}
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
}

import okhttp3.OkHttpClient
import okhttp3.Request
import com.fasterxml.jackson.dataformat.xml.XmlMapper

data class PubmedArticle(val MedlineCitation: MedlineCitation)
data class MedlineCitation(val Article: Article)
data class Article(val ArticleTitle: String)

val client = OkHttpClient()
val xmlMapper = XmlMapper()

fun fetchPaperMetadata(pmid: String): PubmedArticle? {
    val url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=$pmid&retmode=xml"
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val xml = response.body?.string() ?: return null
        return xmlMapper.readValue(xml, PubmedArticle::class.java)
    }
}

fun fetchCitingPapers(pmid: String): List<String> {
    val url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&db=pubmed&linkname=pubmed_pubmed_citedin&id=$pmid"
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val xml = response.body?.string() ?: return emptyList()
        val root = xmlMapper.readTree(xml)
        return root.findValuesAsText("Id")
    }
}

fun fetchCitedPapers(pmid: String): List<String> {
    val url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&db=pubmed&linkname=pubmed_pubmed_refs&id=$pmid"
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val xml = response.body?.string() ?: return emptyList()
        val root = xmlMapper.readTree(xml)
        return root.findValuesAsText("Id")
    }
}

fun getAllMetadata(pmid: String): Map<String, Any?> {
    val mainPaper = fetchPaperMetadata(pmid)
    val citingPmids = fetchCitingPapers(pmid)
    val citingPapers = citingPmids.mapNotNull { fetchPaperMetadata(it) }
    val citedPmids = fetchCitedPapers(pmid)
    val citedPapers = citedPmids.mapNotNull { fetchPaperMetadata(it) }

    return mapOf(
        "main_paper" to mainPaper,
        "citing_papers" to citingPapers,
        "cited_papers" to citedPapers
    )
}

fun main() {
    val pmid = "12345678"  // Replace with the PMID of the paper you're interested in
    val allMetadata = getAllMetadata(pmid)

    println("Main paper title: ${allMetadata["main_paper"]?.MedlineCitation?.Article?.ArticleTitle}")
    println("Number of citing papers: ${(allMetadata["citing_papers"] as List<*>).size}")
    println("Number of cited papers: ${(allMetadata["cited_papers"] as List<*>).size}")
}
*/
