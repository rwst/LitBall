package org.reactome.lit_ball.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.handleException
import java.io.File
import java.io.IOException

enum class QueryStatus { UNINITIALIZED, ANNOTATED, EXPANDED, FILTERED }

enum class FileType(val fileName: String) {
    ACCEPTED("accepted.txt"),
    REJECTED("rejected.txt"),
    EXPANDED("expanded.txt"),
    FILTERED("filtered.txt"),
    ARCHIVED("archived.txt"),
    SETTINGS("settings.json");
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
        val result = S2Client.getRefs(acceptedSet.toList()) {
            doiSet.addAll(it.citations?.mapNotNull { cit -> cit.externalIds?.get("DOI")?.uppercase() } ?: emptyList())
            doiSet.addAll(it.references?.mapNotNull { cit -> cit.externalIds?.get("DOI")?.uppercase() } ?: emptyList())
            }
        if (!result) return
        Logger.i(tag, "Received ${doiSet.size} DOIs")
        val newDoiSet = doiSet.minus(acceptedSet).minus(rejectedSet)
        Logger.i(tag, "${newDoiSet.size} new DOIs received. Writing to expanded...")
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canWrite()) {
            val text = newDoiSet.joinToString("\n").uppercase() + "\n"
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
            val doiSet = getDOIs(queryDir, FileType.EXPANDED.fileName).toList()
            S2Client.getPaperDetailsWithAbstract(doiSet) {
                val textsOfPaper: Set<String> = setOf(
                    it.title ?: "",
                    it.tldr?.get("text") ?: "",
                    it.abstract ?: ""
                )
                if (mandatoryKeyWordRegexes.any { regex1 ->
                    textsOfPaper.any { text ->
                        regex1.containsMatchIn(text) }
                } && forbiddenKeyWordRegexes.none { regex2 ->
                    regex2.containsMatchIn(it.title?: "")
                })
                    paperDetailsList.add(it)
            }
            Logger.i(tag, "Retained ${paperDetailsList.size} records")
            val filteredDOIs = paperDetailsList.mapNotNull { it.externalIds?.get("DOI")?.uppercase() }
            rejectedDOIs = doiSet.toSet().minus(filteredDOIs.toSet())
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
                mergeIntoArchive(paperDetailsList)
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
            file.writeText(json.encodeToString(
                details.mapIndexed { idx, pd -> Paper(idx, pd) })
            )
        }
    }

    fun annotate() {
        if (PaperList.query == this) return
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canRead()) {
            val file: File
            try {
                file = File("${queryDir.absolutePath}/${FileType.FILTERED.fileName}")
            } catch (e: Exception) {
                handleException(e)
                return
            }
            PaperList.setFromQuery(this, file)
            AnnotatingRootStore.refreshList()
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
