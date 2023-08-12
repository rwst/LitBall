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

private const val ACCEPTED_NAME = "accepted.txt"
private const val REJECTED_NAME = "rejected.txt"
private const val EXPANDED_NAME = "expanded.txt"
private const val FILTERED_NAME = "filtered.txt"
private const val SETTINGS_NAME = "settings.json"

@Serializable
object QueryList {
    var list: MutableList<LitBallQuery> = mutableListOf()

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
                    acceptedSet = getDOIs(it, ACCEPTED_NAME),
                    rejectedSet = getDOIs(it, REJECTED_NAME),
                )
            }
        }
        runBlocking {
            delay(200)
            RootStore.refreshList()
        }
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
            File("${queryDir.absolutePath}/$ACCEPTED_NAME").writeText(dois.joinToString("\n"))
        } catch (e: Exception) {
            handleException(e)
            return
        }
        val maxId: Int = list.maxOf { it.id }
        list.add(
            LitBallQuery(
                id = maxId + 1,
                name = name,
                acceptedSet = dois.toMutableSet()
            )
        )
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
    val acceptedSet: MutableSet<String> = mutableSetOf(),
    val rejectedSet: MutableSet<String> = mutableSetOf(),
) {
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
        acceptedSet.chunked(450).forEach {
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
            delay(1000)
        }
        val newDoiSet = doiSet.minus(acceptedSet)
        Logger.i(tag, "${newDoiSet.size} new refs received. Writing to expanded...")
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canWrite()) {
            val text = newDoiSet.joinToString("\n").uppercase()
            status = try {
                File("${queryDir.absolutePath}/$EXPANDED_NAME").writeText(text)
                QueryStatus.EXPANDED
            } catch (e: Exception) {
                handleException(e)
                QueryStatus.ANNOTATED
            }
        }
    }

    suspend fun filter() {
        val mandatoryKeyWordRegexes = setting?.mandatoryKeyWords?.map { "\\b${Regex.escape(it)}\\b".toRegex(RegexOption.IGNORE_CASE) }?: emptyList()
        val forbiddenKeyWordRegexes = setting?.forbiddenKeyWords?.map { "\\b${Regex.escape(it)}\\b".toRegex(RegexOption.IGNORE_CASE) }?: emptyList()
        println(mandatoryKeyWordRegexes.toString())
        println(forbiddenKeyWordRegexes.toString())
//        exitProcess(0)
        val tag = "FILTER"
        val queryDir = getQueryDir(name)
        val paperList = mutableListOf<S2Service.PaperDetailsWithAbstract>()
        val rejectedDOIs: Set<String>
        if (queryDir.isDirectory && queryDir.canRead()) {
            val doiSet = getDOIs(queryDir, EXPANDED_NAME)
            doiSet.chunked(450).forEach {
                val papers: List<S2Service.PaperDetailsWithAbstract?> = try {
                    S2client.getBulkPaperDetailsWithAbstract(it)
                } catch (e: Exception) {
                    handleException(e)
                    null
                } ?: return
                Logger.i(tag, "Received ${papers.size} records")
                paperList.addAll(papers.filterNotNull().filter { paper ->
                    val textsOfPaper: Set<String> = setOf(
                            paper.title ?: "",
                            paper.tldr?.get("text") ?: "",
                            paper.abstract ?: ""
                        )
                    mandatoryKeyWordRegexes.any { regex ->
                        textsOfPaper.any { text -> regex.containsMatchIn(text) }
                    } && forbiddenKeyWordRegexes.none { regex ->
                        regex.containsMatchIn(paper.title?: "")
                    }
                })
                Logger.i(tag, "Retained ${paperList.size} records")
                delay(1000)
            }
            val filteredDOIs = paperList.mapNotNull { it.externalIds?.get("DOI") }
            rejectedDOIs = doiSet.minus(filteredDOIs.toSet())
        }
        else {
            handleException(IOException("Cannot access directory ${queryDir.absolutePath}"))
            return
        }
        sanitize(paperList)
        Logger.i(tag, "rejected ${rejectedDOIs.size} papers, write to rejected...")
        val json = ConfiguredJson.get()
        if (queryDir.isDirectory && queryDir.canWrite()) {
            try {
                val file = File("${queryDir.absolutePath}/$FILTERED_NAME")
                file.writeText(json.encodeToString(paperList))
            } catch (e: Exception) {
                handleException(e)
                return
            }
            val text = rejectedDOIs.joinToString("\n").uppercase()
            try {
                File("${queryDir.absolutePath}/$REJECTED_NAME").appendText(text)
            } catch (e: Exception) {
                handleException(e)
                return
            }
        }
        status = QueryStatus.FILTERED
    }

    fun annotate() {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canRead()) {
            try {
                val file = File("${queryDir.absolutePath}/$FILTERED_NAME")
            } catch (e: Exception) {
                handleException(e)
                return
            }
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
                File("${queryDir.absolutePath}/$SETTINGS_NAME").writeText(text)
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
    if (setOf(ACCEPTED_NAME, SETTINGS_NAME).all { it in fileNames }) {
        if (FILTERED_NAME in fileNames)
            return QueryStatus.FILTERED
        if (EXPANDED_NAME in fileNames)
            return QueryStatus.EXPANDED
        return QueryStatus.ANNOTATED
    }
    return QueryStatus.UNINITIALIZED
}

private fun getSetting(dir: File): QuerySetting? {
    val filePath = dir.absolutePath + "/" + SETTINGS_NAME
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
