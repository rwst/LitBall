package org.reactome.lit_ball.util

import common.QuerySetting
import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import util.StringPatternMatcher

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
                "(ST3GAL2 OR Alpha 2,3-ST 2 OR Beta-galactoside alpha-2,3-sialyltransferase 2 OR Gal-NAc6S OR Monosialoganglioside sialyltransferase OR ST3Gal II OR ST3GalII OR ST3GalA.2 OR Sialyltransferase 4B OR SIAT4-B)",
                "",
                "Human congenital disorders of ganglioside biosynthesis result in paraplegia, epilepsy, and intellectual disability. To better understand sialoglycan functions in the nervous system, we studied brain anatomy, histology, biochemistry, and behavior in mice with engineered mutations in St3gal2 and St3gal3",
                "Sialylation regulates brain structure and function",
                true
            ),
            TestData(
                "(B3GALT4 OR Beta-1,3-galactosyltransferase 4 OR GALT4 OR Beta-1,3-GalTase 4 OR Beta3Gal-T4 OR Beta3GalT4 OR GalT4 OR b3Gal-T4 OR Gal-T2 OR Ganglioside galactosyltransferase)",
                "",
                "The galactosyltransferase family. Galactose is transferred via several linkages to acceptor structures by galactosyltransferase enzymes, which are involved in the formation of several classes of glycoconjugates and in lactose biosynthesis in prokaryotes and eukaryotes.",
                "",
                false
            ),
            TestData(
                "(B3GALT4 OR Beta-1,3-galactosyltransferase 4 OR GALT4 OR Beta-1,3-GalTase 4 OR Beta3Gal-T4 OR Beta3GalT4 OR GalT4 OR b3Gal-T4 OR Gal-T2 OR Ganglioside galactosyltransferase)",
                "",
                "The ganglioside galactosyltransferase family.",
                "",
                true
            ),
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
            TestData(
                "(*valgus or bunion*) and (prevalence or incidence or epidemiology)",
                "",
                "Prevalence estimation and familial tendency of common forefoot deformities in Turkey: A survey of 2662 adults. Hallux valgus, hammertoe and bunionette images were provided as references and every adult participant without any known forefoot problems or past forefoot surgery history was asked to rate his/her foot and to respond the questions about family history and shoe wearing habits. Responses were statistically analyzed.",
                "",
                true,
            ),
            TestData(
                "(*valgus or bunion*) and (prevalence or incidence or epidemiology)",
                "",
                "Hallux valgus in a random population in Spain and its impact on quality of life and functionality. The aim of this study was to determine the prevalence of Hallux valgus (HV) and the association between the presence thereof and quality of life, dependence for basic and instrumental activities of daily living and foot function. Prevalence study was carried out in a random population sample (n = 1837) ",
                "",
                true,
            ),
            TestData(
                "(*valgus or bunion*) and (prevalence or incidence or epidemiology)",
                "",
                "Hallux valgus, the lateral deviation of the great toe, can result in poor balance, impaired mobility and is an independent risk factor for falls. This research aims to compare the prevalence of hallux valgus in subpopulations of medieval Cambridge",
                "",
                true,
            ),
            TestData(
                "(*valgus or bunion*) and (prevalence or incidence or epidemiology)",
                "",
                "Hallux valgus in a random population in Spain and its impact on quality of life and functionality The presence of Hallux valgus was associated with reduced quality of life, dependence for basic and instrumental activities of daily living and foot function, and increases foot pain, disability and functional limitation. \n",
                "",
                false,
                ),
            TestData(
                "(*valgus or bunion*) and (prevalence or incidence or epidemiology)",
                "",
                "Prevalence estimation and familial tendency of common forefoot deformities in Turkey: A survey of 2662 adults This study concludes that forefoot deformities are common with high familial tendency and this may enable the anticipation of forthcoming deformities in order to take early action in prevention, in nearly the half of the population. \n",
                "",
                false
                ),
            TestData(
                "(Pain measurement OR Questionnaire OR instrument* OR form OR assessment* OR score OR measurement* OR scale OR tool*) AND (Back pain OR low back pain OR lumbago OR neck pain OR backache OR spinal pain OR neck ache OR neck pain) AND (Athlete* OR sport OR sportsman OR sportsmen OR sportswom*)",
                "",
                "Purpose: The purpose of this cross-sectional study was to compare the prevalence of low back pain (LBP) among female elite football and handball players to a matched non-professional active control group.\n" +
                        "\n" +
                        "Methods: The participants were requested to answer a questionnaire based on standardized Nordic questionnaires for musculoskeletal symptoms to assess the prevalence. Conclusion: There were no difference in LBP among female elite football and handball players compared with the control group. However, female elite athletes in football and handball reported a high prevalence of LBP compared to previous studies.",
                "",
                true
            ),
            TestData(
                "((*valgus or bunion*) and (prevalence or incidence or epidemiology))",
                "",
                "Hallux valgus in a random population in Spain and its impact on quality of life and functionality. The aim of this study was to determine the prevalence of Hallux valgus (HV) and the association between the presence thereof and quality of life, dependence for basic and instrumental activities of daily living and foot function. Prevalence study was carried out in a random population sample (n = 1837) ",
                "",
                true,
            ),
            TestData(
                "B3GALT4 OR Beta-1,3-galactosyltransferase 4 OR GALT4 OR Beta-1,3-GalTase 4 OR Beta3Gal-T4 OR Beta3GalT4 OR GalT4 OR b3Gal-T4 OR Gal-T2 OR Ganglioside galactosyltransferase",
                "",
                "The ganglioside galactosyltransferase family.",
                "",
                true
            ),
            TestData(
                "(Dengue || DENV) && (Antibody-dependent enhancement || Antibody Dependent Enhancement || ADE || autoimmune || autoimmunity || autoantibody || autoantibodies)",
                "",
                "Lorem DENV ipsum autoimmune stait",
                "",
                false
            ),
            TestData(
                "(Dengue or DENV) and (Antibody-dependent enhancement or Antibody Dependent Enhancement or ADE or autoimmune or autoimmunity or autoantibody or autoantibodies)",
                "",
                "Lorem DENV ipsum autoimmune stait",
                "",
                true
            ),
        )
    }
    @Test
    fun shouldFindMatches() {
        testData.forEach {
            val setting = QuerySetting(
                mandatoryKeyWords = StringPatternMatcher.patternSettingFrom(it.posKeywords),
                forbiddenKeyWords = StringPatternMatcher.patternSettingFrom(it.negKeywords)
            )
            val matcher = StringPatternMatcher(setting)
            if (matcher.match(it.text1, it.text2) != it.result)
                System.err.println("FAIL: ${it.text1}\n${it.posKeywords}")
            assertEquals(matcher.match(it.text1, it.text2), it.result)
        }
    }
}