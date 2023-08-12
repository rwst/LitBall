package org.reactome.lit_ball.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Tag {
    @SerialName("OTHER")
    Other,
    @SerialName("EXP")
    Exp,
    @SerialName("DRUG")
    Drug,
}

@Serializable
class Paper(var id: Int, val details: S2Service.PaperDetailsWithAbstract, var tag: Tag, var flags: MutableSet<String> = mutableSetOf()) {

    override fun toString(): String {
        return "Paper(details=$details, tag=$tag, flags=$flags)"
    }
}
