package org.reactome.lit_ball.util

import org.reactome.lit_ball.common.Logger

fun handleException(e: Exception) {
    Logger.error(e)
    throw e
}

class CantHappenException : Exception()