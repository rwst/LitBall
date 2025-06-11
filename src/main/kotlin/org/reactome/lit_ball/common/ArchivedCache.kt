package common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import service.S2Interface
import util.ConfiguredJson
import java.io.File

// The ArchivedCache stores Paper data as list in JSON. The data is enriched by more details from the AGService
// right before export. It is also enriched by flags during Annotating.
object ArchivedCache {
    private lateinit var file: File
    private lateinit var json: Json
    fun init(queryDir: File) {
        file = File("${queryDir.absolutePath}/${FileType.ARCHIVED.fileName}")
        json = ConfiguredJson.get()
    }

    /**
     * Populates a set of `Paper` objects from a given list of `PaperDetails`, assigns default tags and paper IDs,
     * and archives the resulting set of papers by writing them to a persistent storage.
     *
     * @param list A mutable list of `PaperDetails` objects, each containing metadata about individual papers.
     */
    suspend fun set(list: MutableList<S2Interface.PaperDetails>) {
        val papers: MutableSet<Paper> = emptySet<Paper>().toMutableSet()
        papers.addAll(list.map {
            Paper(0L, it).apply {   // since uniqueId is reset on read, we can zero it here
                tag = Tag.Accepted
                setPaperIdFromDetails()
            }})
        writeArchivedPapers(papers)
    }

    /**
     * Merges a given list of `PaperDetails` into the existing set of archived papers, adding
     * new papers that are not already present in the archive.
     *
     * @param list A mutable list of `S2Interface.PaperDetails` to be merged into the
     * archived dataset.
     */
    suspend fun merge(list: MutableList<S2Interface.PaperDetails>) {
        val papers: MutableSet<Paper> = loadArchivedPapers()
        val details: Set<S2Interface.PaperDetails> = papers.map { it.details }.toSet()
        papers.addAll(list.filterNot { details.contains(it) }
            .map {
                Paper(0L, it).apply {
                    tag = Tag.Accepted
                    setPaperIdFromDetails()
                }
            })
        writeArchivedPapers(papers)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadArchivedPapers(): MutableSet<Paper> =
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    try {
                        json.decodeFromStream<List<Paper>>(file.inputStream()).toMutableSet()
                    } catch (e: SerializationException) {
                        throw IllegalStateException("Failed to decode archived papers: ${e.message}")
                    }
                } else {
                    mutableSetOf()
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to read archived papers: ${e.message}")
            }
        }

    suspend fun writeArchivedPapers(papers: Set<Paper>) {
        papers.forEach { it.tag = Tag.Accepted }
        withContext(Dispatchers.IO) {
            try {
                file.writeText(json.encodeToString(papers))
            } catch (e: Exception) {
                throw IllegalStateException("Failed to write archived papers: ${e.message}")
            }
        }
    }
}

