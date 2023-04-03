package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable

@Serializable
class Query : SerialDBClass() {
    private val setting = QuerySetting()
    private val actions = mutableListOf<LitAction>()
    override fun toString(): String {
        return "Query(setting=$setting, actions=$actions)"
    }
}
