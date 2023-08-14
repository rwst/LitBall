package org.reactome.lit_ball.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.handleException
import java.io.File
import java.io.IOException
import java.nio.file.Files

enum class QueryStatus { UNINITIALIZED, ANNOTATED, EXPANDED, FILTERED }

enum class FileType(val fileName: String) {
    ACCEPTED("accepted.txt"),
    REJECTED("rejected.txt"),
    EXPANDED("expanded.txt"),
    FILTERED("filtered.txt"),
    SETTINGS("settings.json");
}

const val CHUNK_SIZE = 20
const val QUERY_DELAY = 5000L

@Serializable
object QueryList {
    var list: List<LitBallQuery> = listOf()

    fun fill() {
        if (list.isNotEmpty()) return
        val queryPath = Settings.map["path-to-queries"] ?: ""
        val prefix = Settings.map["directory-prefix"] ?: ""
        val dirs = queryDirectories(queryPath, prefix)
        list = MutableList(dirs.size) { index ->
            dirs[index].let {
                LitBallQuery(
                    id = index,
                    name = it.name.removePrefix(prefix),
                    status = getStatus(it),
                    setting = getSetting(it),
                    acceptedSet = getDOIs(it, FileType.ACCEPTED.fileName).filter { doi -> doi.isNotBlank() }.toMutableSet(),
                    rejectedSet = getDOIs(it, FileType.REJECTED.fileName).filter { doi -> doi.isNotBlank() }.toMutableSet(),
                )
            }
        }
        RootStore.setItems(list)
    }

    fun itemFromId(id: Int?): LitBallQuery? = id?.let { list.find { id == it.id } }
    fun addNewItem(name: String, dois: Set<String>) {
        val queryDir = getQueryDir(name)
        if (queryDir.exists()) {
            handleException(IOException("Directory ${queryDir.absolutePath} already exists. Query not created."))
            return
        }
        try {
            Files.createDirectory(queryDir.toPath())
        } catch (e: Exception) {
            handleException(e)
            return
        }
        try {
            File("${queryDir.absolutePath}/${FileType.ACCEPTED.fileName}").writeText(dois.joinToString("\n") + "\n")
        } catch (e: Exception) {
            handleException(e)
            return
        }
        val maxId: Int = list.maxOf { it.id }
        list = list.plus(
            LitBallQuery(
                id = maxId + 1,
                name = name,
                acceptedSet = dois.toMutableSet()
            )
        )
        RootStore.setItems(list)
    }
}

private fun getQueryDir(name: String): File {
    val queryPath = Settings.map["path-to-queries"] ?: ""
    val prefix = Settings.map["directory-prefix"] ?: ""
    val directory = File(queryPath)
    if (!directory.isDirectory || !directory.exists()) {
        throw IllegalArgumentException("Invalid directory path: $queryPath")
    }
    return File("$queryPath/$prefix$name")
}

