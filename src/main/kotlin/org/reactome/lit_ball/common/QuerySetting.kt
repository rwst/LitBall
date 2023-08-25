package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.Logger
import java.io.File

@Serializable
data class QuerySetting(
    var mandatoryKeyWords: MutableSet<String> = mutableSetOf(),
    var forbiddenKeyWords: MutableSet<String> = mutableSetOf(),
    var classifier: String = "virus-EXP",
    var annotationClasses: MutableSet<String> = mutableSetOf(),
) {
    override fun toString(): String {
        return "QuerySetting(posKeyWords=$mandatoryKeyWords, negKeyWords=$forbiddenKeyWords, classifier=$classifier), annotationClasses=$annotationClasses"
    }

    companion object {
        private val json = ConfiguredJson.get()
        fun fromFile(file: File): QuerySetting {
            val text = file.readText()
            return try {
                json.decodeFromString<QuerySetting>(text)
            } catch (e: Exception) {
                println(e)
                Logger.error(e)
                QuerySetting()
            }
        }
    }
}