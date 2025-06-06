package io.github.json5.kotlin

/**
 * JSON5Serializer is responsible for serializing Kotlin objects to JSON5 text.
 */
internal object JSON5Serializer {
    /**
     * Serializes a Kotlin object to a JSON5 string.
     *
     * @param value The value to serialize
     * @param space Number of spaces for indentation or a string to use for indentation
     * @return The JSON5 string representation
     */
    fun stringify(value: Any?, space: Any? = null): String {
        // Initially just throw an exception - we'll implement this through TDD
        throw NotImplementedError("JSON5 serializer not yet implemented")
    }
}
