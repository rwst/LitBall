package common

import kotlinx.serialization.Serializable

@Serializable
enum class QueryType(val pretty: String) {
    EXPRESSION_SEARCH("Expression\nSearch"),
    SNOWBALLING("Snowballing"),
    SUPERVISED_SNOWBALLING("Snowballing with\nInterleaved Supervision"),
    SIMILARITY_SEARCH("Similarity\nSearch")
}