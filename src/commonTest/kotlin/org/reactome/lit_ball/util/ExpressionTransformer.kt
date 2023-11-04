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
            ),
            TestData(
                "(software OR applicati* OR systems ) AND (fault* OR defect* OR quality OR error-prone) AND (predict* OR prone* OR probability OR assess* OR detect* OR estimat* OR classificat*)",
                "(software | applicati* | systems ) + (fault* | defect* | quality | error-prone) + (predict* | prone* | probability | assess* | detect* | estimat* | classificat*)"
            ),
            TestData(
                "(software OR application OR systems ) AND (fault OR defect OR quality OR error-prone) AND (predict OR prediction OR prone OR probability OR assess OR assession OR detect OR detection OR estimate OR estimation OR classification OR classify)",
                "(software | application | systems ) + (fault | defect | quality | error-prone) + (predict | prediction | prone | probability | assess | assession | detect | detection | estimate | estimation | classification | classify)"
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
