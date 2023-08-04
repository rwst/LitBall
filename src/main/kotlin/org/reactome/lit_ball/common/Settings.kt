package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Paths

@Serializable
object Settings {
    var map: MutableMap<String, String> = mutableMapOf()
    private var initialized = false
    private const val PATH = "settings.json"
    private val Json = Json { prettyPrint = true }
    fun load() {
        if (initialized)
            return
        else
            initialized = true
        val file = File(PATH)
        if (file.canRead()) {
            val text = file.readText()
            try {
                map = Json.decodeFromString(text)
            } catch (e: Exception) {
                handleException(e)
            }
        }
        else {
            reset()
            save()
        }
    }

    private fun reset() {
        map.clear()
        map["path-to-queries"] = Paths.get("").toAbsolutePath().toString()
        map["directory-prefix"] = "Query-"
    }

    fun save() {
        val text = Json.encodeToString(map)
        try {
            File(PATH).writeText(text)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleException(e: Exception) {
        when (e) {
            is IOException, is SerializationException -> {
                Logger.error(e)
                map = mutableMapOf()
            }
            else -> throw e
        }
    }

    override fun toString(): String {
        return "Settings()=$map"
    }
}
