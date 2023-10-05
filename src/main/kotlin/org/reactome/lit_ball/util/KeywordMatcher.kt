package org.reactome.lit_ball.util

import org.reactome.lit_ball.common.QuerySetting

class KeywordMatcher(setting: QuerySetting) {

    private val mandatoryKeyWordRegexes: List<Regex> = makeRegexListFrom(setting.mandatoryKeyWords)
    private val forbiddenKeyWordRegexes: List<Regex> = makeRegexListFrom(setting.forbiddenKeyWords)

    private fun makeRegexListFrom(aSet: MutableSet<String>?) = aSet
        ?.filter { it.isNotEmpty() }
        ?.map { s ->
            s.split(".")
            .joinToString(separator = ".", prefix = "\\b", postfix = "\\b") { Regex.escape(it) } }
        ?.map { it.toRegex(RegexOption.IGNORE_CASE) } ?: emptyList()

    fun match(text1: String, text2: String) =
        mandatoryKeyWordRegexes.any { it.containsMatchIn(text1) } &&
            forbiddenKeyWordRegexes.none { it.containsMatchIn(text2) }
}