package org.reactome.lit_ball.common

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
private const val SETTINGS_NAME = "settings.json"
@Serializable
object QueryList
{
    var list: MutableList<Query> = mutableListOf()
    fun updateItem(id: Int, transformer: (Query) -> Query): QueryList {
        list.replaceFirst(transformer) { it.id == id }
        return this
    }

    fun fill() {
        if (list.isNotEmpty()) return
        val queryPath = Settings.map["path-to-queries"] ?: ""
        val prefix = Settings.map["directory-prefix"] ?: ""
        val dirs = queryDirectories(queryPath, prefix)
        list = MutableList(dirs.size) { index ->
            dirs[index].let {
                Query(
                    id = index,
                    name = it.name.removePrefix(prefix),
                    status = getStatus(it),
                    setting = getSetting(it),
                    acceptedSet = getDOIs(it, ACCEPTED_NAME),
                    rejectedSet = getDOIs(it, REJECTED_NAME),
                )
            }
        }
        print(list)
    }
    fun itemFromId (id: Int?): Query? = id?.let { list.find { id == it.id } }
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
        list.add(Query(
            id = maxId + 1,
            name = name,
            acceptedSet = dois.toMutableSet()
        ))
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

data class Query(
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
            "Go to Annotation")[status.ordinal]
    }

    fun saveSettings() {
        val queryDir = getQueryDir(name)
        if (queryDir.isDirectory && queryDir.canWrite()) {
            val json = ConfiguredJson.get()
            val text = json.encodeToString<QuerySetting>(setting?: QuerySetting())
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
        if ("expanded" in fileNames)
            return QueryStatus.EXPANDED
        if ("filtered" in fileNames)
            return QueryStatus.FILTERED
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