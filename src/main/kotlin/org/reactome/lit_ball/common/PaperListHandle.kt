package common

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import util.CantHappenException
import window.components.SortingType

/**
 * Paper list handle behaves as full paper list, unless filtered list is set.
 * It also encapsuloates low level list handling to relieve class PaperList.
 */
class PaperListHandle {
    private var fullList = mutableStateListOf<Paper>()
    private var filteredList: List<Paper>? = null
    private var fullShadowMap: MutableMap<Int, Int> = mutableMapOf()
    private var filteredShadowMap: MutableMap<Int, Int>? = null

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

    fun getList(): List<Paper> {
        if (filteredList == null)
            return fullList
        return filteredList as List<Paper>
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
            fullList.forEachIndexed { index, paper ->
                val newPaper: Paper = paper
                var isChanged = false
                sanitizeMap(paper.details.externalIds) {
                    newPaper.details.externalIds = it
                    isChanged = true
                }
                sanitizeMap(paper.details.tldr) {
                    newPaper.details.tldr = it
                    isChanged = true
                }
                if (isChanged)
                    fullList = fullList.toMutableList().apply {
                        this[index] = newPaper
                    }.toMutableStateList()
            }
        }
        fullList = list.toMutableStateList()
        filteredList = null
        sanitize()
        updateShadowMap()
    }

    private fun getShadowMap(): MutableMap<Int, Int> {
        if (filteredShadowMap == null) return fullShadowMap
        return filteredShadowMap as MutableMap<Int, Int>
    }

    private fun updateItemInBothLists(id: Int, transformer: (Paper) -> Paper) {
        val index = fullShadowMap[id] ?: return
        val old = fullList[index]
        val new = transformer(old)
        if (old == new)
            return
        fullList[index] = new
        filteredList?.let {
            val findex = filteredShadowMap?.get(id) ?: return
            filteredList = it.toMutableList().apply {
                this[findex] = new
            }.toList()
        }
    }

    private fun updateShadowMap() {
        fullShadowMap.clear()
        fullList.forEachIndexed { index, paper ->
            fullShadowMap[paper.id] = index
        }
        filteredShadowMap = filteredList?.let {
            val map: MutableMap<Int, Int> = mutableMapOf()
            it.forEachIndexed { index, paper ->
                map[paper.id] = index
            }
            map
        }
    }

    fun delete(doi: String?) {
        if (doi.isNullOrEmpty()) return
        fullList.removeIf { p -> p.paperId?.let { it == doi } ?: false }
        filteredList?.let { list ->
            val tmp2 = list.toMutableList()
            tmp2.removeIf { p -> p.paperId?.let { it == doi } ?: false }
            filteredList = tmp2.toList()
        }
        updateShadowMap()
    }

    fun deleteAllFiltered() {
        filteredList?.let { list ->
            val dois = list.map { it.paperId }.toSet()
            val fList = fullList.toMutableList()
            fList.removeIf { dois.contains(it.paperId) }
            fullList = fList.toMutableStateList()
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
        fullList.replaceAll { Paper(it.id, it.details, tag, it.flags, it.details.externalIds?.get("DOI")?.lowercase()) }
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

    fun setTag(id: Int, tag: Tag) {
        updateItemInBothLists(id) {
            if (it.tag == tag)
                it
            else
                Paper(it.id, it.details, tag, it.flags, it.details.externalIds?.get("DOI")?.lowercase())
        }
    }

    fun setFlag(id: Int, flagNo: Int, value: Boolean) {
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

    fun getDisplayedPaper(index: Int): Paper? {
        val i = getShadowMap()[index] ?: return null
        return getList()[i]
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