package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable

@Serializable
class Settings(
    private var map: Map<String, String> = mutableMapOf()
) : SerialDBClass() {
    override fun toString(): String {
        return "Settings()=$map"
    }
}
