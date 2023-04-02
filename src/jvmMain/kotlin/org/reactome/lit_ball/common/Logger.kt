package org.reactome.lit_ball.common

import io.github.oshai.KotlinLogging

object Logger {
    private val logger = KotlinLogging.logger {}
    fun error(e: Exception) { logger.error(e.message) }
}