package common

import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import util.UniqueIdGenerator
import util.checkFileInDirectory
import util.handleException
import util.makeQueryDir
import util.writeFile
import window.components.SortingType
import java.io.File

const val DAY_IN_MS = 1000L * 60 * 60 * 24

@Serializable
object QueryList {
    var list: List<LitBallQuery> = listOf()

    suspend fun fill() {
        list = listOf()
        val queryPath = Settings.map["path-to-queries"] ?: ""
        val prefix = Settings.map["directory-prefix"] ?: ""
        val dirs = queryDirectories(queryPath, prefix)
        list = MutableList(dirs.size) { index ->
            dirs[index].let {
                val newQuery = LitBallQuery(
                    id = UniqueIdGenerator.nextId(),
                    name = it.name.removePrefix(prefix),
                    status = mutableStateOf(getStatus(it)),
                    acceptedSet = getDOIs(it, FileType.ACCEPTED).filter { doi -> doi.isNotBlank() }
                        .toMutableSet(),
                    rejectedSet = getDOIs(it, FileType.REJECTED).filter { doi -> doi.isNotBlank() }
                        .toMutableSet(),
                )
                newQuery.setting = getSetting(it)
                newQuery.lastExpansionDate = newQuery.getFileDate(fromFile = true, FileType.ACCEPTED)
                newQuery.noNewAccepted = newQuery.readNoNewAccepted()
                if (newQuery.noNewAccepted) {
                    val now = System.currentTimeMillis()
                    val cacheMillis = DAY_IN_MS * (Settings.map["cache-max-age-days"] ?: "30").toInt()
                    if (now - (newQuery.lastExpansionDate?.time ?: 0) > cacheMillis) {
                        newQuery.noNewAccepted = false
                    }
                }
                newQuery.type = newQuery.setting.type
                newQuery.expSearchParams =
                    Pair(newQuery.setting.pubDate, typeStringsToBoolArray(newQuery.setting.pubType))
                newQuery
            }
        }
    }

    fun itemFromId(id: Int?): LitBallQuery? = id?.let { list.find { id == it.id } }

    suspend fun addNewItem(
        type: QueryType,
        name: String,
        dois: Set<String>,
        expSearchParams: Pair<String, BooleanArray>,
    ) {
        val cleanedDois = dois.filter { doi -> doi.isNotBlank() }.toMutableSet()
        val queryDir = getQueryDir(name)
        if (!makeQueryDir(queryDir))
            return
        if (!writeFile(queryDir, FileType.ACCEPTED, cleanedDois.joinToString("\n") + "\n"))
            return
        val newQuery = LitBallQuery(
            id = UniqueIdGenerator.nextId(),
            type = type,
            name = name,
            acceptedSet = cleanedDois,
            expSearchParams = expSearchParams,
        )
        if (type == QueryType.SIMILARITY_SEARCH) {
            newQuery.status.value = QueryStatus.FILTERED2
            newQuery.saveSettings()
        }
        list = list.plus(newQuery)
    }

    fun touchItem(id: Int?): List<LitBallQuery>? {
        id?.let {
            list = list.map {
                if (it.id == id) {
                    it.id = UniqueIdGenerator.nextId()
                }
                it
            }
            return list
        }
        return null
    }

    fun removeDir(id: Int?) {
        val name = itemFromId(id)?.name
        val queryDir = name?.let { getQueryDir(it) }
        if (queryDir != null) {
            if (queryDir.exists()) {
                try {
                    queryDir.deleteRecursively()
                } catch (e: Exception) {
                    handleException(e)
                }
            }
        }
    }

    fun sort(type: SortingType) {
        list = when (type) {
            SortingType.ALPHA_ASCENDING -> list.sortedBy { it.name }
            SortingType.ALPHA_DESCENDING -> list.sortedByDescending { it.name }
            SortingType.NUMER_ASCENDING -> list.sortedBy { it.lastExpansionDate }
            SortingType.NUMER_DESCENDING -> list.sortedByDescending { it.lastExpansionDate }
            else ->
                throw Exception("can't happen: $type")
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

private suspend fun getSetting(dir: File): QuerySetting {
    return checkFileInDirectory(dir, FileType.SETTINGS,
        QuerySetting::fromFile
    ).getOrElse { QuerySetting() }
}
