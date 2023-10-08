package org.reactome.lit_ball.util

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.text.SimpleDateFormat
import java.util.*

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