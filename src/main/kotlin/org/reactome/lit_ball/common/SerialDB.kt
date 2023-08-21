package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.reactome.lit_ball.util.Logger
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable

//val module = SerializersModule {
//    polymorphic(SerialDBClass::class) {
//        subclass(Query::class)
//    }
//}
//
//val format = Json { serializersModule = module }

@Serializable
sealed class SerialDBClass

object SerialDB {
    private const val PATH = "db/map.json"
    private val Json = Json { prettyPrint = true }
    private lateinit var map: MutableMap<String, SerialDBClass>

    fun open() {
        val dir = Path("db")
        if (!dir.isDirectory()) {
            try {
                dir.createDirectory()
            } catch (e: IOException) {
                Logger.error(e)
            }
        }
        val db = Path(PATH)
        var text = ""
        if (db.isReadable()) {
            try {
                text = File(PATH).readText()
            } catch (e: IOException) {
                Logger.error(e)
            }
        }
        map = try {
            Json.decodeFromString<MutableMap<String, SerialDBClass>>(text)
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun get(): MutableMap<String, SerialDBClass> {
        return map
    }

    fun set(key: String, value: SerialDBClass) {
        map[key] = value
    }

    fun commit() {
        try {
            val text = Json.encodeToString(map)
            File(PATH).writeText(text)
        } catch (e: IOException) {
            Logger.error(e)
        }
    }

    fun close() {}
}