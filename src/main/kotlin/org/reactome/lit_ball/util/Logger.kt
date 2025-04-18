package util

import io.github.oshai.kotlinlogging.KotlinLogging

object Logger {
    private val logger = KotlinLogging.logger {}
    fun error(e: Exception) {
        logger.error { e.message }
    }

    fun i(tag: String, s: String) {
        logger.info { "$tag: $s" }
    }
}
