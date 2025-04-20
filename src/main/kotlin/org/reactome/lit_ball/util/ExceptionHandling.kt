package util

fun handleException(e: Exception) {
    Logger.error(e)
    throw e
}

fun handleException(t: Throwable) {
    Logger.error(t)
    throw t
}

class CantHappenException : Exception()