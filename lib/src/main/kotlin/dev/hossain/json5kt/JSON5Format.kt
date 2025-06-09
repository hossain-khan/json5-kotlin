package dev.hossain.json5kt

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

/**
 * JSON5 serialization format for kotlinx.serialization.
 * 
 * This format allows encoding and decoding of @Serializable classes to/from JSON5 format.
 * It builds on top of the existing JSON5Parser and JSON5Serializer implementations.
 */
@OptIn(ExperimentalSerializationApi::class)
class JSON5Format(
    private val configuration: JSON5Configuration = JSON5Configuration.Default
) : StringFormat {
    
    override val serializersModule: SerializersModule = EmptySerializersModule()

    /**
     * Encodes the given [value] to JSON5 string using the serializer retrieved from reified type parameter.
     */
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val jsonElement = JSON5Encoder(configuration).encodeToJsonElement(serializer, value)
        return jsonElementToJson5String(jsonElement)
    }

    /**
     * Decodes the given JSON5 [string] to a value of type [T] using the given [deserializer].
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val jsonElement = json5StringToJsonElement(string)
        return JSON5Decoder(configuration, jsonElement).decodeSerializableValue(deserializer)
    }

    private fun jsonElementToJson5String(element: JsonElement): String {
        // Convert JsonElement to a regular Kotlin object that JSON5Serializer can handle
        val kotlinObject = jsonElementToKotlinObject(element)
        return JSON5Serializer.stringify(kotlinObject, configuration.prettyPrint)
    }

    private fun json5StringToJsonElement(string: String): JsonElement {
        // Parse JSON5 string to Kotlin object, then convert to JsonElement
        val kotlinObject = JSON5Parser.parse(string)
        return kotlinObjectToJsonElement(kotlinObject)
    }

    private fun jsonElementToKotlinObject(element: JsonElement): Any? {
        return when (element) {
            is JsonNull -> null
            is JsonPrimitive -> when {
                element.isString -> element.content
                element.content == "true" -> true
                element.content == "false" -> false
                else -> {
                    // Try to preserve the original numeric type
                    val content = element.content
                    // Handle scientific notation that should be parsed as Long
                    if (content.contains('E') || content.contains('e')) {
                        val doubleValue = content.toDoubleOrNull()
                        if (doubleValue != null && doubleValue.isFinite() && doubleValue % 1.0 == 0.0) {
                            when {
                                doubleValue >= Int.MIN_VALUE && doubleValue <= Int.MAX_VALUE -> doubleValue.toInt()
                                doubleValue >= Long.MIN_VALUE && doubleValue <= Long.MAX_VALUE -> doubleValue.toLong()
                                else -> doubleValue
                            }
                        } else {
                            doubleValue ?: content
                        }
                    } else {
                        content.toIntOrNull() 
                            ?: content.toLongOrNull() 
                            ?: content.toDoubleOrNull() 
                            ?: content
                    }
                }
            }
            is JsonObject -> element.mapValues { jsonElementToKotlinObject(it.value) }
            is JsonArray -> element.map { jsonElementToKotlinObject(it) }
        }
    }

    private fun kotlinObjectToJsonElement(obj: Any?): JsonElement {
        return when (obj) {
            null -> JsonNull
            is Boolean -> JsonPrimitive(obj)
            is Int -> JsonPrimitive(obj)
            is Long -> JsonPrimitive(obj)
            is Double -> {
                // If the double is actually a whole number, try to represent it as int or long if it fits
                if (obj.isFinite() && obj % 1.0 == 0.0) {
                    when {
                        obj >= Int.MIN_VALUE && obj <= Int.MAX_VALUE -> JsonPrimitive(obj.toInt())
                        obj >= Long.MIN_VALUE && obj <= Long.MAX_VALUE -> JsonPrimitive(obj.toLong())
                        else -> JsonPrimitive(obj)
                    }
                } else {
                    JsonPrimitive(obj)
                }
            }
            is Float -> {
                // Similar handling for float
                if (obj.isFinite() && obj % 1.0f == 0.0f) {
                    when {
                        obj >= Int.MIN_VALUE && obj <= Int.MAX_VALUE -> JsonPrimitive(obj.toInt())
                        obj >= Long.MIN_VALUE && obj <= Long.MAX_VALUE -> JsonPrimitive(obj.toLong())
                        else -> JsonPrimitive(obj)
                    }
                } else {
                    JsonPrimitive(obj)
                }
            }
            is Number -> JsonPrimitive(obj)
            is String -> JsonPrimitive(obj)
            is Map<*, *> -> {
                val jsonObject = mutableMapOf<String, JsonElement>()
                @Suppress("UNCHECKED_CAST")
                val map = obj as Map<String, Any?>
                for ((key, value) in map) {
                    jsonObject[key] = kotlinObjectToJsonElement(value)
                }
                JsonObject(jsonObject)
            }
            is List<*> -> {
                JsonArray(obj.map { kotlinObjectToJsonElement(it) })
            }
            else -> JsonPrimitive(obj.toString())
        }
    }
}

/**
 * Configuration for JSON5 serialization.
 */
data class JSON5Configuration(
    val prettyPrint: Boolean = false,
    val prettyPrintIndent: String = "  "
) {
    companion object {
        val Default = JSON5Configuration()
    }
}

/**
 * Encoder implementation that uses kotlinx.serialization's JSON encoder as a bridge.
 */
private class JSON5Encoder(private val configuration: JSON5Configuration) {
    private val json = Json { 
        encodeDefaults = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    fun <T> encodeToJsonElement(serializer: SerializationStrategy<T>, value: T): JsonElement {
        return json.encodeToJsonElement(serializer, value)
    }
}

/**
 * Decoder implementation that uses kotlinx.serialization's JSON decoder as a bridge.
 */
private class JSON5Decoder(
    private val configuration: JSON5Configuration,
    private val element: JsonElement
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return json.decodeFromJsonElement(deserializer, element)
    }
}

/**
 * Default JSON5 format instance.
 */
val DefaultJSON5Format = JSON5Format()