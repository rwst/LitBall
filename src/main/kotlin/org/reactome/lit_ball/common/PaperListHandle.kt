package common

import androidx.compose.runtime.mutableStateListOf
import util.CantHappenException
import util.UniqueIdGenerator
import window.components.SortingType

/**
 * Paper list handle behaves as full paper list, unless filtered list is set.
 * It also encapsuloates low level list handling to relieve class PaperList.
 */
class PaperListHandle {
    private val fullList = mutableStateListOf<Paper>()
    private val filteredList = mutableStateListOf<Paper>()

    fun applyFilter(string: String) {
        filteredList.clear()
        if (string.isEmpty()) return
        filteredList.addAll(fullList.filter {
            it.details.title?.contains(string) ?: false
                    || it.details.tldr?.get("text")?.contains(string) ?: false
                    || it.details.abstract?.contains(string) ?: false
            } )
    }

    fun getFullList(): List<Paper> {
        return fullList
    }

    fun getFilteredList(): List<Paper> {
        return filteredList
    }

    fun setFullList(list: List<Paper>) {
        fullList.clear()
        fullList.addAll(list)
        filteredList.clear()
    }

    private fun updateItemInBothLists(id: Long, transformer: (Paper) -> Paper) {
        val idx = fullList.indexOfFirst { it.uniqueId == id }
        if (idx == -1) return
        val old = fullList[idx]
        val new = transformer(old).also { it.uniqueId = UniqueIdGenerator.nextId() }
        if (old == new) return
        fullList[idx] = new
        if (filteredList.isEmpty()) return
        val index = filteredList.indexOfFirst { it.uniqueId == id }
        if (index == -1) return
        filteredList[index] = new
    }

    fun delete(id: Long) {
        fullList.removeIf { p -> p.uniqueId == id }
        filteredList.removeIf { p -> p.uniqueId == id }
    }

    fun deleteAllFiltered() {
        val uniqueIds = filteredList.map { it.uniqueId }.toSet()
        fullList.removeIf { uniqueIds.contains(it.uniqueId) }
    }

    /**
     * Set all tags in the full list to the [tag].
     *
     * @param tag
     */
    fun setFullAllTags(tag: Tag) {
        fullList.replaceAll { paper -> paper.copy(tag) }
    }

    /**
     * Updates the tag of all papers in the filtered list to the specified tag.
     * Preserves other paper properties while updating the tag.
     *
     * @param newTag The tag to be applied to all papers in the filtered list
     */
    fun setFilteredAllTags(newTag: Tag) {
        filteredList.forEachIndexed { index, paper ->
            if (paper.tag != newTag)
                filteredList[index] = paper.copy(newTag)
            }
    }

    fun setFullTagsFromFiltered() {
        val tagMap: Map<String, Tag> = filteredList.associate { Pair(it.paperId ?: "", it.tag) }
        setFullTagsFromPaperIdMap(tagMap)
    }

    fun setFullTagsFromPaperIdMap(tagMap: Map<String, Tag>) {
        fullList.forEachIndexed { index, paper ->
            val newTag = tagMap[paper.paperId] ?: Tag.Accepted
            if (paper.tag != newTag) {
                fullList[index] = paper.copy(newTag)
            }
        }
    }

    fun setTag(id: Long, tag: Tag) {
        updateItemInBothLists(id) {
            if (it.tag == tag)
                it
            else
                it.copy(tag)
        }
    }

    fun setFlag(id: Long, flagNo: Int, value: Boolean) {
        val flag = PaperList.flagList[flagNo]
        updateItemInBothLists(id) {
            if (!value)
                flag.let { it1 -> it.flags.add(it1) }
            else
                it.flags.remove(flag)
            it
        }
    }

    fun sort(type: SortingType) {
        sortInPlace(fullList, type)
        sortInPlace(filteredList, type)
    }

    fun getPaperFromId(id: Long): Paper? {
        return fullList.find { it.uniqueId == id }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaperListHandle

        if (fullList != other.fullList) return false
        if (filteredList != other.filteredList) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fullList.hashCode()
        result = 31 * result + filteredList.hashCode()
        return result
    }
}

fun sortInPlace(list: MutableList<Paper>, type: SortingType) {
    when (type) {
        SortingType.ALPHA_ASCENDING -> list.sortBy { it.details.title?.uppercase() }
        SortingType.ALPHA_DESCENDING -> list.sortByDescending { it.details.title?.uppercase() }
        SortingType.DATE_ASCENDING -> list.sortBy { it.details.publicationDate }
        SortingType.DATE_DESCENDING -> list.sortByDescending { it.details.publicationDate }
        else ->
            throw CantHappenException()
    }
}

fun sort(list: List<Paper>, type: SortingType): List<Paper> {
    return when (type) {
        SortingType.ALPHA_ASCENDING -> list.sortedBy { it.details.title?.uppercase() }
        SortingType.ALPHA_DESCENDING -> list.sortedByDescending { it.details.title?.uppercase() }
        SortingType.DATE_ASCENDING -> list.sortedBy { it.details.publicationDate }
        SortingType.DATE_DESCENDING -> list.sortedByDescending { it.details.publicationDate }
        else ->
            throw Exception("can't happen: $type")
    }
}