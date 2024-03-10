package org.reactome.lit_ball.common

enum class ArticleType(val s2name: String) {
    ARTICLE("JournalArticle"),
    CONFERENCE("Conference"),
    REVIEW("Review");
}

fun typeStringsToBoolArray(types: List<String>): BooleanArray {
    return BooleanArray(ArticleType.entries.size) { index ->
        ArticleType.entries[index].s2name in types
    }
}

fun typeMatches(publicationTypes: List<String>?, filteredTypes: BooleanArray?): Boolean {
    publicationTypes?.let { pTypes ->
        filteredTypes?.let { fTypes ->
            return pTypes.any {
                ArticleType.entries.toTypedArray().forEachIndexed { index, articleType ->
                    if (fTypes[index] && it == articleType.s2name)
                        return@any true
                }
                return@any false
            }
        }
        return false
    }
    return true
}

