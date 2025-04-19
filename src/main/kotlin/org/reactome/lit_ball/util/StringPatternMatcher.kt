package util

import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlException
import org.apache.commons.jexl3.MapContext
import common.QuerySetting
import java.util.*

const val tag = "KeywordMatcher"
val logicOps = listOf("and", "or", "not")
val logicOpSymbols = listOf("&&", "||", "!")
val logicDelims = logicOps.map { " $it " } + logicOps.map { " ${it.uppercase(Locale.ENGLISH)} " }

class StringPatternMatcher(setting: QuerySetting) {
    abstract class PatternParser(val regexes: List<Regex>, val expr: String) {
        abstract val wordList: List<String>
        abstract val theExpr: String
        abstract fun match(text: String): Boolean

        companion object {
            fun createFrom(aSet: Set<String>?): PatternParser {
                if (aSet.isNullOrEmpty())
                    return KeywordListParser(emptyList(), emptyList(), "")
                if (logicOpRegexes.any { it.containsMatchIn(aSet.first()) }) {
                    val wordList = aSet
                        .first()
                        .split("(", ")", *logicDelims.toTypedArray())
                        .map { it.trim() }
                        .filterNot { it.isEmpty() }
                    var expr = aSet.first()
                    wordList.forEachIndexed { index, s -> expr = expr.replaceFirst(s, "word$index") }
                    return LogicalExpressionParser(wordList,
                        wordList
                            .map { "\\b" + it.replace("*", "\\p{Alnum}*") + "\\b" }
                            .map { it.toRegex(RegexOption.IGNORE_CASE) },
                        expr
                    )
                }
                return KeywordListParser(
                    aSet.toList(),
                    makeRegexListFrom(aSet),
                    ""
                )
            }
        }
    }

    class KeywordListParser(words: List<String>, regexes: List<Regex>, expr: String) : PatternParser(regexes, expr) {
        override val wordList: List<String> = words
        override val theExpr: String = expr
        override fun match(text: String) = regexes.any { it.containsMatchIn(text) }
    }

    class LogicalExpressionParser(words: List<String>, regexes: List<Regex>, expr: String) :
        PatternParser(regexes, expr) {
        override val wordList: List<String> = words
        override val theExpr: String = expr
        override fun match(text: String): Boolean {
            val vars: MutableMap<String, Boolean> = mutableMapOf()
            regexes.forEachIndexed { index, regex ->
                vars["word$index"] = regex.containsMatchIn(text)
            }
            var resExpr: String = expr
            logicOpRegexes.forEachIndexed { idx, rgx -> resExpr = resExpr.replace(rgx, logicOpSymbols[idx]) }
            return ExpressionEvaluator.evaluateAsBoolean(resExpr, vars)
                ?: throw Exception("Could not evaluate logical expression")
        }
    }

    val parser1: PatternParser = PatternParser.createFrom(setting.mandatoryKeyWords)
    val parser2: PatternParser = PatternParser.createFrom(setting.forbiddenKeyWords)

    fun match(text1: String, text2: String): Boolean {
        return parser1.match(text1) && !parser2.match(text2)
    }

    companion object {
        val logicOpRegexes: List<Regex>

        init {
            logicOpRegexes = makeRegexListFrom(logicOps.toSet())
        }

        fun makeRegexListFrom(aSet: Set<String>): List<Regex> = aSet
            .filter { it.isNotEmpty() }
            .map { s ->
                s.split(".")
                    .joinToString(separator = ".", prefix = "\\b", postfix = "\\b") { Regex.escape(it) }
            }
            .map { it.toRegex(RegexOption.IGNORE_CASE) }

        fun patternSettingFrom(value: String): MutableSet<String> {
            if (logicOpRegexes.any { it.containsMatchIn(value) })
                return mutableSetOf(value)
            return value.splitToSet(",")
        }

        fun validateSetting(value: String): Boolean {
            if (logicOpRegexes.any { it.containsMatchIn(value) }) {
                val parser = PatternParser.createFrom(mutableSetOf(value))
                try {
                    parser.match("")
                } catch (e: Exception) {
                    Logger.error(e)
                    return false
                }
            }
            return true
        }
    }
}

object ExpressionEvaluator {
    private val jexlEngine = JexlBuilder().create()
    private val jexlContext = MapContext()

//    init {
//        jexlEngine.setSilent(true)
//        jexlEngine.set(true)
//    }

    private fun evaluate(expression: String): Any? = try {
        val jexlExpression = jexlEngine.createExpression(expression)
        jexlExpression.evaluate(jexlContext)
    } catch (e: JexlException) {
        Logger.error(e) //just logs
        null
    }

    fun evaluateAsBoolean(expression: String, vars: Map<String, Boolean>): Boolean? {
        vars.forEach { (key, value) -> jexlContext.set(key, value) }

        val boolean = evaluate(expression) as? Boolean
        if (boolean == null) {
            Logger.i(tag, "Could not evaluate expression '$expression' as Boolean")
        }
        return boolean
    }
}
