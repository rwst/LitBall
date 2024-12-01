package org.reactome.lit_ball.util

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

fun formatDateToyyyyMMMddFormat(date: Date?): String {
    if (date == null) return "-.-"
    val format = SimpleDateFormat("yyyy-MMM-dd", Locale.ENGLISH)
    return format.format(date)
}

var scrollerTag: String = ""

fun setupLazyListScroller(
    tag: String,
    scope: CoroutineScope,
    state: LazyListState,
    setupAction: (Channel<Int>) -> Unit,
) {
    if (tag == scrollerTag) return
    scrollerTag = tag
    val scrollChannel = Channel<Int>()
    scope.launch {
        while (true) {
            val pos = scrollChannel.receive()
            state.scrollToItem(pos)
        }
    }
    setupAction(scrollChannel)
}

fun String.splitToSet(delim: String): MutableSet<String> =
    this.split(delim)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toMutableSet()

fun LocalDate.toEpochMilliseconds(): Long {
    return atStartOfDayIn((TimeZone.currentSystemDefault())).toEpochMilliseconds()
}

class DateMatcher(filteredDate: String?) {
    private val infinity = 10000
    private var fromYear by Delegates.notNull<Int>()
    private var toYear by Delegates.notNull<Int>()
    private var initialized by Delegates.notNull<Boolean>()

    init {
        initialized = filteredDate?.let { fDate ->
            when (fDate.length) {
                0 -> return@let true.also {
                    fromYear = -infinity
                    toYear = infinity
                }

                1, 2, 3, 6, 7, 8 -> return@let false
                4 -> if (fDate.all { it.isDigit() }) {
                    fromYear = fDate.toInt()
                    toYear = fromYear
                } else
                    return@let false

                5 -> if (fDate[4] == '-'
                    && fDate.slice(0..3).all { it.isDigit() }
                ) {
                    fromYear = fDate.slice(0..3).toInt()
                    toYear = infinity
                } else if (fDate[0] == '-'
                    && fDate.slice(1..4).all { it.isDigit() }
                ) {
                    fromYear = -infinity
                    toYear = fDate.slice(1..4).toInt()
                } else
                    return@let false

                9 -> if (fDate[4] == '-'
                    && fDate.slice(0..3).all { it.isDigit() }
                    && fDate.slice(5..8).all { it.isDigit() }
                ) {
                    fromYear = fDate.slice(0..3).toInt()
                    toYear = fDate.slice(5..8).toInt()
                } else
                    return@let false

                else -> return@let false
            }
            true
        } ?: true
    }

    fun matches(publicationDate: String?): Boolean {
        if (!initialized) return false
        if (fromYear == -infinity && toYear == infinity) return true
        publicationDate?.let { pDate ->
            if (pDate.isBlank()) return true
            val pYearString = pDate.substringBefore('-')
            if (pYearString.length != 4 || pYearString.any { !it.isDigit() })
                return false
            val pYear = pYearString.toInt()
            if (fromYear == infinity)
                return pYear <= toYear
            if (toYear == infinity)
                return pYear >= fromYear
            return (pYear in fromYear..toYear)
        }
        return true
    }
}
