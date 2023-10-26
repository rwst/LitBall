package org.reactome.lit_ball.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import org.reactome.lit_ball.model.AnnotatingRootStore
import org.reactome.lit_ball.model.Filtering2RootStore
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.service.S2Client
import org.reactome.lit_ball.service.S2Service
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.StringPatternMatcher
import org.reactome.lit_ball.util.Logger
import org.reactome.lit_ball.util.handleException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.*
import kotlin.io.path.Path

enum class QueryStatus { UNINITIALIZED, FILTERED2, EXPANDED, FILTERED1 }
@Serializable
enum class Qtype(val pretty: String) {
    EXPRESSION_SEARCH("Expression Search"),
    SNOWBALLING("Snowballing"),
    SUPERVISED_SNOWBALLING("Snowballing with Interleaved Supervision"),
}

fun getQueryDir(name: String): File {
    val queryPath = Settings.map["path-to-queries"] ?: ""
    val prefix = Settings.map["directory-prefix"] ?: ""
    val directory = File(queryPath)
    if (!directory.isDirectory || !directory.exists()) {
        throw IllegalArgumentException("Invalid directory path: $queryPath")
    }
    return File("$queryPath/$prefix$name")
}

fun getDOIs(dir: File, fileName: String): MutableSet<String> {
    val filePath = dir.absolutePath + "/" + fileName
    val doiFile = File(filePath)
    if (doiFile.exists() && doiFile.isFile && doiFile.canRead()) {
        return doiFile.readLines().map { it.uppercase() }.toMutableSet()
    }
    return mutableSetOf()
}

