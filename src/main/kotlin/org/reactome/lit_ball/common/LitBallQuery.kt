package org.reactome.lit_ball.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import model.AnnotatingRootStore
import org.reactome.lit_ball.model.Filtering2RootStore
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.service.AGService
import org.reactome.lit_ball.service.S2Interface
import org.reactome.lit_ball.service.getAGService
import org.reactome.lit_ball.util.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.*
import kotlin.io.path.Path

enum class QueryStatus { UNINITIALIZED, FILTERED2, EXPANDED, FILTERED1, EXPLODED }

const val EXPLODED_LIMIT = 20000

fun getQueryDir(name: String): File {
    val queryPath = Settings.map["path-to-queries"] ?: ""
    val prefix = Settings.map["directory-prefix"] ?: ""
    val directory = File(queryPath)
    if (!directory.isDirectory || !directory.exists()) {
        throw IllegalArgumentException("Invalid directory path: $queryPath")
    }
    return File("$queryPath/$prefix$name")
}

fun getDOIs(dir: File, fileType: FileType): MutableSet<String> {
    val filePath = dir.absolutePath + "/" + fileType.fileName
    val doiFile = File(filePath)
    if (doiFile.exists() && doiFile.isFile && doiFile.canRead()) {
        return doiFile.readLines().map { it.uppercase() }.toMutableSet()
    }
    return mutableSetOf()
}

