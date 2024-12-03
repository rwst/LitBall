package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.Logger
import java.io.File

@Serializable
data class QuerySetting(
    var startDois: List<String> = emptyList(),
    var type: QueryType = QueryType.SUPERVISED_SNOWBALLING,
    var mandatoryKeyWords: MutableSet<String> = mutableSetOf(),
    var forbiddenKeyWords: MutableSet<String> = mutableSetOf(),
    var classifier: String = "",
    var annotationClasses: MutableSet<String> = mutableSetOf(),
    var pubDate: String = "",
    var pubType: List<String> = emptyList()
) {
    override fun toString(): String {
        return "QuerySetting(type=${type.name} posKeyWords=$mandatoryKeyWords, negKeyWords=$forbiddenKeyWords, classifier=$classifier), annotationClasses=$annotationClasses"
    }

    companion object {
        private val json = ConfiguredJson.get()
        fun fromFile(file: File): QuerySetting {
            val text = file.readText()
            return try {
                json.decodeFromString<QuerySetting>(text)
            } catch (e: Exception) {
                Logger.error(e)
                QuerySetting()
            }
        }
    }
}