package common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import common.PaperList.listHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import model.AnnotatingRootStore
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
    var allLinkedDoiSize: Int = 0,
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

    suspend fun snowBall(auto: Boolean = false): Triple<Int, Int, Boolean> {
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
            if (!status) return Triple(0, 0, false)
        }
        Logger.i(tag, "New snowball size: ${allLinkedDois.size}")
        allLinkedDoiSize = allLinkedDois.size

        val newDoiSet = allLinkedDois.minus(acceptedSet).minus(rejectedSet)
        if (auto && newDoiSet.size > EXPLODED_LIMIT) {
            status.value = QueryStatus.EXPLODED
            return Triple(newDoiSet.size, newDoiSet.size, false)
        }
        Logger.i(tag, "${newDoiSet.size} new DOIs. Writing to expanded...")

        if (newDoiSet.isEmpty() && nrMissing != 0 && allNullsMissing) {
            status.value = QueryStatus.FILTERED2
            return Triple(0, 0, true)
        }
        writeExpandedFile(newDoiSet)
        return Triple(newDoiSet.size, nrMissing, false)
    }

    suspend fun filter1(): Pair<Int, Int> {
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

            if (!fetchDetails(paperDetailsList, rejectedDOIs)) return Pair(0, 0)

            Logger.i(tag, "rejected ${rejectedDOIs.size} papers, write to rejected...")

            if (!writeFiltered(paperDetailsList)) return Pair(0, 0)
            if (!writeRejected(rejectedDOIs)) return Pair(0,0)
            File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").delete()
            status.value = QueryStatus.FILTERED1
            Pair(paperDetailsList.size, rejectedDOIs.size)
        }
    }

    suspend fun expressionSearch(): Int {
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
        if (!result) return -1
        Logger.i(tag, "Retained ${paperDetailsList.size} records")
        lowercaseDois(paperDetailsList)
        sanitize(paperDetailsList)

        acceptedSet = idSetFromPaperDetailsList(paperDetailsList)
        if (!writeFile(queryDir, FileType.ACCEPTED,
            acceptedSet.joinToString("\n")
            ))
            return 0
        ArchivedCache.init(queryDir)
        ArchivedCache.merge(paperDetailsList)
        noNewAccepted = true
        writeNoNewAccepted()
        status.value = QueryStatus.FILTERED2
        return paperDetailsList.size
    }

    // Similarity Search will add 20 new papers. User deletes as much as wanted. Following clicks on Search will
    // add the same amount of what remains accepted, but at least 20.
    suspend fun similaritySearch(): Int {
        val queryDir = getQueryDir(name)
        val paperDetailsList = mutableListOf<S2Interface.PaperDetails>()
        val ids = acceptedSet.toMutableList()
        val result = agService.getSimilarDetails(ids) {
            paperDetailsList.add(it)
        }
        // Bail out on Cancel
        if (!result) return -2
        Logger.i(tag, "Retained ${paperDetailsList.size} records")
        lowercaseDois(paperDetailsList)
        sanitize(paperDetailsList)

        acceptedSet = ids.plus(idSetFromPaperDetailsList(paperDetailsList)).toMutableSet()
        if (!writeFile(queryDir, FileType.ACCEPTED,
            acceptedSet.joinToString("\n")))
            return -1
        ArchivedCache.init(queryDir)
        ArchivedCache.set(paperDetailsList)
        status.value = QueryStatus.FILTERED2
        return paperDetailsList.size
    }

    suspend fun filter2() {
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.FILTERED1)?.let { file ->
            PaperList.setFromQuery(this, file)
        }
    }

    private suspend fun acceptAll() {
        // TODO: why read it again from file?
        val queryDir = getQueryDir(name)
        checkFileInDirectory(queryDir, FileType.FILTERED1)?.let { file ->
            PaperList.setFromQuery(this, file)
            listHandle.setFullAllTags(Tag.Accepted)
            PaperList.finish()
            syncBuffers()
        }
    }

    suspend fun autoSnowBall(): Int {
        while (true) {
            snowBall(true)
            if (status.value == QueryStatus.FILTERED2 || status.value == QueryStatus.EXPLODED) break
            mutex.unlock()
            filter1()
            mutex.tryLock()
            if (status.value == QueryStatus.FILTERED2) break
            acceptAll()
        }
        if (status.value == QueryStatus.FILTERED2) {
            val noAcc = listHandle.getFullList().count { it.tag == Tag.Accepted }
            QueryList.itemFromId(id)?.let {
                it.noNewAccepted = (noAcc == 0)
                it.writeNoNewAccepted()
            }
            return noAcc
        }
        if (status.value == QueryStatus.EXPLODED) {
            status.value = QueryStatus.FILTERED2
        }
        return -1
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