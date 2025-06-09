package io.github.json5.kotlin

import kotlinx.serialization.*

/**
 * JSON5 Implementation for Kotlin
 *
 * This is the main entry point for working with JSON5 data.
 */
object JSON5 {
    /**
     * Default JSON5 format for kotlinx.serialization.
     */
    private val format = DefaultJSON5Format

    /**
     * Parses a JSON5 string into a Kotlin object.
     *
     * @param text JSON5 text to parse
     * @return The parsed value (Map, List, String, Number, Boolean, or null)
     * @throws JSON5Exception if the input is invalid JSON5
     */
    fun parse(text: String): Any? {
        return JSON5Parser.parse(text)
    }

    /**
     * Parses a JSON5 string into a Kotlin object, with a reviver function.
     *
     * @param text JSON5 text to parse
     * @param reviver A function that transforms the parsed values
     * @return The parsed value (Map, List, String, Number, Boolean, or null)
     * @throws JSON5Exception if the input is invalid JSON5
     */
    fun parse(text: String, reviver: (key: String, value: Any?) -> Any?): Any? {
        return JSON5Parser.parse(text, reviver)
    }

    /**
     * Serializes a Kotlin object to a JSON5 string.
     *
     * @param value The value to serialize
     * @param space Number of spaces for indentation or a string to use for indentation
     * @return The JSON5 string representation
     */
    fun stringify(value: Any?, space: Any? = null): String {
        return JSON5Serializer.stringify(value, space)
    }

    /**
     * Encodes the given [value] to JSON5 string using kotlinx.serialization.
     *
     * @param serializer The serializer for type [T]
     * @param value The value to encode
     * @return JSON5 string representation
     */
    fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        return format.encodeToString(serializer, value)
    }

    /**
     * Decodes the given JSON5 [string] to a value of type [T] using kotlinx.serialization.
     *
     * @param deserializer The deserializer for type [T]
     * @param string JSON5 string to decode
     * @return Decoded value of type [T]
     */
    fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        return format.decodeFromString(deserializer, string)
    }
}
