package org.reactome.lit_ball.util

import java.text.SimpleDateFormat
import java.util.*

inline fun <reified T> Any?.tryCast(block: T.() -> Unit) {
    if (this is T) {
        block()
    }
}

inline fun <T : Any> MutableList<T>.replaceFirst(transformer: (T) -> T, block: (T) -> Boolean): MutableList<T> {
    val i = indexOfFirst { block(it) }
    this[i] = transformer(this[i])
    return this
}

fun formatDateToyyyyMMMddFormat(date: Date?): String {
    if (date == null) return "-.-"
    val format = SimpleDateFormat("yyyy-MMM-dd", Locale.ENGLISH)
    return format.format(date)
}

fun Once(action: () -> Unit): () -> Unit {
    var executed = false

    return {
        if (!executed) {
            action()
            executed = true
        }
    }
}