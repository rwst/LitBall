package org.reactome.lit_ball.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.reactome.lit_ball.service.S2Service

enum class Tag {
    @SerialName("REJECTED")
    Rejected,

    @SerialName("ACCEPTED")
    Accepted,
}

@Serializable
class Paper(
    var id: Int = -1,
    val details: S2Service.PaperDetails = S2Service.PaperDetails(),
    var tag: Tag = Tag.Rejected,
    var flags: MutableSet<String> = mutableSetOf(),
    var paperId: String? = null,
) {

    override fun toString(): String {
        return "Paper(details=$details, tag=$tag, flags=$flags)"
    }

    fun setPaperIdFromDetails(): Paper {
        paperId = details.externalIds?.get("DOI")?.uppercase()
        if (paperId.isNullOrBlank()) {
            paperId = "S2:${details.paperId}"
        }
        return this
    }

    fun uppercaseDoi(): Paper {
        val extIds = details.externalIds?.toMutableMap()
        if (extIds != null) {
            val oldDoi = extIds["DOI"]
            val doi = oldDoi?.uppercase()
            if (doi != null && doi != oldDoi) {
                extIds["DOI"] = doi
                details.externalIds = extIds
            }
        }
        return this
    }
}