data class LitBallQuery(
    var id: Int,
    val name: String = "",
    var type: QueryType = QueryType.SUPERVISED_SNOWBALLING,
    var status: MutableState<QueryStatus> = mutableStateOf(QueryStatus.UNINITIALIZED),
    var setting: QuerySetting = QuerySetting(),
    var acceptedSet: MutableSet<String> = mutableSetOf(),
    var rejectedSet: MutableSet<String> = mutableSetOf(),
    var lastExpansionDate: Date? = null,
    var noNewAccepted: Boolean = false,
    var expSearchParams: Pair<String, BooleanArray>? = null,
    var agService: AGService = getAGService(),
) {
    init {
        setting.type = type
        expSearchParams?.let { pair ->
            setting.pubDate = pair.first
            setting.pubType =
                ArticleType.entries.map { it.s2name }
                    .zip(pair.second.toList())
                    .filter { it.second }
                    .map { it.first }
        }
    }

    fun syncBuffers() {
        acceptedSet = getDOIs(getQueryDir(name), FileType.ACCEPTED).filter { it.isNotBlank() }.toMutableSet()
        rejectedSet = getDOIs(getQueryDir(name), FileType.REJECTED).filter { it.isNotBlank() }.toMutableSet()
    }

    fun nrAccepted() = acceptedSet.size
    fun nrRejected() = rejectedSet.size
    override fun toString(): String {
        return "Query(id=$id, name=$name, status=$status, setting=$setting, nrAccepted=${nrAccepted()}, nrRejected=${nrRejected()}, lastExpansionDate=$lastExpansionDate)"
    }

    fun nextActionText(): String = when (type) {
        QueryType.EXPRESSION_SEARCH ->
            arrayOf(
                "Complete the Setting",
                "Search",
                "Search",
                "Search",
            )[status.value.ordinal]

        QueryType.SNOWBALLING ->
            arrayOf(
                "Complete the Setting",
                "Start expansion",
                "Start expansion",
                "Start expansion",
            )[status.value.ordinal]

        QueryType.SUPERVISED_SNOWBALLING ->
            arrayOf(
                "Complete the Setting",
                "Start expansion",
                "Automatic filtering",
                "Supervised filtering"
            )[status.value.ordinal]

        QueryType.SIMILARITY_SEARCH ->
            arrayOf(
                "Search",
                "Search",
                "Search",
                "Search",
            )[status.value.ordinal]
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
        when (type) {
            QueryType.EXPRESSION_SEARCH -> expressionSearch()
            QueryType.SNOWBALLING -> autoSnowBall()
            QueryType.SUPERVISED_SNOWBALLING -> snowBall()
            QueryType.SIMILARITY_SEARCH -> similaritySearch()
        }
        mutex.unlock()
        return
    }

    private suspend fun snowBall(auto: Boolean = false) {
        val tag = "EXPAND"
        val queryDir = getQueryDir(name)
        ExpandQueryCache.init(queryDir)
        val (missingAccepted, doiSet) = ExpandQueryCache.get(acceptedSet)
        var nulls = 0
        val size = missingAccepted.size
        val result = agService.getRefs(missingAccepted.toList()) { doi, refs ->
            val rlist = refs.citations?.let { idListFromPaperRefs(it) } ?: emptyList()
            val clist = refs.references?.let { idListFromPaperRefs(it) } ?: emptyList()
            if (rlist.isEmpty() && clist.isEmpty())
                nulls += 1
            doiSet.addAll(rlist)
            doiSet.addAll(clist)
            ExpandQueryCache.add(doi, refs)
        }
        if (!result) return
        Logger.i(tag, "Received ${doiSet.size} DOIs")
        if (missingAccepted.isEmpty()) {
            if (!auto)
                RootStore.setInformationalDialog("Expansion complete. New DOIs can only emerge when new papers are published.\nSet \"cache-max-age-days\" to control when expansion cache should be deleted.")
            status.value = QueryStatus.FILTERED2
            return
        }
        val newDoiSet = doiSet.minus(acceptedSet).minus(rejectedSet)
        if (auto && newDoiSet.size > EXPLODED_LIMIT) {
            status.value = QueryStatus.EXPLODED
            return
        }
        Logger.i(tag, "${newDoiSet.size} new DOIs received. Writing to expanded...")
        if (!auto) {
            if (nulls == size)
                RootStore.setInformationalDialog(
                    """
                    None of the $size DOIs was found on Semantic
                    Scholar. Please check:
                    1. are you searching outside the biomed or compsci fields?
                    2. do the DOIs in the file "Query-xyz/accepted.txt" start with "10."?
                """.trimIndent()
                )
            else
                RootStore.setInformationalDialog("Received ${doiSet.size} DOIs\n\n${newDoiSet.size} new DOIs received. Writing to expanded...")
        }
        if (nulls == size) {
            if (!auto)
                RootStore.setInformationalDialog("Expansion complete. New DOIs can only emerge when new papers are published.\nSet \"cache-max-age-days\" to control when expansion cache should be deleted.")
            status.value = QueryStatus.FILTERED2
            return
        }
        checkFileInDirectory(queryDir, FileType.EXPANDED)?.let { file ->
            val text = newDoiSet.joinToString("\n").uppercase() + "\n"
            status.value = try {
                file.writeText(text)
                lastExpansionDate = getFileDate(fileType = FileType.ACCEPTED)
                QueryStatus.EXPANDED
            } catch (e: Exception) {
                handleException(e)
                QueryStatus.FILTERED2
            }
//            RootStore.refreshList()
        }
    }

    suspend fun filter1(auto: Boolean = false) {
        if (!mutex.tryLock()) return
        val tag = "FILTER"
        val queryDir = getQueryDir(name)
        val paperDetailsList = mutableListOf<S2Interface.PaperDetails>()
        val rejectedDOIs: Set<String>

        // Load and match details of DOIs in expanded.txt
        // Result goes into paperDetailsList
        if (queryDir.isDirectory && queryDir.canRead()) {
            val matcher = StringPatternMatcher(setting)
            val doiSet = getDOIs(queryDir, FileType.EXPANDED).toList()
            val result = agService.getPaperDetails(doiSet,
                fields = "paperId,externalIds,title,abstract,publicationTypes,tldr,publicationDate",
            ) {
                val textsOfPaper: Set<String> = setOf(
                    it.title ?: "",
                    it.tldr?.get("text") ?: "",
                    it.abstract ?: ""
                )
                if (matcher.match(textsOfPaper.joinToString(" "), it.title ?: ""))
                    paperDetailsList.add(it)
            }
            // Bail out on Cancel
            if (!result) {
                mutex.unlock()
                return
            }
            Logger.i(tag, "Retained ${paperDetailsList.size} records")
            val filteredDOIs = idListFromPaperDetailsList(paperDetailsList)
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
        if (!auto)
            RootStore.setInformationalDialog("Retained ${paperDetailsList.size} records\n\nrejected ${rejectedDOIs.size} papers, write to rejected...")

        // Write filtered.txt if new matches exist
        val json = ConfiguredJson.get()
        if (queryDir.isDirectory && queryDir.canWrite()) {
            if (paperDetailsList.isEmpty()) {
                File("${queryDir.absolutePath}/${FileType.FILTERED1.fileName}").delete()
                File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").delete()
                status.value = QueryStatus.FILTERED2
                noNewAccepted = true
                writeNoNewAccepted()
                mutex.unlock()
                return
            }
            try {
                val file = File("${queryDir.absolutePath}/${FileType.FILTERED1.fileName}")
                file.writeText(
                    json.encodeToString(
                        paperDetailsList.mapIndexed { idx, pd ->
                            Paper(idx, pd).uppercaseDoi().setPaperIdFromDetails()
                        })
                )
                mergeIntoArchive(paperDetailsList)
            } catch (e: Exception) {
                handleException(e)
                mutex.unlock()
                return
            }
            val text = rejectedDOIs.joinToString("\n").uppercase() + "\n"
            try {
                File("${queryDir.absolutePath}/${FileType.REJECTED.fileName}").appendText(text)
            } catch (e: Exception) {
                handleException(e)
                mutex.unlock()
                return
            }
        }
        File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").delete()
        status.value = QueryStatus.FILTERED1
        mutex.unlock()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun mergeIntoArchive(list: MutableList<S2Interface.PaperDetails>) {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.ARCHIVED)?.let { file ->
            val json = ConfiguredJson.get()
            val papers: MutableSet<Paper> = if (file.exists()) {
                json.decodeFromStream<List<Paper>>(file.inputStream()).toMutableSet()
            } else {
                mutableSetOf()
            }
            val details: MutableSet<S2Interface.PaperDetails> = papers.map { it.details }.toMutableSet()
            details.addAll(list)
            file.writeText(
                json.encodeToString(
                    details.mapIndexed { idx, pd -> Paper(idx, pd).uppercaseDoi().setPaperIdFromDetails() })
            )
        }
    }

    private suspend fun expressionSearch() {
        val tag = "EXPRSEARCH"
        val queryDir = getQueryDir(name)
        val paperDetailsList = mutableListOf<S2Interface.PaperDetails>()

        val matcher = StringPatternMatcher(setting)
        val dateMatcher = DateMatcher(expSearchParams?.first)
        val result = agService.getBulkPaperSearch(setting) {
            if (typeMatches(it.publicationTypes, expSearchParams?.second)
                && dateMatcher.matches(it.publicationDate)
                && !matcher.parser2.match(it.title ?: "")
            )
                paperDetailsList.add(it)
        }
        // Bail out on Cancel
        if (!result) return
        Logger.i(tag, "Retained ${paperDetailsList.size} records")
        uppercaseDois(paperDetailsList)
        sanitize(paperDetailsList)
        RootStore.setInformationalDialog("Received ${paperDetailsList.size} records\naccepting all. Query finished.")

        acceptedSet = idSetFromPaperDetailsList(paperDetailsList)
        checkFileInDirectory(queryDir, FileType.ACCEPTED)?.let { file ->
            file.writeText(acceptedSet.joinToString("\n"))
            mergeIntoArchive(paperDetailsList)
        }
        noNewAccepted = true
        writeNoNewAccepted()
        status.value = QueryStatus.FILTERED2
    }

    // Similarity Search will add 20 new papers. User deletes as much as wanted. Following clicks on Search will
    // add the same amount of what remains accepted, but at least 20.
    private suspend fun similaritySearch() {
        val queryDir = getQueryDir(name)
        val paperDetailsList = mutableListOf<S2Interface.PaperDetails>()
        val ids = acceptedSet.toMutableList()
        val result = agService.getSimilarDetails(ids) {
            paperDetailsList.add(it)
        }
        // Bail out on Cancel
        if (!result) return
        Logger.i(tag, "Retained ${paperDetailsList.size} records")
        uppercaseDois(paperDetailsList)
        sanitize(paperDetailsList)
        RootStore.setInformationalDialog("Received ${paperDetailsList.size} records\naccepting all. Query finished.")

        acceptedSet = ids.plus(idSetFromPaperDetailsList(paperDetailsList)).toMutableSet()
        checkFileInDirectory(queryDir, FileType.ACCEPTED)?.let { file ->
            file.writeText(acceptedSet.joinToString("\n"))
            mergeIntoArchive(paperDetailsList)
        }
        status.value = QueryStatus.FILTERED2
    }

    suspend fun filter2() {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.FILTERED1)?.let { file ->
            PaperList.setFromQuery(this, file)
            Filtering2RootStore.state.paperListStore.refreshList()
        }
    }

    private suspend fun acceptAll() {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.FILTERED1)?.let { file ->
            PaperList.setFromQuery(this, file)
            PaperList.listHandle.setFullAllTags(Tag.Accepted)
            PaperList.finish(true)
            syncBuffers()
        }
    }

    private suspend fun autoSnowBall() {
        while (true) {
            snowBall(true)
            if (status.value == QueryStatus.FILTERED2 || status.value == QueryStatus.EXPLODED) break
            mutex.unlock()
            filter1(true)
            mutex.tryLock()
            if (status.value == QueryStatus.FILTERED2) break
            acceptAll()
        }
        if (status.value == QueryStatus.EXPLODED) {
            RootStore.setInformationalDialog(
                """
                Number of new DOIs exceeds EXPLODED_LIMIT of $EXPLODED_LIMIT.
                Please try again with more specific keywords / expression.
                """.trimIndent()
            )
            status.value = QueryStatus.FILTERED2
        }
    }

    suspend fun annotate() {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.ARCHIVED)?.let { file ->
            PaperList.setFromQuery(this, file, acceptedSet)
            AnnotatingRootStore.state.paperListStore.refreshList()
            PaperList.saveAnnotated()
        }
    }

    suspend fun writeNoNewAccepted() {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.NONEWACCEPTED)?.let { file ->
            val text = noNewAccepted.toString()
            file.writeText(text)
        }
        val path = "${queryDir.absolutePath}/${FileType.ACCEPTED.fileName}"
        val now = FileTime.fromMillis(System.currentTimeMillis())
        withContext(Dispatchers.IO) {
            Files.setLastModifiedTime(Path(path), now)
        }
    }

    fun readNoNewAccepted(): Boolean {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.NONEWACCEPTED)?.let { file ->
            try {
                return file.readText().trim() == "true"
            } catch (e: FileNotFoundException) {
                return false
            }
        }
        return false
    }

    fun saveSettings() {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.SETTINGS)?.let { file ->
            val json = ConfiguredJson.get()
            val text = json.encodeToString<QuerySetting>(setting)
            file.writeText(text)
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

private fun sanitize(list: MutableList<S2Interface.PaperDetails>) {
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

fun uppercaseDois(list: MutableList<S2Interface.PaperDetails>) {
    list.forEach {
        val extIds = it.externalIds?.toMutableMap()
        if (extIds != null) {
            val doi = extIds["DOI"]
            if (doi != null) {
                val upperDoi = doi.uppercase()
                if (doi != upperDoi) {
                    extIds["DOI"] = upperDoi
                    it.externalIds = extIds
                }
            }
        }
    }
}