package org.reactome.lit_ball.util

import kotlinx.serialization.SerializationException
import org.reactome.lit_ball.common.Logger
import org.reactome.lit_ball.common.Settings
import java.io.IOException

fun handleException(e: Exception) {
    when (e) {
        is IOException, is SerializationException -> {
            Logger.error(e)
            Settings.map = mutableMapOf()
        }

        else -> throw e
    }
}

