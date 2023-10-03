package org.reactome.lit_ball.util

import org.reactome.lit_ball.common.QuerySetting

class KeywordMatcher (setting: QuerySetting) {
    private var mandatoryKeyWordRegexes: List<Regex>
    private var forbiddenKeyWordRegexes: List<Regex>
    init {
        mandatoryKeyWordRegexes = makeRegexListFrom(setting.mandatoryKeyWords)
        forbiddenKeyWordRegexes = makeRegexListFrom(setting.forbiddenKeyWords)
    }
    private fun makeRegexListFrom (aSet: MutableSet<String>?) = aSet
        ?.filter { it.isNotEmpty() }
        ?.map { it.split(".")
            .joinToString(separator = ".", prefix = "\\b", postfix = "\\b")
            { it1 -> Regex.escape(it1) }}
        ?.map { it.toRegex(RegexOption.IGNORE_CASE) } ?: emptyList()

    fun match(text1: String, text2: String): Boolean {
        return mandatoryKeyWordRegexes.any { regex1 ->
                regex1.containsMatchIn(text1)
            } && forbiddenKeyWordRegexes.none { regex2 ->
                regex2.containsMatchIn(text2)
            }
    }
}