package org.reactome.lit_ball.util

import org.testng.Assert
import org.testng.annotations.Test

class DateMatcherTest {
    data class TestData(
        val filteredDate: String = "",
        val publicationDate: String?,
        val result: Boolean = false
    )
    companion object {
        val testData = listOf(
            TestData("", "2002", true),
            TestData("", "2002-01-04", true),
            TestData("2002", "2002", true),
            TestData("2000-2004", "2002", true),
            TestData("1999", "2002", false),
            TestData("1997-1999", "2002", false),
            TestData("1999-", "2002", true),
            TestData("2004-", "2002", false),
            TestData("2004-2006", "2002", false),
            TestData("-2006", "2002", true),
            TestData("-1999", "2002", false),
            TestData("2002-", "2002", true),
            TestData("-2002", "2002", true),
        )
    }

    @Test
    fun shouldFindMatches() {
        testData.forEach {
            val matcher = DateMatcher(it.filteredDate)
            val result = matcher.matches(it.publicationDate)
            if (result != it.result)
                System.err.println("FAIL: |${it.filteredDate}|${it.publicationDate}|: $result")
            Assert.assertEquals(result, it.result)
        }
    }
}