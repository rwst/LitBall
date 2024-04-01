package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable

@Serializable
enum class QueryType(val pretty: String) {
    EXPRESSION_SEARCH("Expression Search"),
    SNOWBALLING("Snowballing"),
    SUPERVISED_SNOWBALLING("Snowballing with Interleaved Supervision"),
    SIMILARITY_SEARCH("Similarity Search")
}