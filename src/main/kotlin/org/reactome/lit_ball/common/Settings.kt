package common

import dev.dirs.ProjectDirectories
import dev.dirs.UserDirectories
import kotlinx.serialization.Serializable
import util.ConfiguredJson
import util.handleException
import window.components.SortingType
import java.io.File
import java.nio.file.Paths

@Serializable
object Settings {
    var map: MutableMap<String, String> = mutableMapOf()
    val advancedSet = setOf("directory-prefix")
    var initialized = false
    private val myProjDirs: ProjectDirectories = ProjectDirectories.from("org", "reactome", "LitBall")
    private val configDir: String = myProjDirs.configDir
    private val homeDir: String = UserDirectories.get().homeDir
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
        var newMap: Map<String, String> = emptyMap()
        reset()
        if (file.canRead()) {
            val text = file.readText()
            try {
                newMap = json.decodeFromString(text)
            } catch (e: Exception) {
                handleException(e)
            }
        } else {
            save()
        }
        map += newMap
    }

    private fun reset() {
        map.clear()
        map["AG-service"] = "S2"
        map["path-to-queries"] = homeDir
        map["directory-prefix"] = "Query-"
        map["path-to-YDF"] = Paths.get("").toAbsolutePath().toString()
        map["path-to-classifiers"] = Paths.get("").toAbsolutePath().toString() + "/classifier"
        map["query-sort-type"] = SortingType.ALPHA_ASCENDING.toString()
        map["paper-sort-type"] = SortingType.ALPHA_ASCENDING.toString()
        map["cache-max-age-days"] = "30"
        map["S2-API-key"] = ""
        map["OpenAlex-email"] = ""
        map["Entrez-API-key"] = ""
        map["PYTHONPATH"] = System.getenv("PYTHONPATH") ?: ""
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

    val helpText = mapOf(
        "path-to-queries" to """
            Directory containing all current query subdirectories.
            If the query list gets too big, organize your query topics
            by creating additional directories.
        """.trimIndent(),
        "path-to-YDF" to """
            If you installed Yggdrasil Decision Forests on Linux,
            set the path to it here, to be able to use
            a classifier.
        """.trimIndent(),
        "path-to-classifiers" to """
            Directory where YDF models are stored that get used
            as text classifiers. The query-specific model is set
            in the query settings.
        """.trimIndent(),
        "query-sort-type" to """
            Current sorting type for the query list. Change this
            also by clicking on the resp. sort buttons.
        """.trimIndent(),
        "paper-sort-type" to """
            Current sorting type for the paper list. Change this
            also by clicking on the resp. sort buttons.
        """.trimIndent(),
        "cache-max-age-days" to """
            Number of days after which a completed query (no more
            new accepted papers after a snowballing round) can be
            started afresh using the existing accepted papers as
            core for snowballing.
        """.trimIndent(),
        "S2-API-key" to """
            Request an API key from Semantic Scholar for faster
            access through bulk queries. Also enables queries of
            type 1 (bulk expression search).
        """.trimIndent(),
        "OpenAlex-email" to """
            Sending a valid mail address with requests to openalex.org
            will put you in the "polite pool" which has much faster and
            more consistent response times.
        """.trimIndent(),
        "Entrez-API-key" to """
            Input your API key from your NCBI account for faster
            access to Entrez services (not implemented).
        """.trimIndent(),
        "PYTHONPATH" to """
            Path to your Python where Spacy / Prodigy is installed.
        """.trimIndent(),
        "AG-service" to """
            S2 is the only choice at the moment.
        """.trimIndent()
//            One of S2 / OpenAlex.
    )
}
