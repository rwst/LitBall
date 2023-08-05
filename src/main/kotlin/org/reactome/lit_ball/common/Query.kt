package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import java.io.File

enum class QueryStatus { UNINITIALIZED, ANNOTATED, EXPANDED, FILTERED }

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
                )
            }
        }
        print(list)
    }
}

@Serializable
data class Query(
    val id: Int,
    val name: String = "",
    var status: QueryStatus = QueryStatus.UNINITIALIZED,
    val setting: QuerySetting? = null,
) {
    override fun toString(): String {
        return "Query(id=$id, name=$name, status=$status, setting=$setting)"
    }
    fun nextActionText(): String {
        return arrayOf(
            "Complete the Setting",
            "Start expansion",
            "Start filtering",
            "Go to Annotation")[status.ordinal]
    }
    fun nextAction(): () -> Unit {
        return {}
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
    if (setOf("accepted", "settings.json").all { it in fileNames }) {
        if ("expanded" in fileNames)
            return QueryStatus.EXPANDED
        if ("filtered" in fileNames)
            return QueryStatus.FILTERED
        return QueryStatus.ANNOTATED
    }
    return QueryStatus.UNINITIALIZED
}

private fun getSetting(dir: File): QuerySetting? {
    val filePath = dir.name + "/settings.json"
    val settingsFile = File(filePath)
    if (settingsFile.exists() && settingsFile.isFile && settingsFile.canRead())
        return QuerySetting.fromFile(settingsFile)
    return null
}