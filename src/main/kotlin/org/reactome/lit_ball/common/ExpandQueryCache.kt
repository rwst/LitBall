package common

import kotlinx.serialization.encodeToString
import service.S2Interface
import util.ConfiguredJson
import util.ConfiguredUglyJson
import java.io.File
import java.util.*
import kotlin.properties.Delegates

object ExpandQueryCache {
    private lateinit var file: File
    private var maxAge by Delegates.notNull<Int>()
    private const val MILLISECONDS_PER_DAY = 1000L * 3600 * 24
    fun init(queryDir: File) {
        file = File("${queryDir.absolutePath}/${FileType.CACHE_EXPANDED.fileName}")
        maxAge = Settings.map["cache-max-age-days"]?.toInt() ?: 31
    }

    fun get(doiSet: MutableSet<String>): Pair<Set<String>, MutableSet<String>> {
        if (!file.exists()) {
            return Pair(doiSet, mutableSetOf())
        }
        val date = Date(file.lastModified()).time
        val now = Date().time
        if (now - date > maxAge * MILLISECONDS_PER_DAY) {
            file.delete()
            return Pair(doiSet, mutableSetOf())
        }
        val json = ConfiguredJson.get()
        val refs = mutableSetOf<Pair<String, List<String>>>()
        val lines = file.readLines()
        lines.forEach {
            if (it.isNotBlank())
                refs += json.decodeFromString<Pair<String, List<String>>>(it)
        }
        val doisFound = refs.map { it.first }
        val missingDois = doiSet.minus(doisFound.toSet())
        val refDois = mutableSetOf<String>()
        refs.forEach {
            refDois.addAll(it.second)
        }
        return Pair(missingDois, refDois)
    }

    fun add(doi: String, refs: S2Interface.PaperRefs) {
        val json = ConfiguredUglyJson.get()
        val dois: MutableSet<String> = mutableSetOf()
        dois.addAll(refs.citations?.mapNotNull { cit -> cit.externalIds?.get("DOI")?.lowercase() } ?: emptyList())
        dois.addAll(refs.references?.mapNotNull { cit -> cit.externalIds?.get("DOI")?.lowercase() } ?: emptyList())
        file.appendText(json.encodeToString(Pair(doi, dois.toList())) + "\n")
    }
}