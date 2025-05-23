package common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import model.AnnotatingRootStore
import model.Filtering2RootStore
import model.RootStore
import service.AGService
import service.S2Interface
import service.getAGService
import util.*
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
        return doiFile.readLines().map { it.lowercase() }.toMutableSet()
    }
    return mutableSetOf()
}

data class LitBallQuery(
    var id: Int,
    val name: String = "",
    var type: QueryType = QueryType.SUPERVISED_SNOWBALLING,
    val status: MutableState<QueryStatus> = mutableStateOf(QueryStatus.UNINITIALIZED),
    var setting: QuerySetting = QuerySetting(),
    var acceptedSet: MutableSet<String> = mutableSetOf(),
    var rejectedSet: MutableSet<String> = mutableSetOf(),
    var lastExpansionDate: Date? = null,
    var noNewAccepted: Boolean = false,
    var expSearchParams: Pair<String, BooleanArray>? = null,
    val agService: AGService = getAGService(),
) {
    init {
        setting.type = type
        expSearchParams?.let { pair ->
            setting.pubDate = pair.first
            setting.pubType.clear()
            setting.pubType.addAll(
                ArticleType.entries.map { it.s2name }
                    .zip(pair.second.toList())
                    .filter { it.second }
                    .map { it.first }
            )
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
        QueryType.EXPRESSION_SEARCH -> when (status.value) {
            QueryStatus.UNINITIALIZED -> "Complete the Setting"
            else -> "Search"
        }

        QueryType.SNOWBALLING -> when (status.value) {
            QueryStatus.UNINITIALIZED -> "Complete the Setting"
            else -> "Start expansion"
        }

        QueryType.SUPERVISED_SNOWBALLING -> when (status.value) {
            QueryStatus.UNINITIALIZED -> "Complete the Setting"
            QueryStatus.EXPANDED -> "Automatic filtering"
            QueryStatus.FILTERED1 -> "Supervised filtering"
            else -> "Start expansion"
        }

        QueryType.SIMILARITY_SEARCH -> "Search"
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
        suspend fun fetchMissingReferences(): Pair<Set<String>, MutableSet<String>> {
            ExpandQueryCache.init(getQueryDir(name))
            return ExpandQueryCache.get(acceptedSet)
        }

        suspend fun fetchMissingAccepted(missingAccepted: Set<String>, allLinkedDois: MutableSet<String>):
                Pair<Boolean, Int> {
            var nulls = 0
            return Pair(agService.getRefs(missingAccepted.toList()) { doi, refs ->
                val rlist = refs.citations?.let { idListFromPaperRefs(it) } ?: emptyList()
                val clist = refs.references?.let { idListFromPaperRefs(it) } ?: emptyList()
                if (rlist.isEmpty() && clist.isEmpty())
                    nulls += 1
                allLinkedDois.addAll(rlist)
                allLinkedDois.addAll(clist)
                ExpandQueryCache.add(doi, refs)
            }, nulls)
        }

        suspend fun writeExpandedFile(newDoiSet: Set<String>) {
            if (writeFile(getQueryDir(name),
                    FileType.EXPANDED,
                    newDoiSet.joinToString("\n").lowercase() + "\n")
                ) {
                lastExpansionDate = getFileDate(fileType = FileType.ACCEPTED)
                status.value = QueryStatus.EXPANDED
            }
            else
            {
                status.value = QueryStatus.FILTERED2
            }
        }
        val (missingAccepted, allLinkedDois) = fetchMissingReferences()
        val nrMissing = missingAccepted.size
        var allNullsMissing = false
        if (nrMissing != 0) {
            val (status, nulls) = fetchMissingAccepted(missingAccepted, allLinkedDois)
            allNullsMissing = nulls == nrMissing
            if (!status) return
        }
        Logger.i(tag, "New snowball size: ${allLinkedDois.size}")

        val newDoiSet = allLinkedDois.minus(acceptedSet).minus(rejectedSet)
        if (auto && newDoiSet.size > EXPLODED_LIMIT) {
            status.value = QueryStatus.EXPLODED
            return
        }
        Logger.i(tag, "${newDoiSet.size} new DOIs. Writing to expanded...")

        if (!auto) {
            if (nrMissing != 0 && allNullsMissing)
                RootStore.setInformationalDialog(
                    """
                    None of the $nrMissing DOIs was found on Semantic
                    Scholar. Please check:
                    1. are you searching outside the biomed or compsci fields?
                    2. do the DOIs in the file "Query-xyz/accepted.txt" start with "10."?
                """.trimIndent()
                )
            else
                RootStore.setInformationalDialog(
                    """
                    Accepted Dois: ${acceptedSet.size}. Not cached: $nrMissing
                    Updated snowball size: ${allLinkedDois.size}
                    New DOIs: ${newDoiSet.size}. Writing to expanded...
                """.trimIndent()
                )
        }
        if (newDoiSet.isEmpty() && nrMissing != 0 && allNullsMissing) {
            if (!auto)
                RootStore.setInformationalDialog("Expansion complete. New DOIs can only emerge when new papers are published.\nSet \"cache-max-age-days\" to control when expansion cache should be deleted.")
            status.value = QueryStatus.FILTERED2
            return
        }
        writeExpandedFile(newDoiSet)
    }

    suspend fun filter1(auto: Boolean = false) {
        val tag = "FILTER"
        val queryDir = getQueryDir(name)
        suspend fun fetchDetails(
            paperDetailsList: MutableList<S2Interface.PaperDetails>,
            rejectedDOIs: MutableSet<String>
        ): Boolean {
            // Load and match details of DOIs in expanded.txt
            // Result goes into paperDetailsList
            if (queryDir.isDirectory && queryDir.canRead()) {
                val matcher = StringPatternMatcher(setting)
                val doiSet = getDOIs(queryDir, FileType.EXPANDED).minus(acceptedSet).minus(rejectedSet).toList()
                val result = agService.getPaperDetails(
                    doiSet,
                    fields = "paperId,externalIds,title,abstract,publicationTypes,tldr,publicationDate",
                ) {
                    val textsOfPaper: Set<String> = setOf(
                        it.title ?: "",
                        it.tldr?.get("text") ?: "",
                        it.abstract ?: ""
                    )
                    it.authors = null    // reset unasked for author data
                    if (matcher.match(textsOfPaper.joinToString(" "), it.title ?: ""))
                        paperDetailsList.add(it)
                }
                // Bail out on Cancel
                if (!result) {
                    return false
                }
                Logger.i(tag, "Retained ${paperDetailsList.size} records")
                val filteredDOIs = idListFromPaperDetailsList(paperDetailsList)
                rejectedDOIs.addAll(doiSet.toSet().minus(filteredDOIs.toSet()))
                rejectedSet.addAll(rejectedDOIs)
            } else {
                handleException(IOException("Cannot access directory ${queryDir.absolutePath}"))
                return false
            }
            lowercaseDois(paperDetailsList)
            sanitize(paperDetailsList)
            return true
        }

        suspend fun writeFiltered(paperDetailsList: MutableList<S2Interface.PaperDetails>): Boolean {
            // Write filtered.txt if new matches exist
            val json = ConfiguredJson.get()
            if (queryDir.isDirectory && queryDir.canWrite()) {
                if (paperDetailsList.isEmpty()) {
                    File("${queryDir.absolutePath}/${FileType.FILTERED1.fileName}").delete()
                    File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").delete()
                    status.value = QueryStatus.FILTERED2
                    noNewAccepted = true
                    writeNoNewAccepted()
                    return false
                }
                if (!writeFile(
                        queryDir, FileType.FILTERED1,
                        json.encodeToString(
                            paperDetailsList.mapIndexed { idx, pd ->
                                Paper(idx, pd).setPaperIdFromDetails()
                            })
                    )
                )
                    return false
                ArchivedCache.init(queryDir)
                ArchivedCache.merge(paperDetailsList)
            }
            return true
        }

        suspend fun writeRejected(rejectedDOIs: MutableSet<String>): Boolean {
            val text = rejectedDOIs.joinToString("\n").lowercase() + "\n"
            try {
                withContext(Dispatchers.IO) {
                    File("${queryDir.absolutePath}/${FileType.REJECTED.fileName}").appendText(text)
                }
            } catch (e: Exception) {
                handleException(e)
                return false
            }
            return true
        }
        return mutex.withLock {
            val paperDetailsList = mutableListOf<S2Interface.PaperDetails>()
            val rejectedDOIs = mutableSetOf<String>()

            if (!fetchDetails(paperDetailsList, rejectedDOIs)) return

            Logger.i(tag, "rejected ${rejectedDOIs.size} papers, write to rejected...")
            if (!auto) {
                RootStore.setInformationalDialog("Retained ${paperDetailsList.size} records\n\nrejected ${rejectedDOIs.size} papers, write to rejected...")
            }

            if (!writeFiltered(paperDetailsList)) return
            if (!writeRejected(rejectedDOIs)) return
            File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").delete()
            status.value = QueryStatus.FILTERED1
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
        lowercaseDois(paperDetailsList)
        sanitize(paperDetailsList)
        RootStore.setInformationalDialog("Received ${paperDetailsList.size} records\naccepting all. Query finished.")

        acceptedSet = idSetFromPaperDetailsList(paperDetailsList)
        if (!writeFile(queryDir, FileType.ACCEPTED,
            acceptedSet.joinToString("\n")
            ))
            return
        ArchivedCache.init(queryDir)
        ArchivedCache.merge(paperDetailsList)
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
        lowercaseDois(paperDetailsList)
        sanitize(paperDetailsList)
        RootStore.setInformationalDialog("Received ${paperDetailsList.size} records\naccepting all. Query finished.")

        acceptedSet = ids.plus(idSetFromPaperDetailsList(paperDetailsList)).toMutableSet()
        if (!writeFile(queryDir, FileType.ACCEPTED,
            acceptedSet.joinToString("\n")))
            return
        ArchivedCache.init(queryDir)
        ArchivedCache.set(paperDetailsList)
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
        // TODO: why read it again from file?
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
        // Update the last modified time of the accepted file to indicate when it was last processed
        val path = "${queryDir.absolutePath}/${FileType.ACCEPTED.fileName}"
        val now = FileTime.fromMillis(System.currentTimeMillis())
        withContext(Dispatchers.IO) {
            Files.setLastModifiedTime(Path(path), now)
        }
    }

    suspend fun readNoNewAccepted(): Boolean {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.NONEWACCEPTED)?.let { file ->
            return try {
                file.readText().trim() == "true"
            } catch (_: FileNotFoundException) {
                false
            }
        }
        return false
    }

    suspend fun saveSettings() {
        writeFile(getQueryDir(name), FileType.SETTINGS,
            ConfiguredJson.get().encodeToString<QuerySetting>(setting))
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

fun lowercaseDois(list: MutableList<S2Interface.PaperDetails>) {
    list.forEach {
        val extIds = it.externalIds?.toMutableMap()
        if (extIds != null) {
            val doi = extIds["DOI"]
            if (doi != null) {
                val lowerDoi = doi.lowercase()
                if (doi != lowerDoi) {
                    extIds["DOI"] = lowerDoi
                    it.externalIds = extIds
                }
            }
        }
    }
}