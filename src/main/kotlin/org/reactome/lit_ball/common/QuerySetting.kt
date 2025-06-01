package common

import kotlinx.serialization.Serializable
import util.ConfiguredJson
import java.io.File

@Serializable
data class QuerySetting(
    val startDois: MutableList<String> = mutableListOf(),
    var type: QueryType = QueryType.SUPERVISED_SNOWBALLING,
    val mandatoryKeyWords: MutableSet<String> = mutableSetOf(),
    val forbiddenKeyWords: MutableSet<String> = mutableSetOf(),
    var classifier: String = "",
    val annotationClasses: MutableSet<String> = mutableSetOf(),
    var pubDate: String = "",
    val pubType: MutableList<String> = mutableListOf()
) {
    override fun toString(): String {
        return "QuerySetting(" +
            "type=${type.name}, " +
            "startDois=$startDois, " +
            "mandatoryKeyWords=$mandatoryKeyWords, " +
            "forbiddenKeyWords=$forbiddenKeyWords, " +
            "classifier=$classifier, " +
            "annotationClasses=$annotationClasses, " +
            "pubDate=$pubDate, " +
            "pubType=$pubType" +
            ")"
    }

    fun pubTypeString(): String = pubType.joinToString(separator = ",")

    companion object {
        private val json = ConfiguredJson.get()
        
        fun fromFile(file: File): Result<QuerySetting> {
            return runCatching {
                val text = file.readText()
                json.decodeFromString<QuerySetting>(text)
            }
        }
    }
}