data class LitBallQuery(
    var id: Int,
    val name: String = "",
    var type: Qtype = Qtype.SUPERVISED_SNOWBALLING,
    var status: QueryStatus = QueryStatus.UNINITIALIZED,
    var setting: QuerySetting? = null,
    var acceptedSet: MutableSet<String> = mutableSetOf(),
    var rejectedSet: MutableSet<String> = mutableSetOf(),
    var lastExpansionDate: Date? = null,
    var noNewAccepted: Boolean = false
) {
    fun syncBuffers() {
        acceptedSet = getDOIs(getQueryDir(name), FileType.ACCEPTED.fileName).filter { it.isNotBlank() }.toMutableSet()
        rejectedSet = getDOIs(getQueryDir(name), FileType.REJECTED.fileName).filter { it.isNotBlank() }.toMutableSet()
    }

    fun nrAccepted() = acceptedSet.size
    fun nrRejected() = rejectedSet.size
    override fun toString(): String {
        return "Query(id=$id, name=$name, status=$status, setting=$setting, nrAccepted=${nrAccepted()}, nrRejected=${nrRejected()}, lastExpansionDate=$lastExpansionDate)"
    }

    fun nextActionText(): String {
        return arrayOf(
            "Complete the Setting",
            "Start expansion",
            "Automatic filtering",
            "Supervised filtering"
        )[status.ordinal]
    }

    fun getFileDate(fromFile: Boolean = false, fileType: FileType): Date? {
        return if (fromFile) {
            val queryDir = getQueryDir(name)
            if (queryDir.isDirectory && queryDir.canRead()) {
                val file = File("${queryDir.absolutePath}/${fileType.fileName}")
                if (file.canRead())
                    Date(file.lastModified())
                else
                    null
            } else
                null
        } else
            Date()
    }

    private val mutex = Mutex()

    suspend fun expand() {
        if (!mutex.tryLock()) return
        val tag = "EXPAND"
        val queryDir = getQueryDir(name)
        ExpandQueryCache.init(File("${queryDir.absolutePath}/${FileType.CACHE_EXPANDED.fileName}"))
        val (missingAccepted, doiSet) = ExpandQueryCache.get(acceptedSet)
        var nulls = 0
        val size = missingAccepted.size
        val result = S2Client.getRefs(missingAccepted.toList()) { doi, refs ->
            val rlist = refs.citations?.mapNotNull { cit -> cit.externalIds?.get("DOI")?.uppercase() } ?: emptyList()
            val clist = refs.references?.mapNotNull { cit -> cit.externalIds?.get("DOI")?.uppercase() } ?: emptyList()
            if (rlist.isEmpty() && clist.isEmpty())
                nulls += 1
            doiSet.addAll(rlist)
            doiSet.addAll(clist)
            ExpandQueryCache.add(doi, refs)
        }
        if (!result) {
            mutex.unlock()
            return
        }
        Logger.i(tag, "Received ${doiSet.size} DOIs")
        if (missingAccepted.isEmpty()) {
            RootStore.setInformationalDialog("Expansion complete. New DOIs can only emerge when new papers are published.\nSet \"cache-max-age-days\" to control when expansion cache should be deleted.")
            mutex.unlock()
            return
        }
        val newDoiSet = doiSet.minus(acceptedSet).minus(rejectedSet)
        Logger.i(tag, "${newDoiSet.size} new DOIs received. Writing to expanded...")
        if (nulls == size)
            RootStore.setInformationalDialog("""
                None of the $size DOIs was found on Semantic
                Scholar. Please check:
                1. are you searching outside the biomed or compsci fields?
                2. do the DOIs in the file "Query-xyz/accepted.txt" start with "10."?
            """.trimIndent())
        else
            RootStore.setInformationalDialog("Received ${doiSet.size} DOIs\n\n${newDoiSet.size} new DOIs received. Writing to expanded...")
        if (queryDir.isDirectory && queryDir.canWrite()) {
            val text = newDoiSet.joinToString("\n").uppercase() + "\n"
            status = try {
                File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").writeText(text)
                lastExpansionDate = getFileDate(fileType = FileType.ACCEPTED)
                QueryStatus.EXPANDED
            } catch (e: Exception) {
                handleException(e)
                QueryStatus.FILTERED2
            }
            RootStore.refreshList()
        }
        mutex.unlock()
    }

    suspend fun filter1() {
        if (!mutex.tryLock()) return
        val tag = "FILTER"
        val queryDir = getQueryDir(name)
        val paperDetailsList = mutableListOf<S2Service.PaperDetailsWithAbstract>()
        val rejectedDOIs: Set<String>
        if (setting == null)
            throw Exception("Can't happen")

        // Load and match details of DOIs in expanded.txt
        // Result goes into paperDetailsList
        if (queryDir.isDirectory && queryDir.canRead()) {
            val matcher = StringPatternMatcher(setting!!)
            val doiSet = getDOIs(queryDir, FileType.EXPANDED.fileName).toList()
            val result = S2Client.getPaperDetailsWithAbstract(doiSet) {
                val textsOfPaper: Set<String> = setOf(
                    it.title ?: "",
                    it.tldr?.get("text") ?: "",
                    it.abstract ?: ""
                )
                if (matcher.match(textsOfPaper.joinToString(" "), it.title?: ""))
                    paperDetailsList.add(it)
            }
            // Bail out on Cancel
            if (!result) {
                mutex.unlock()
                return
            }
            Logger.i(tag, "Retained ${paperDetailsList.size} records")
            val filteredDOIs = paperDetailsList.mapNotNull { it.externalIds?.get("DOI")?.uppercase() }
            rejectedDOIs = doiSet.toSet().minus(filteredDOIs.toSet())
            rejectedSet.addAll(rejectedDOIs)
        } else {
            handleException(IOException("Cannot access directory ${queryDir.absolutePath}"))
            mutex.unlock()
            return
        }
        uppercaseDois(paperDetailsList)
        sanitize(paperDetailsList)
        Logger.i(tag, "rejected ${rejectedDOIs.size} papers, write to rejected...")
        RootStore.setInformationalDialog("Retained ${paperDetailsList.size} records\n\nrejected ${rejectedDOIs.size} papers, write to rejected...")

        // Write filtered.txt if new matches exist
        val json = ConfiguredJson.get()
        if (queryDir.isDirectory && queryDir.canWrite()) {
            if (paperDetailsList.isEmpty()) {
                File("${queryDir.absolutePath}/${FileType.FILTERED1.fileName}").delete()
                File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").delete()
                status = QueryStatus.FILTERED2
                noNewAccepted = true
                writeNoNewAccepted()
                RootStore.refreshList()
                mutex.unlock()
                return
            }
            try {
                val file = File("${queryDir.absolutePath}/${FileType.FILTERED1.fileName}")
                file.writeText(
                    json.encodeToString(
                        paperDetailsList.mapIndexed { idx, pd -> Paper(idx, pd) })
                )
                mergeIntoArchive(paperDetailsList)
            } catch (e: Exception) {
                handleException(e)
                mutex.unlock()
                return
            }
            val text = rejectedDOIs.joinToString("\n") + "\n"
            try {
                File("${queryDir.absolutePath}/${FileType.REJECTED.fileName}").appendText(text)
            } catch (e: Exception) {
                handleException(e)
                mutex.unlock()
                return
            }
        }
        File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").delete()
        status = QueryStatus.FILTERED1
        RootStore.refreshList()
        mutex.unlock()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun mergeIntoArchive(list: MutableList<S2Service.PaperDetailsWithAbstract>) {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canRead()) {
            val file: File
            try {
                file = File("${queryDir.absolutePath}/${FileType.ARCHIVED.fileName}")
            } catch (e: Exception) {
                handleException(e)
                return
            }
            val json = ConfiguredJson.get()
            val papers: MutableSet<Paper> = if (file.exists()) {
                json.decodeFromStream<List<Paper>>(file.inputStream()).toMutableSet()
            } else {
                mutableSetOf()
            }
            val details: MutableSet<S2Service.PaperDetailsWithAbstract> = papers.map { it.details }.toMutableSet()
            details.addAll(list)
            file.writeText(
                json.encodeToString(
                    details.mapIndexed { idx, pd -> Paper(idx, pd) })
            )
        }
    }

    suspend fun filter2() {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canRead()) {
            val file: File
            try {
                file = File("${queryDir.absolutePath}/${FileType.FILTERED1.fileName}")
            } catch (e: Exception) {
                handleException(e)
                return
            }
            PaperList.setFromQuery(this, file)
            Filtering2RootStore.refreshList()
        } else {
            handleException(IOException("Cannot access directory ${queryDir.absolutePath}"))
            return
        }
    }

    suspend fun annotate() {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canRead()) {
            val file: File
            try {
                file = File("${queryDir.absolutePath}/${FileType.ARCHIVED.fileName}")
            } catch (e: Exception) {
                handleException(e)
                return
            }
            PaperList.setFromQuery(this, file, acceptedSet)
            AnnotatingRootStore.refreshList()
            PaperList.saveAnnotated()
        } else {
            handleException(IOException("Cannot access directory ${queryDir.absolutePath}"))
            return
        }
    }

    suspend fun writeNoNewAccepted() {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canWrite()) {
            val text = noNewAccepted.toString()
            try {
                File("${queryDir.absolutePath}/${FileType.NONEWACCEPTED.fileName}").writeText(text)
            } catch (e: Exception) {
                handleException(e)
            }
        }
        val path = "${queryDir.absolutePath}/${FileType.ACCEPTED.fileName}"
        val now = FileTime.fromMillis(System.currentTimeMillis())
        withContext(Dispatchers.IO) {
            Files.setLastModifiedTime(Path(path), now)
        }
    }

    fun readNoNewAccepted(): Boolean {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canRead()) {
            try {
                return File("${queryDir.absolutePath}/${FileType.NONEWACCEPTED.fileName}").readText().trim() == "true"
            } catch (e: FileNotFoundException) {
                return false
            } catch (e: Exception) {
                handleException(e)
            }
        }
        return false
    }

    fun saveSettings() {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canWrite()) {
            val json = ConfiguredJson.get()
            val text = json.encodeToString<QuerySetting>(setting ?: QuerySetting())
            try {
                File("${queryDir.absolutePath}/${FileType.SETTINGS.fileName}").writeText(text)
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
}

@Suppress("SENSELESS_COMPARISON")
private fun sanitizeMap(map: Map<String, String>?, onChanged: (MutableMap<String, String>) -> Unit) {
    val extIds = map?.toMutableMap()
    extIds?.entries?.forEach {
        if (it.value == null) {
            extIds.remove(it.key)
            onChanged(extIds)
        }
    }
}

private fun sanitize(list: MutableList<S2Service.PaperDetailsWithAbstract>) {
    list.forEachIndexed { index, paper ->
        val newPaper = paper.copy()
        var isChanged = false
        sanitizeMap(paper.externalIds) {
            newPaper.externalIds = it
            isChanged = true
        }
        sanitizeMap(paper.tldr) {
            newPaper.tldr = it
            isChanged = true
        }
        if (isChanged)
            list[index] = newPaper
    }
}

fun uppercaseDois(list: MutableList<S2Service.PaperDetailsWithAbstract>) {
    list.forEach {
        val doi = it.externalIds?.get("DOI")
        if (it.externalIds != null && doi != null) {
            val upperDoi = doi.uppercase()
            if (doi != upperDoi) {
                val map = it.externalIds!!.toMutableMap()
                map["DOI"] = upperDoi
                it.externalIds = map
            }
        }
    }
}