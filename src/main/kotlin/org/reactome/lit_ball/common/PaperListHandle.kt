package org.reactome.lit_ball.common

import org.reactome.lit_ball.window.components.SortingType

/**
 * Paper list handle behaves as full paper list, unless filtered list is set.
 * It also encapsuloates low level list handling to relieve class PaperList.
 */
class PaperListHandle {
    private var fullList: List<Paper> = listOf()
    private var filteredList: List<Paper>? = null
    private var fullShadowMap: MutableMap<Int, Int> = mutableMapOf()
    private var filteredShadowMap: MutableMap<Int, Int>? = null

    fun getList(): List<Paper> {
        if (filteredList == null)
            return fullList
        return filteredList as List<Paper>
    }

    fun getFullList(): List<Paper> {
        return fullList
    }

    private fun setList(list: List<Paper>) {

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
                    }.toList()
            }
        }
        fullList = list
        filteredList = null
        sanitize()
        updateShadowMap()
    }

    private fun getShadowMap(): MutableMap<Int, Int> {
        if (filteredShadowMap == null) return fullShadowMap
        return filteredShadowMap as MutableMap<Int, Int>
    }

    private fun updateItem(id: Int, transformer: (Paper) -> Paper) {
        val index = fullShadowMap[id] ?: return
        val list = getList()
        val old = list[index]
        val new = transformer(old)
        if (old == new)
            return
        setList(list.toMutableList().apply {
            this[index] = new
        }.toList() )
    }
    private fun updateShadowMap() {
        fullShadowMap.clear()
        fullList.forEachIndexed { index, paper ->
            fullShadowMap[paper.id] = index
        }
        if (filteredList != null) {
            filteredShadowMap = mutableMapOf()
            filteredList!!.forEachIndexed { index, paper ->
                filteredShadowMap!![paper.id] = index
            }
        }
        else
            filteredShadowMap = null
    }
    fun delete(doi: String?) {
        if (doi.isNullOrEmpty()) return
        val tmp1 = fullList.toMutableList()
        tmp1.removeIf { p -> p.doi?.let { it == doi }?: false }
        fullList = tmp1.toList()
        filteredList?.let { list ->
            val tmp2 = list.toMutableList()
            tmp2.removeIf { p -> p.doi?.let { it == doi } ?: false }
            filteredList = tmp2.toList()
        }
        updateShadowMap()
    }

    /**
     * Set all tags in the full list to the [tag].
     *
     * @param tag
     */
    fun setFullAllTags(tag: Tag) {
        val ids= fullList.map { it.id }
        ids.forEach { setTag(it, tag) }
    }

    fun setTag(id: Int, tag: Tag) {
        updateItem(id) {
            if (it.tag == tag)
                it
            else
                Paper(it.id, it.details, tag, it.flags, it.details.externalIds?.get("DOI")?.uppercase())
        }
    }

    fun setFlag(id: Int, flagNo: Int, value: Boolean) {
        val flag = PaperList.flagList[flagNo]
        updateItem(id) {
            if (!value)
                flag.let { it1 -> it.flags.add(it1) }
            else
                it.flags.remove(flag)
            it
        }
    }

    fun sort(type: SortingType) {
        val list = getList()
        setList(when (type) {
            SortingType.ALPHA_ASCENDING -> list.sortedBy { it.details.title }
            SortingType.ALPHA_DESCENDING -> list.sortedByDescending { it.details.title }
            SortingType.DATE_ASCENDING -> list.sortedBy { it.details.publicationDate }
            SortingType.DATE_DESCENDING -> list.sortedByDescending { it.details.publicationDate }
            else ->
                throw Exception("can't happen: $type")
        })
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