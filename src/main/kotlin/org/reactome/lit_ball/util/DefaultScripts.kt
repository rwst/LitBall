package util

import dev.dirs.ProjectDirectories
import org.reactome.lit_ball.util.DefaultScriptsData
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

object DefaultScripts {
    private val myProjDirs: ProjectDirectories = ProjectDirectories.from("org", "reactome", "LitBall")
    private val configDir: String = myProjDirs.configDir

    private fun byteArrayToSHAString(byteArray: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return BigInteger(1, md.digest(byteArray)).toString(16).padStart(64, '0')
    }

    fun install() {
        val configDirectory = File(configDir)
        if (!configDirectory.exists()) {
            if (!configDirectory.mkdir())
                throw IllegalArgumentException("Directory could not be created: $configDir")
        }
        val scriptDirectory = File("$configDir/scripts")
        if (!scriptDirectory.exists()) {
            if (!scriptDirectory.mkdir())
                throw IllegalArgumentException("Directory could not be created: $configDir/scripts")
        }
        val fileSHAMap = scriptDirectory
            .walk()
            .filter { it.isFile }
            .associate { Pair(it.name, Pair(it, byteArrayToSHAString(it.readBytes()))) }
            .toMap()
        DefaultScriptsData.scriptMap.forEach { (k, v) ->
            val text = v.replace("longstringdelimiterreplacement", "\"\"\"")
            val sha = byteArrayToSHAString(text.toByteArray())
            val pair = fileSHAMap[k]
            if (pair != null && pair.second == sha) return@forEach
            val newFile: File = pair?.first ?: File("$configDir/scripts/$k")
            newFile.writeText(v)
        }
    }
}