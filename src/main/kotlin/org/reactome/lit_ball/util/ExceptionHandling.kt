package util

fun handleException(e: Exception) {
    Logger.error(e)
    throw e
}

class CantHappenException : Exception()