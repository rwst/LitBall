package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable

@Serializable
class QuerySetting : SerialDBClass() {
    private val startRefSet = mutableSetOf <String>()
    fun add(s: String) {
        startRefSet.add(s)
    }
    override fun toString(): String {
        return "QuerySetting(startRefSet=$startRefSet)"
    }
}