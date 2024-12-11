package org.reactome.lit_ball.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.reactome.lit_ball.service.S2Interface

enum class Tag {
    @SerialName("REJECTED")
    Rejected,

    @SerialName("ACCEPTED")
    Accepted,
}

@Serializable
class Paper(
    var id: Int = -1,
    val details: S2Interface.PaperDetails = S2Interface.PaperDetails(),
    var tag: Tag = Tag.Rejected,
    var flags: MutableSet<String> = mutableSetOf(),
    var paperId: String? = null,
) {

    override fun toString(): String {
        return "Paper(id=$id, details=$details, tag=$tag, flags=$flags, paperId=$paperId)"
    }

    fun setPaperIdFromDetails(): Paper {
        paperId = details.externalIds?.get("DOI")?.lowercase()
        if (paperId.isNullOrBlank()) {
            paperId = "s2:${details.paperId}"
        }
        return this
    }

    fun fixNullTldr(): Paper {
        val tldr = details.tldr
        if (tldr != null && tldr.get("text") == null) {
            details.tldr = null
        }
        return this
    }
}

fun idSetFromPaperDetailsList(pDList: List<S2Interface.PaperDetails>): MutableSet<String> {
    return idListFromPaperDetailsList(pDList).toMutableSet()
}

fun idListFromPaperDetailsList(pDList: List<S2Interface.PaperDetails>): List<String> {
    return pDList.map { pd ->
        pd.externalIds?.get("DOI")?.let { if (it.isNotBlank()) return@map it.lowercase() }
        "s2:${pd.paperId?.lowercase()}"
    }
}

fun idListFromPaperRefs(refList: List<S2Interface.PaperFullId>): List<String> {
    return refList.map { pd ->
        pd.externalIds?.get("DOI")?.let { if (it.isNotBlank()) return@map it.lowercase() }
        "s2:${pd.paperId?.lowercase()}"
    }
}