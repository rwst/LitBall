package org.reactome.lit_ball.util

import org.reactome.lit_ball.common.QuerySetting

object S2SearchExpression {
    fun from(setting: QuerySetting): String {
        val matcher = StringPatternMatcher(setting)
        return matcher.getS2SearchExpression()
    }
}

val notRegex = "!\\s*".toRegex()
val spaceRegex = "\\s+".toRegex()

fun StringPatternMatcher.getS2SearchExpression(): String {
    if (this.parser1.theExpr.isEmpty()) {
        return this.parser1.wordList.joinToString(separator = " | ") {
            if (it.contains(' '))
                "\"$it\""
            else it
        }
    }
    var expr = this.parser1.theExpr
    expr = expr.replace("||", " | ")
        .replace("&&", " + ")
        .replace(notRegex, " !")
        .replace(spaceRegex, " ")
    this.parser1.wordList.forEachIndexed { index, s ->
        var str = s
        if (s.contains(' '))
            str = "\"$s\""
        expr = expr.replaceFirst("word$index\\b".toRegex(), str)
    }
    return expr
}