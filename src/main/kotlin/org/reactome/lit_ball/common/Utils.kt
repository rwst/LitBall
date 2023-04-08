package org.reactome.lit_ball.common

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
