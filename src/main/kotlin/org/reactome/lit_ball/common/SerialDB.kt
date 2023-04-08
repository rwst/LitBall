package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import java.io.File
import java.io.IOException
import kotlin.io.path.*

val module = SerializersModule {
    polymorphic(SerialDBClass::class) {
        subclass(Query::class)
    }
}

val format = Json { serializersModule = module }

@Serializable
sealed class SerialDBClass

object SerialDB {
    private const val path = "db/map.json"
    private val Json = Json { prettyPrint = true }
    private lateinit var map : MutableMap<String,SerialDBClass>

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
            Json.decodeFromString<MutableMap<String,SerialDBClass>>(text)
        } catch (e: Exception) {
            mutableMapOf()
        }
    }
    fun get(): MutableMap<String,SerialDBClass> {
        return map
    }

    fun set(key: String, value: SerialDBClass) {
        map[key] = value
    }

    fun commit() {
        try {
            val text = Json.encodeToString(map)
            File(path).writeText(text)
        }
        catch (e: IOException) { Logger.error(e) }
    }
    fun close() {}
}