package common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import util.UniqueIdGenerator
import util.checkFileInDirectory
import util.handleException
import util.makeQueryDir
import util.writeFile
import window.components.SortingType
import java.io.File

/**
 * A constant representing the number of milliseconds in a day.
 */
const val DAY_IN_MS = 1000L * 60 * 60 * 24

/**
 * A serializable singleton object that manages the list of [LitBallQuery] items.
 * It is responsible for loading, creating, deleting, and sorting queries,
 * as well as interfacing with the file system to persist query data.
 */
@Serializable
object QueryList {
    // Internal mutable state flow to hold the list of queries.
    private val _list = MutableStateFlow<List<LitBallQuery>>(emptyList())
    /**
     * The public, read-only state flow of the query list, suitable for collection by UI components.
     */
    val list = _list.asStateFlow()
    var lastAnnotatedQName: String? by mutableStateOf(null)

    /**
     * Populates the query list by scanning the query directory specified in the settings.
     * It reads each query's data from its subdirectory and constructs [LitBallQuery] objects.
     */
    suspend fun fill() {
        val queryPath = Settings.map["path-to-queries"] ?: ""
        val prefix = Settings.map["directory-prefix"] ?: ""
        val dirs = queryDirectories(queryPath, prefix)
        val newList = mutableListOf<LitBallQuery>()
        dirs.forEach {
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
            // Invalidate `noNewAccepted` flag if cache is older than max age
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
            newList.add(newQuery)
        }
        _list.value = newList
    }

    /**
     * Retrieves a [LitBallQuery] from the list by its unique ID.
     * @param id The ID of the query to find.
     * @return The [LitBallQuery] if found, otherwise null.
     */
    fun itemFromId(id: Long?): LitBallQuery? = id?.let { _list.value.find { id == it.id } }

    fun  indexOfName(name: String): Int = _list.value.indexOfFirst { name == it.name }

    /**
     * Creates a new query, saves its initial data to the file system, and adds it to the list.
     * @param type The [QueryType] of the new query.
     * @param name The name for the new query.
     * @param dois The initial set of DOIs for the accepted list.
     * @param expSearchParams Parameters for expression-based searches.
     * @param mandatoryKeyWords A list of mandatory keywords for the query.
     * @param forbiddenKeyWords A list of forbidden keywords for the query.
     */
    suspend fun addNewItem(
        type: QueryType,
        name: String,
        dois: Set<String>,
        expSearchParams: Pair<String, BooleanArray>,
        mandatoryKeyWords: List<String> = emptyList(),
        forbiddenKeyWords: List<String> = emptyList(),
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
        newQuery.setting.mandatoryKeyWords = mandatoryKeyWords.toMutableSet()
        newQuery.setting.forbiddenKeyWords = forbiddenKeyWords.toMutableSet()
        if (type == QueryType.SIMILARITY_SEARCH) {
            newQuery.status.value = QueryStatus.FILTERED2
        }
        newQuery.saveSettings()
        _list.value = _list.value + newQuery
    }

    /**
     * "Touches" an item in the list to trigger a UI recomposition.
     * This is achieved by creating a copy of the item with a new unique ID and replacing
     * the original item in the list. This ensures that StateFlow collectors see a new list instance.
     * @param id The ID of the item to touch.
     */
    fun touchItem(id: Long?) {
        val index = _list.value.indexOfFirst { id == it.id }
        if (index < 0) return
        val newList = _list.value.toMutableList()
        val newItem = newList[index].copy()
        newItem.id = UniqueIdGenerator.nextId()
        newList[index] = newItem
        _list.value = newList
    }

    /**
     * Removes a query from the list and deletes its associated directory from the file system.
     * @param id The ID of the query to remove.
     */
    fun remove(id: Long) {
        val query = itemFromId(id)
        _list.value = _list.value.filterNot { it.id == id }
        query?.name?.let { removeDir(it) }
    }

    /**
     * Deletes the directory associated with a query name.
     * @param name The name of the query whose directory should be removed.
     */
    fun removeDir(name: String) {
        val queryDir = getQueryDir(name)
        if (queryDir.exists()) {
            try {
                queryDir.deleteRecursively()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    /**
     * Sorts the query list based on the specified [SortingType].
     * The chosen sort type is persisted in the application settings.
     * @param type The [SortingType] to apply.
     */
    fun sort(type: SortingType) {
        val newList = when (type) {
            SortingType.ALPHA_ASCENDING -> _list.value.sortedBy { it.name }
            SortingType.ALPHA_DESCENDING -> _list.value.sortedByDescending { it.name }
            SortingType.NUMER_ASCENDING -> _list.value.sortedBy { it.lastExpansionDate }
            SortingType.NUMER_DESCENDING -> _list.value.sortedByDescending { it.lastExpansionDate }
            // This case should not be reachable if all SortingType enum values are handled.
            else ->
                throw Exception("can't happen: $type")
        }
        Settings.map["query-sort-type"] = type.toString()
        Settings.save()
        _list.value = newList
    }
}

/**
 * Scans a given directory path for valid query directories.
 * A valid query directory is a readable directory whose name starts with the specified prefix.
 * @param directoryPath The path to the parent directory containing all query directories.
 * @param prefix The prefix that query directory names must have.
 * @return A list of [File] objects representing the valid query directories.
 * @throws IllegalArgumentException if the path does not exist or is not a directory.
 */
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

/**
 * Determines the [QueryStatus] of a query by inspecting the files in its directory.
 * The status reflects the latest completed step in the literature search workflow.
 * @param dir The query's directory as a [File] object.
 * @return The determined [QueryStatus].
 */
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

/**
 * Reads and deserializes the [QuerySetting] from the settings file within a query's directory.
 * @param dir The query's directory as a [File] object.
 * @return The deserialized [QuerySetting] object, or a default instance if the file doesn't exist or fails to parse.
 */
private suspend fun getSetting(dir: File): QuerySetting {
    return checkFileInDirectory(dir, FileType.SETTINGS,
        QuerySetting::fromFile
    ).getOrElse { QuerySetting() }
}