package util

import common.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files

fun makeQueryDir(queryDir: File) : Boolean {
    if (queryDir.exists()) {
        handleException(IOException("Directory ${queryDir.absolutePath} already exists. Query not created."))
        return false
    }
    try {
        Files.createDirectory(queryDir.toPath())
    } catch (e: Exception) {
        handleException(e)
        return false
    }
    return true
}

suspend fun checkFileInDirectory(dir: File, fileType: FileType): File? {
    val file: File
    val canRead = withContext(Dispatchers.IO) { dir.isDirectory && dir.canRead() }
    if (canRead) {
        try {
            withContext(Dispatchers.IO) {
                file = File("${dir.absolutePath}/${fileType.fileName}")
            }
        } catch (e: Exception) {
            handleException(e)
            return null
        }

    } else {
        handleException(IOException("Cannot access directory ${dir.absolutePath}"))
        return null
    }
    return file
}
