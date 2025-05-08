package util

import common.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Creates a directory for storing query files if it does not already exist.
 * Logs an error if the directory already exists or if an exception occurs during creation.
 *
 * @param queryDir The directory to be created for storing query files.
 * @return `true` if the directory was successfully created, `false` otherwise.
 */
fun makeQueryDir(queryDir: File): Boolean {
    return try {
        if (queryDir.exists()) {
            Logger.error(IOException("Directory ${queryDir.absolutePath} already exists. Query not created."))
            false
        } else {
            Files.createDirectories(queryDir.toPath())
            true
        }
    } catch (e: Exception) {
        Logger.error(e)
        false
    }
}

/**
 * Checks if a specified file type exists within a given directory while ensuring the directory is accessible.
 *
 * @param dir The directory to search for the file.
 * @param fileType The type of file to search for, as defined by the `FileType` enum.
 * @return The File object representing the specified file if it exists, or null if the directory is inaccessible
 *         or an exception occurs during the operation.
 */
suspend fun checkFileInDirectory(dir: File, fileType: FileType): File? {
    try {
        val canRead = withContext(Dispatchers.IO) { dir.isDirectory && dir.canRead() }
        if (!canRead) {
            handleException(IOException("Cannot access directory ${dir.absolutePath}"))
            return null
        }
        return File("${dir.absolutePath}/${fileType.fileName}")
    } catch (e: SecurityException) {
        handleException(e)
        return null
    } catch (_: IOException) {
        return null
    }
}

suspend fun <T> checkFileInDirectory(
    dir: File,
    fileType: FileType,
    fileOperation: (File) -> Result<T>,
    ): Result<T> {
    return runCatching {
        val file = File("${dir.absolutePath}/${fileType.fileName}")
        val result = withContext(Dispatchers.IO) { fileOperation(file) }
        result.onFailure { handleException(it) }
        result.getOrThrow()
    }
}

suspend fun writeFile(dir: File, fileType: FileType, text: String): Boolean {
    return withContext(Dispatchers.IO) {
        if (checkFileInDirectory(dir, fileType) == null) false
        try {
            File("${dir.absolutePath}/${FileType.ACCEPTED.fileName}")
                .writeText(text)
            true
        } catch (e: Exception) {
            Logger.error(e)
            false
        }
    }
}