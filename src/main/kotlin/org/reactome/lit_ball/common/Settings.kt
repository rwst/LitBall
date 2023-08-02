package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

@Serializable
object Settings {
    private var map: MutableMap<String, String> = mutableMapOf()
    private const val PATH = "settings.json"
    private val Json = Json { prettyPrint = true }
    fun load() {
        val file = File(PATH)
        if (file.canRead()) {
            val text = file.readText()
            try {
                map = Json.decodeFromString(text)
            } catch (e: Exception) {
                handleException(e)
            }
        }
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
