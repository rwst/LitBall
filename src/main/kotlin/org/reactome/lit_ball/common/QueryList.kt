package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.util.handleException
import org.reactome.lit_ball.window.components.SortingType
import java.io.File
import java.io.IOException
import java.nio.file.Files

@Serializable
object QueryList {
    var list: List<LitBallQuery> = listOf()

    fun fill() {
        list = listOf()
        val queryPath = Settings.map["path-to-queries"] ?: ""
        val prefix = Settings.map["directory-prefix"] ?: ""
        val dirs = queryDirectories(queryPath, prefix)
        list = MutableList(dirs.size) { index ->
            dirs[index].let {
                val newQuery = LitBallQuery(
                    id = index,
                    name = it.name.removePrefix(prefix),
                    status = getStatus(it),
                    setting = getSetting(it),
                    acceptedSet = getDOIs(it, FileType.ACCEPTED.fileName).filter { doi -> doi.isNotBlank() }
                        .toMutableSet(),
                    rejectedSet = getDOIs(it, FileType.REJECTED.fileName).filter { doi -> doi.isNotBlank() }
                        .toMutableSet(),
                )
                newQuery.lastExpansionDate = newQuery.getFileDate(
                    fromFile = true,
                    fileType = FileType.ACCEPTED
                )
                newQuery.noNewAccepted = newQuery.readNoNewAccepted()
                newQuery
            }
        }
        sort(SortingType.valueOf(Settings.map["query-sort-type"]?: SortingType.ALPHA_ASCENDING.toString()))
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
        val maxId = if (list.isNotEmpty()) list.maxOf { it.id } else 0
        list = list.plus(
            LitBallQuery(
                id = maxId + 1,
                name = name,
                acceptedSet = dois.toMutableSet()
            )
        )
        RootStore.setItems(list)
    }

    fun sort(type: SortingType) {
        list = when (type) {
            SortingType.ALPHA_ASCENDING -> list.sortedBy { it.name }
            SortingType.ALPHA_DESCENDING -> list.sortedByDescending { it.name }
            SortingType.NUMER_ASCENDING -> list.sortedBy { it.lastExpansionDate }
            SortingType.NUMER_DESCENDING -> list.sortedByDescending { it.lastExpansionDate }
        }
        Settings.map["query-sort-type"] = type.toString()
        Settings.save()
    }
}

private fun queryDirectories(directoryPath: String, prefix: String): List<File> {
    val directory = File(directoryPath)
    if (!directory.exists()) {
        if (!directory.mkdir())
            throw IllegalArgumentException("Directory could not be created: $directoryPath")
    }
    if (!directory.isDirectory) {
        throw IllegalArgumentException("Not a directory: $directoryPath")
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
        if (FileType.FILTERED1.fileName in fileNames)
            return QueryStatus.FILTERED1
        if (FileType.EXPANDED.fileName in fileNames)
            return QueryStatus.EXPANDED
        return QueryStatus.FILTERED2
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
