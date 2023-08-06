package org.reactome.lit_ball.util

import kotlinx.serialization.json.Json

object ConfiguredJson {
    private var json = Json {
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
    }
    fun get() = json
}