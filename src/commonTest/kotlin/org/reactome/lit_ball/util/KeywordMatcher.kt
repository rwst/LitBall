package org.reactome.lit_ball.util

import org.reactome.lit_ball.common.QuerySetting
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class KeywordMatcherTest {
    data class TestData(
        val posKeywords: String = "",
        val negKeywords: String = "",
        val text1: String = "",
        val text2: String = "",
        val result: Boolean = false
    )

    companion object {
        val testData = listOf(
            TestData(
                "ST3GAL2, Alpha 2.3-ST 2, Beta-galactoside alpha-2.3-sialyltransferase 2, Gal-NAc6S, Monosialoganglioside sialyltransferase, ST3Gal II, ST3GalII, ST3GalA.2, Sialyltransferase 4B, SIAT4-B",
                "",
                "Human congenital disorders of ganglioside biosynthesis result in paraplegia, epilepsy, and intellectual disability. To better understand sialoglycan functions in the nervous system, we studied brain anatomy, histology, biochemistry, and behavior in mice with engineered mutations in St3gal2 and St3gal3",
                "Sialylation regulates brain structure and function",
                true
            ),
            TestData(
                "B3GALT4, Beta-1.3-galactosyltransferase 4, GALT4, Beta-1.3-GalTase 4, Beta3Gal-T4, Beta3GalT4, GalT4, b3Gal-T4, Gal-T2, Ganglioside galactosyltransferase",
                "",
                "The galactosyltransferase family. Galactose is transferred via several linkages to acceptor structures by galactosyltransferase enzymes, which are involved in the formation of several classes of glycoconjugates and in lactose biosynthesis in prokaryotes and eukaryotes.",
                "",
                false
            ),
            TestData(
                "B3GALT4, Beta-1.3-galactosyltransferase 4, GALT4, Beta-1.3-GalTase 4, Beta3Gal-T4, Beta3GalT4, GalT4, b3Gal-T4, Gal-T2, Ganglioside galactosyltransferase",
                "",
                "The ganglioside galactosyltransferase family.",
                "",
                true
            ),
        )
    }
    @Test
    fun shouldFindMatches() {
        testData.forEach {
            val setting = QuerySetting(it.posKeywords.splitToSet(","), it.negKeywords.splitToSet(","))
            val matcher = KeywordMatcher(setting)
            assertEquals(matcher.match(it.text1, it.text2), it.result)
        }
    }
}