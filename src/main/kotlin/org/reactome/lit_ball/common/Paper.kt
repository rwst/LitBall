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
    var flags: MutableSet<String> = mutableSetOf()
) {

    override fun toString(): String {
        return "Paper(details=$details, tag=$tag, flags=$flags)"
    }
}
