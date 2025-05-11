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

/**
 * Writes the specified text to a file of the given type within the provided directory.
 *
 * The method attempts to locate the specified file type in the directory using the `checkFileInDirectory` function.
 * If the file exists, it writes the text to the file asynchronously on an IO coroutine dispatcher.
 * If an error occurs during the file operation, it logs the exception and returns false.
 *
 * @param dir The directory where the file will be written.
 * @param fileType The type of file to be written, represented by the `FileType` enum.
 * @param text The text content to write into the file.
 * @return A boolean indicating whether the write operation was successful. Returns true if the file is successfully
 *         written, false otherwise.
 */
suspend fun writeFile(dir: File, fileType: FileType, text: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            if (!dir.isDirectory) false
            if (!dir.canRead()) false
            File("${dir.absolutePath}/${fileType.fileName}")
                .writeText(text)
            true
        } catch (e: Exception) {
            Logger.error(e)
            false
        }
    }
}

/**
 * Creates a file path by combining a directory path and a file name.
 *
 * @param dir The directory where the file is located.
 * @param fileType The type of file, represented by the `FileType` enum.
 * @return A File object representing the file path.
 */
fun createFilePath(dir: File, fileType: FileType): File {
    return File("${dir.absolutePath}/${fileType.fileName}")
}

/**
 * Writes the specified text to a file of the given type within the provided directory.
 * This is a non-suspend version of writeFile that can be used in non-suspend functions.
 *
 * @param dir The directory where the file will be written.
 * @param fileType The type of file to be written, represented by the `FileType` enum.
 * @param text The text content to write into the file.
 * @return A boolean indicating whether the write operation was successful.
 */
fun writeFileSync(dir: File, fileType: FileType, text: String): Boolean {
    return try {
        if (!dir.isDirectory) return false
        if (!dir.canRead()) return false
        createFilePath(dir, fileType).writeText(text)
        true
    } catch (e: Exception) {
        Logger.error(e)
        false
    }
}

/**
 * Appends the specified text to a file of the given type within the provided directory.
 * This is a non-suspend version of appendToFile that can be used in non-suspend functions.
 *
 * @param dir The directory where the file is located.
 * @param fileType The type of file to be appended to, represented by the `FileType` enum.
 * @param text The text content to append to the file.
 * @return A boolean indicating whether the append operation was successful.
 */
fun appendToFileSync(dir: File, fileType: FileType, text: String): Boolean {
    return try {
        if (!dir.isDirectory) return false
        if (!dir.canRead()) return false
        createFilePath(dir, fileType).appendText(text)
        true
    } catch (e: Exception) {
        Logger.error(e)
        false
    }
}
