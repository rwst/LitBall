package common

enum class ArticleType(val s2name: String) {
    ARTICLE("JournalArticle"),
    CASEREPORT("CaseReport"),
    CLI8NICALTRIAL("ClinicalTrial"),
    CONFERENCE("Conference"),
    EDITORIAL("Editorial"),
    REVIEW("Review");
}

fun typeStringsToBoolArray(types: List<String>): BooleanArray {
    return BooleanArray(ArticleType.entries.size) { index ->
        ArticleType.entries[index].s2name in types
    }
}

fun boolArrayToTypeStrings(types: BooleanArray): List<String> {
    return ArticleType.entries.filterIndexed { index, _ -> types[index] }.map { it.s2name }
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

