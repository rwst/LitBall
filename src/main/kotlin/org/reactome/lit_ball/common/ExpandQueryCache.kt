package common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import service.S2Interface
import util.ConfiguredJson
import util.ConfiguredUglyJson
import java.io.File
import kotlin.properties.Delegates

object ExpandQueryCache {
    private lateinit var file: File
    private var maxAge by Delegates.notNull<Int>()
    private const val MILLISECONDS_PER_DAY = 1000L * 3600 * 24
    fun init(queryDir: File) {
        file = File("${queryDir.absolutePath}/${FileType.CACHE_EXPANDED.fileName}")
        maxAge = Settings.map["cache-max-age-days"]?.toInt() ?: 31
    }

    suspend fun get(doiSet: MutableSet<String>): Pair<Set<String>, MutableSet<String>> {
        if (!file.exists()) {
            return Pair(doiSet, mutableSetOf())
        }
        val date = file.lastModified()
        val now = System.currentTimeMillis()
        return withContext(Dispatchers.IO) {
            if (now - date > maxAge * MILLISECONDS_PER_DAY) {
                file.delete()
                Pair(doiSet, mutableSetOf())
            }
            else {
                val json = ConfiguredJson.get()
                val lines = file.readLines()
                val refs = lines.filter { it.isNotBlank() }
                    .map { json.decodeFromString<Pair<String, List<String>>>(it) }
                    .toSet()
                val doisFound = refs.map { it.first }.toSet()
                val missingDois = doiSet.minus(doisFound)
                val refDois = refs.flatMap { it.second }.toMutableSet()
                Pair(missingDois, refDois)
            }
        }
    }

    suspend fun add(doi: String, refs: S2Interface.PaperRefs) {
        val json = ConfiguredUglyJson.get()
        val dois: MutableSet<String> = mutableSetOf()
        dois.addAll(refs.citations?.mapNotNull { cit -> cit.externalIds?.get("DOI")?.lowercase() } ?: emptyList())
        dois.addAll(refs.references?.mapNotNull { cit -> cit.externalIds?.get("DOI")?.lowercase() } ?: emptyList())
        withContext(Dispatchers.IO) {
            file.appendText(json.encodeToString(Pair(doi, dois.toList())) + "\n")
        }
    }
}