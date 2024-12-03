package org.reactome.lit_ball.util

fun handleException(e: Exception) {
    Logger.error(e)
    throw e
}

class CantHappenException : Exception()