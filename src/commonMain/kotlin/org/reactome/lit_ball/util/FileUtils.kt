package org.reactome.lit_ball.util

import org.reactome.lit_ball.common.FileType
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

fun checkFileInDirectory(dir: File, fileType: FileType): File? {
    val file: File
    if (dir.isDirectory && dir.canRead()) {
        try {
            file = File("${dir.absolutePath}/${fileType.fileName}")
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
