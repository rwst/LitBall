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
    private var filteredList: List<Paper>? = null
    private var filteredShadowMap: MutableMap<Long, Int>? = null

    fun applyFilter(string: String) {
        filteredList = if (string.isEmpty()) null
        else
            fullList.filter {
                it.details.title?.contains(string) ?: false
                        || it.details.tldr?.get("text")?.contains(string) ?: false
                        || it.details.abstract?.contains(string) ?: false
            }
        updateShadowMap()
    }

    fun getFullList(): List<Paper> {
        return fullList
    }

    fun getFilteredList(): List<Paper>? {
        return filteredList
    }

    @Suppress("SENSELESS_COMPARISON")
    fun setFullList(list: List<Paper>) {
        fun sanitizeMap(map: Map<String, String>?, onChanged: (MutableMap<String, String>) -> Unit) {
            val extIds = map?.toMutableMap()
            extIds?.entries?.forEach {
                if (it.value == null) {
                    extIds.remove(it.key)
                    onChanged(extIds)
                }
            }
        }

        fun sanitize() {
            fullList.forEach { paper ->
                sanitizeMap(paper.details.externalIds) {
                    paper.details.externalIds = it
                }
                sanitizeMap(paper.details.tldr) {
                    paper.details.tldr = it
                }
            }
        }

        fullList.clear()
        fullList.addAll(list)
        filteredList = null
        sanitize()
        updateShadowMap()
    }

    private fun updateItemInBothLists(id: Long, transformer: (Paper) -> Paper) {
        var new: Paper? = null
        fullList.forEachIndexed { index, paper ->
            if (paper.uniqueId == id) {
                new = transformer(paper)
                if (paper == new)
                    return
                new.uniqueId = UniqueIdGenerator.nextId()
                fullList[index] = new
            }
        }
        new?.let {
            filteredList?.let {
                val findex = filteredShadowMap?.get(id) ?: return
                filteredList = it.toMutableList().apply {
                    this[findex] = new
                }.toList()
            }
        }
    }

    private fun updateShadowMap() {
        filteredShadowMap = filteredList?.let {
            val map: MutableMap<Long, Int> = mutableMapOf()
            it.forEachIndexed { index, paper ->
                map[paper.uniqueId] = index
            }
            map
        }
    }

    fun delete(id: Long) {
        fullList.removeIf { p -> p.uniqueId == id }
        filteredList?.let { list ->
            val tmp2 = list.toMutableList()
            tmp2.removeIf { p -> p.uniqueId == id }
            filteredList = tmp2.toList()
        }
        updateShadowMap()
    }

    fun deleteAllFiltered() {
        filteredList?.let { list ->
            val dois = list.map { it.paperId }.toSet()
            val fList = fullList.toMutableList()
            fList.removeIf { dois.contains(it.paperId) }
            fullList.clear()
            fullList.addAll(fList)
            filteredList = null
            updateShadowMap()
        }
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
        filteredList?.let { papers ->
            val updatedPapers = papers.map { paper ->
                if (paper.tag == newTag) paper else paper.copy(newTag)
            }
            filteredList = updatedPapers
        }
    }

    fun setFullTagsFromFiltered() {
        val tagMap: Map<String, Tag> = filteredList?.associate { Pair(it.paperId ?: "", it.tag) } ?: emptyMap()
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
        filteredList?.let {
            filteredList = sort(it, type)
        }
        updateShadowMap()
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
        result = 31 * result + (filteredList?.hashCode() ?: 0)
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