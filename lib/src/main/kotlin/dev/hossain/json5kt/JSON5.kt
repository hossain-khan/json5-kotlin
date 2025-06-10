package dev.hossain.json5kt

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

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
     * Parses a JSON5 string into a strongly-typed JSON5Value.
     *
     * @param text JSON5 text to parse
     * @return The parsed value as a JSON5Value
     * @throws JSON5Exception if the input is invalid JSON5
     */
    fun parse(text: String): JSON5Value {
        val result = JSON5Parser.parse(text)
        return JSON5Value.from(result)
    }

    /**
     * Parses a JSON5 string into a strongly-typed JSON5Value, with a reviver function.
     *
     * @param text JSON5 text to parse
     * @param reviver A function that transforms the parsed values
     * @return The parsed value as a JSON5Value
     * @throws JSON5Exception if the input is invalid JSON5
     */
    fun parse(
        text: String,
        reviver: (key: String, value: Any?) -> Any?,
    ): JSON5Value {
        val result = JSON5Parser.parse(text, reviver)
        return JSON5Value.from(result)
    }

    /**
     * Serializes a Kotlin object to a JSON5 string.
     *
     * @param value The value to serialize
     * @param space Number of spaces for indentation or a string to use for indentation
     * @return The JSON5 string representation
     */
    fun stringify(
        value: Any?,
        space: Any? = null,
    ): String = JSON5Serializer.stringify(value, space)

    /**
     * Encodes the given [value] to JSON5 string using kotlinx.serialization.
     *
     * @param serializer The serializer for type [T]
     * @param value The value to encode
     * @return JSON5 string representation
     *
     * ```kotlin
     * @Serializable
     * data class Config(val name: String, val version: Int)
     *
     * val config = Config("MyApp", 1)
     * val json5 = JSON5.encodeToString(Config.serializer(), config)
     * // Result: {name:'MyApp',version:1}
     * ```
     */
    fun <T> encodeToString(
        serializer: SerializationStrategy<T>,
        value: T,
    ): String = format.encodeToString(serializer, value)

    /**
     * Decodes the given JSON5 [string] to a value of type [T] using kotlinx.serialization.
     *
     * @param deserializer The deserializer for type [T]
     * @param string JSON5 string to decode
     * @return Decoded value of type [T]
     *
     * ```kotlin
     * @Serializable
     * data class Config(val name: String, val version: Int)
     *
     * val json5 = """
     *     {
     *         // Application name
     *         name: 'MyApp',
     *         version: 1, // current version
     *     }
     * """.trimIndent()
     * val config = JSON5.decodeFromString(Config.serializer(), json5)
     * // Result: Config(name="MyApp", version=1)
     * ```
     */
    fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        string: String,
    ): T = format.decodeFromString(deserializer, string)
}
