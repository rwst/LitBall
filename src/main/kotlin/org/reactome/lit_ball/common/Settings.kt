package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.handleException
import org.reactome.lit_ball.window.components.SortingType
import java.io.File
import java.nio.file.Paths
import dev.dirs.ProjectDirectories

@Serializable
object Settings {
    var map: MutableMap<String, String> = mutableMapOf()
    var initialized = false
    private val myProjDirs: ProjectDirectories = ProjectDirectories.from("org", "reactome", "LitBall")
    private val configDir: String = myProjDirs.configDir
    private val PATH = "$configDir/settings.json"
    private val json = ConfiguredJson.get()
    fun load() {
        if (initialized)
            return
        else
            initialized = true
        val configDirectory = File(configDir)
        if (!configDirectory.exists()) {
            if (!configDirectory.mkdir())
                throw IllegalArgumentException("Directory could not be created: $configDir")
        }
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
        RootStore.refreshQueryPathDisplay()
    }

    private fun reset() {
        map.clear()
        map["path-to-queries"] = Paths.get("").toAbsolutePath().toString()
        map["directory-prefix"] = "Query-"
        map["path-to-YDF"] = Paths.get("").toAbsolutePath().toString()
        map["path-to-classifiers"] = Paths.get("").toAbsolutePath().toString() + "/classifier"
        map["query-sort-type"] = SortingType.ALPHA_ASCENDING.toString()
        map["paper-sort-type"] = SortingType.ALPHA_ASCENDING.toString()
        map["cache-max-age-days"] = "30"
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
