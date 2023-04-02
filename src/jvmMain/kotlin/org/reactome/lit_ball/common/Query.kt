package org.reactome.lit_ball.common

import kotlinx.serialization.Serializable

@Serializable
data class Query(val x: Int)

//@OptIn(ExperimentalSerializationApi::class)
//@Serializer
//object QuerySerializer : Serializer() {
//    val defaultValue = Query(0)
//
//    override suspend fun readFrom(input: InputStream): UserPreferences {
//        try {
//            return Json.decodeFromString(
//                UserPreferences.serializer(), input.readBytes().decodeToString()
//            )
//        } catch (serialization: SerializationException) {
//            throw CorruptionException("Unable to read UserPrefs", serialization)
//        }
//    }
//
//    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
//        output.write(
//            Json.encodeToString(UserPreferences.serializer(), t)
//                .encodeToByteArray()
//        )
//    }
//}