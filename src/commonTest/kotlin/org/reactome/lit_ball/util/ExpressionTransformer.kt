package org.reactome.lit_ball.util

import org.reactome.lit_ball.common.QuerySetting
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class ExpressionTransformer {
    data class TestData(
        val posKeywords: String = "",
        val resultExpr: String = "",
    )
    companion object {
        val testData = listOf(
            TestData(
                "A, B, C, D",
                "A | B | C | D"
            ),
            TestData(
                "(*valgus or bunion*) and   not (prevalence or incidence or epidemiology)",
                "(*valgus | bunion*) + -(prevalence | incidence | epidemiology)"
            )
        )
    }
    @Test
    fun shouldFindMatches() {
        testData.forEach {
            val setting = QuerySetting(mandatoryKeyWords = StringPatternMatcher.patternSettingFrom(it.posKeywords))
            val expr = S2SearchExpression.from(setting)
            assertEquals(expr, it.resultExpr)
        }
    }
}
