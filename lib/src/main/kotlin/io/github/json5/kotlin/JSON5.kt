package io.github.json5.kotlin

/**
 * JSON5 Implementation for Kotlin
 *
 * This is the main entry point for working with JSON5 data.
 */
object JSON5 {
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
}
