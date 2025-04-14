package util

import common.QuerySetting

object S2SearchExpression {
    val logicOpSymbols = listOf(" + ", " | ", " -")
    fun from(setting: QuerySetting): String {
        val matcher = StringPatternMatcher(setting)
        return matcher.getS2SearchExpression()
    }
}

private val notRegex = "-\\s*".toRegex()
private val spaceRegex = "\\s+".toRegex()

fun StringPatternMatcher.getS2SearchExpression(): String {
    if (this.parser1.theExpr.isEmpty()) {
        return this.parser1.wordList.joinToString(separator = " | ") {
            if (it.contains(' '))
                "\"$it\""
            else it
        }
    }
    var expr = this.parser1.theExpr
    StringPatternMatcher.logicOpRegexes.forEachIndexed { idx, rgx ->
        expr = expr.replace(rgx, S2SearchExpression.logicOpSymbols[idx])
    }
    expr = expr.replace(spaceRegex, " ")
    expr = expr.replace(notRegex, "-")
    this.parser1.wordList.forEachIndexed { index, s ->
        var str = s
        if (s.contains(' '))
            str = "\"$s\""
        expr = expr.replaceFirst("word$index\\b".toRegex(), str)
    }
    return expr
}