package org.reactome.lit_ball.common

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import kotlin.io.path.*

object SerialDB {
    private const val path = "db/map.json"
    private val Json = Json { prettyPrint = true }
    private lateinit var map : MutableMap<String,Query>

    fun open() {
        val dir = Path("db")
        if (!dir.isDirectory()) {
            try {
                dir.createDirectory()
            }
            catch (e: IOException) { Logger.error(e) }
        }
        val db = Path(path)
        var text = ""
        if (db.isReadable()) {
            try {
                text = File(path).readText()
            }
            catch (e: IOException) { Logger.error(e) }
        }
        map = try {
            Json.decodeFromString<MutableMap<String, Query>>(text)
        } catch (e: Exception) {
            mutableMapOf()
        }
    }
    fun get(): MutableMap<String, Query> {
        return map
    }
    fun commit() {
        val db = Path(path)
        if (db.isWritable()) {
            try {
                val text = Json.encodeToString(map)
                File(path).writeText(text)
            }
            catch (e: IOException) { Logger.error(e) }
        }
    }
    fun close() {}
}