data class LitBallQuery(
    val id: Int,
    val name: String = "",
    var status: QueryStatus = QueryStatus.UNINITIALIZED,
    var setting: QuerySetting? = null,
    var acceptedSet: MutableSet<String> = mutableSetOf(),
    var rejectedSet: MutableSet<String> = mutableSetOf(),
) {
    fun syncBuffers() {
        acceptedSet = getDOIs(getQueryDir(name), FileType.ACCEPTED.fileName).filter { it.isNotBlank() }.toMutableSet()
        rejectedSet = getDOIs(getQueryDir(name), FileType.REJECTED.fileName).filter { it.isNotBlank() }.toMutableSet()
    }
    fun nrAccepted() = acceptedSet.size
    fun nrRejected() = rejectedSet.size
    override fun toString(): String {
        return "Query(id=$id, name=$name, status=$status, setting=$setting, nrAccepted=${nrAccepted()}, nrRejected=${nrRejected()})"
    }

    fun nextActionText(): String {
        return arrayOf(
            "Complete the Setting",
            "Start expansion",
            "Start filtering",
            "Go to Annotation"
        )[status.ordinal]
    }

    suspend fun expand() {
        val tag = "EXPAND"
        val doiSet = mutableSetOf<String>()
        acceptedSet.chunked(CHUNK_SIZE).forEach {
            val refs: List<S2Service.PaperRefs?> = try {
                S2client.getRefs(it)
            } catch (e: Exception) {
                handleException(e)
                null
            } ?: return
            Logger.i(tag, "Received ${refs.size} records")
            refs.forEach { paperRef ->
                doiSet.addAll(paperRef?.citations?.mapNotNull { cit -> cit.externalIds?.get("DOI") } ?: emptyList())
                doiSet.addAll(paperRef?.references?.mapNotNull { cit -> cit.externalIds?.get("DOI") } ?: emptyList())
            }
            delay(QUERY_DELAY)
        }
        val newDoiSet = doiSet.minus(acceptedSet)
        Logger.i(tag, "${newDoiSet.size} new refs received. Writing to expanded...")
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canWrite()) {
            val text = newDoiSet.joinToString("\n").uppercase()
            status = try {
                File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").writeText(text)
                QueryStatus.EXPANDED
            } catch (e: Exception) {
                handleException(e)
                QueryStatus.ANNOTATED
            }
            RootStore.onDoExpandStopped()
        }
    }

    suspend fun filter() {
        val mandatoryKeyWordRegexes = setting?.mandatoryKeyWords?.filter { it.isNotEmpty() }?.map { "\\b${Regex.escape(it)}\\b".toRegex(RegexOption.IGNORE_CASE) }
            ?: emptyList()
        val forbiddenKeyWordRegexes = setting?.forbiddenKeyWords?.filter { it.isNotEmpty() }?.map { "\\b${Regex.escape(it)}\\b".toRegex(RegexOption.IGNORE_CASE) }
            ?: emptyList()
        val tag = "FILTER"
        val queryDir = getQueryDir(name)
        val paperDetailsList = mutableListOf<S2Service.PaperDetailsWithAbstract>()
        val rejectedDOIs: Set<String>
        if (queryDir.isDirectory && queryDir.canRead()) {
            val doiSet = getDOIs(queryDir, FileType.EXPANDED.fileName)
            doiSet.chunked(CHUNK_SIZE).forEach {
                val papers: List<S2Service.PaperDetailsWithAbstract?> = try {
                    S2client.getBulkPaperDetailsWithAbstract(it)
                } catch (e: Exception) {
                    handleException(e)
                    null
                } ?: return
                Logger.i(tag, "Received ${papers.size} records")
                paperDetailsList.addAll(papers.filterNotNull().filter { paper ->
                    val textsOfPaper: Set<String> = setOf(
                            paper.title ?: "",
                            paper.tldr?.get("text") ?: "",
                            paper.abstract ?: ""
                        )
                    val bool = mandatoryKeyWordRegexes.any { regex1 ->
                        textsOfPaper.any { text ->
                            regex1.containsMatchIn(text) }
                    } && forbiddenKeyWordRegexes.none { regex2 ->
                        regex2.containsMatchIn(paper.title?: "")
                    }
                    bool
                })
                Logger.i(tag, "Retained ${paperDetailsList.size} records")
                delay(QUERY_DELAY)
            }
            val filteredDOIs = paperDetailsList.mapNotNull { it.externalIds?.get("DOI")?.uppercase() }
            rejectedDOIs = doiSet.minus(filteredDOIs.toSet())
            rejectedSet.addAll(rejectedDOIs)
        }
        else {
            handleException(IOException("Cannot access directory ${queryDir.absolutePath}"))
            return
        }
        sanitize(paperDetailsList)
        Logger.i(tag, "rejected ${rejectedDOIs.size} papers, write to rejected...")
        val json = ConfiguredJson.get()
        if (queryDir.isDirectory && queryDir.canWrite()) {
            try {
                val file = File("${queryDir.absolutePath}/${FileType.FILTERED.fileName}")
                file.writeText(json.encodeToString(
                    paperDetailsList.mapIndexed { idx, pd -> Paper(idx, pd) })
                )
            } catch (e: Exception) {
                handleException(e)
                return
            }
            val text = rejectedDOIs.joinToString("\n") + "\n"
            try {
                File("${queryDir.absolutePath}/${FileType.REJECTED.fileName}").appendText(text)
            } catch (e: Exception) {
                handleException(e)
                return
            }
        }
        File("${queryDir.absolutePath}/${FileType.EXPANDED.fileName}").delete()
        status = QueryStatus.FILTERED
        RootStore.onDoFilterStopped()
    }

    fun annotate() {
        if (PaperList.list.isNotEmpty()) return
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canRead()) {
            val file: File
            try {
                file = File("${queryDir.absolutePath}/${FileType.FILTERED.fileName}")
            } catch (e: Exception) {
                handleException(e)
                return
            }
            PaperList.readFromFile(file)
            runBlocking {
                delay(200)
                AnnotatingRootStore.refreshList()
            }
        }
        else {
            handleException(IOException("Cannot access directory ${queryDir.absolutePath}"))
            return
        }
    }

    fun saveSettings() {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canWrite()) {
            val json = ConfiguredJson.get()
            val text = json.encodeToString<QuerySetting>(setting ?: QuerySetting())
            println(setting.toString())
            println(text)
            try {
                File("${queryDir.absolutePath}/${FileType.SETTINGS.fileName}").writeText(text)
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
}

private fun queryDirectories(directoryPath: String, prefix: String): List<File> {
    val directory = File(directoryPath)
    if (!directory.isDirectory || !directory.exists()) {
        throw IllegalArgumentException("Invalid directory path: $directoryPath")
    }
    val directories = directory.listFiles { file ->
        file.isDirectory && file.canRead() && file.name.startsWith(prefix)
    }
    return directories?.toList() ?: emptyList()
}

private fun getStatus(dir: File): QueryStatus {
    val fileNames = dir.listFiles { file ->
        file.isFile && file.canRead()
    }?.map { it.name } ?: emptyList()
    if (setOf(FileType.ACCEPTED.fileName, FileType.SETTINGS.fileName).all { it in fileNames }) {
        if (FileType.FILTERED.fileName in fileNames)
            return QueryStatus.FILTERED
        if (FileType.EXPANDED.fileName in fileNames)
            return QueryStatus.EXPANDED
        return QueryStatus.ANNOTATED
    }
    return QueryStatus.UNINITIALIZED
}

private fun getSetting(dir: File): QuerySetting? {
    val filePath = dir.absolutePath + "/" + FileType.SETTINGS.fileName
    val settingsFile = File(filePath)
    if (settingsFile.exists() && settingsFile.isFile && settingsFile.canRead())
        return QuerySetting.fromFile(settingsFile)
    return null
}

private fun getDOIs(dir: File, fileName: String): MutableSet<String> {
    val filePath = dir.absolutePath + "/" + fileName
    val doiFile = File(filePath)
    if (doiFile.exists() && doiFile.isFile && doiFile.canRead()) {
        return doiFile.readLines().map { it.uppercase() }.toMutableSet()
    }
    return mutableSetOf()
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
