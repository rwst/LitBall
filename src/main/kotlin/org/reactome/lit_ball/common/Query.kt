package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable

@Serializable
data class Query(
    val id: Int,
    val text: String = "",
    val setting: QuerySetting = QuerySetting(),
    val actions: MutableList<LitAction> = mutableListOf()
) : SerialDBClass()
{

    override fun toString(): String {
        return "Query(setting=$setting, actions=$actions)"
    }
}
