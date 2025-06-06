package io.github.json5.kotlin

/**
 * JSON5Parser is responsible for parsing JSON5 text into Kotlin objects.
 */
internal object JSON5Parser {
    /**
     * Parses a JSON5 string into a Kotlin object.
     *
     * @param text JSON5 text to parse
     * @return The parsed value (Map, List, String, Number, Boolean, or null)
     * @throws JSON5Exception if the input is invalid JSON5
     */
    fun parse(text: String): Any? {
        // Initially just throw an exception - we'll implement this through TDD
        throw NotImplementedError("JSON5 parser not yet implemented")
    }
}
