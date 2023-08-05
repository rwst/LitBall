package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
class QuerySetting {
    private var posKeyWords = mutableSetOf <String>()
    private var negKeyWords = mutableSetOf <String>()
    private var classifier = "virus-EXP"
    override fun toString(): String {
        return "QuerySetting(posKeyWords=$posKeyWords, negKeyWords=$negKeyWords, classifier=$classifier)"
    }
    companion object {
        private val json = Json { prettyPrint = true }
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