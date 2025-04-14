package util

import common.QuerySetting

object OASearchExpression {
    val logicOpSymbols = listOf(" AND ", " OR ", " NOT ")
    fun from(setting: QuerySetting): String {
        val matcher = StringPatternMatcher(setting)
        return matcher.getOASearchExpression()
    }
}

private val notRegex = " NOT\\s*".toRegex()
private val spaceRegex = "\\s+".toRegex()

fun StringPatternMatcher.getOASearchExpression(): String {
    if (this.parser1.theExpr.isEmpty()) {
        return this.parser1.wordList.joinToString(separator = " | ") {
            if (it.contains(' '))
                "\"$it\""
            else it
        }
    }
    var expr = this.parser1.theExpr
    StringPatternMatcher.logicOpRegexes.forEachIndexed { idx, rgx ->
        expr = expr.replace(rgx, OASearchExpression.logicOpSymbols[idx])
    }
    expr = expr.replace(spaceRegex, " ")
    expr = expr.replace(notRegex, "NOT")
    this.parser1.wordList.forEachIndexed { index, s ->
        var str = s
        if (s.contains(' '))
            str = "\"$s\""
        expr = expr.replaceFirst("word$index\\b".toRegex(), str)
    }
    return expr
}