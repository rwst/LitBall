package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.handleException
import org.reactome.lit_ball.window.components.SortingType
import java.io.File
import java.nio.file.Paths

@Serializable
object Settings {
    var map: MutableMap<String, String> = mutableMapOf()
    var initialized = false
    private const val PATH = "settings.json"
    private val json = ConfiguredJson.get()
    fun load() {
        if (initialized)
            return
        else
            initialized = true
        val file = File(PATH)
        if (file.canRead()) {
            val text = file.readText()
            try {
                map = json.decodeFromString(text)
            } catch (e: Exception) {
                handleException(e)
            }
        } else {
            reset()
            save()
        }
    }

    private fun reset() {
        map.clear()
        map["path-to-queries"] = Paths.get("").toAbsolutePath().toString()
        map["directory-prefix"] = "Query-"
        map["path-to-YDF"] = Paths.get("").toAbsolutePath().toString()
        map["path-to-classifiers"] = Paths.get("").toAbsolutePath().toString() + "/classifier"
        map["query-sort-type"] = SortingType.ALPHA_ASCENDING.toString()
    }

    fun save() {
        val text = json.encodeToString(map)
        try {
            File(PATH).writeText(text)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override fun toString(): String {
        return "Settings()=$map"
    }
}
