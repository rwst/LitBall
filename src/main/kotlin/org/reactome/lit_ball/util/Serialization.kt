package org.reactome.lit_ball.util

import kotlinx.serialization.json.Json

/**
 * A utility class for configuring the behavior of the JSON serialization/deserialization.
 */
object ConfiguredJson {
    private var json = Json {
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
    }

    fun get() = json
}

object ConfiguredUglyJson {
    private var json = Json {
        prettyPrint = false
        isLenient = true
        encodeDefaults = true
    }

    fun get() = json
}