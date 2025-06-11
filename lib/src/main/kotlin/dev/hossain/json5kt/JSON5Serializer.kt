package dev.hossain.json5kt

/**
 * JSON5Serializer is responsible for serializing Kotlin objects to JSON5 text.
 *
 * **Performance Optimizations:**
 * - Fast path for simple strings that don't require escaping
 * - Pre-allocated StringBuilder with estimated capacity
 * - Efficient character handling in string serialization
 * - Pre-sized collections for object and array serialization
 *
 * @since 1.1.0 Performance improvements for faster JSON5 string generation
 */
internal object JSON5Serializer {
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
    ): String {
        val visitor = SerializerVisitor(space)
        return visitor.serializeValue(value)
    }

    private class SerializerVisitor(
        space: Any?,
    ) {
        private val stack = mutableListOf<Any>()
        private val gap: String

        init {
            gap =
                when (space) {
                    is Int -> " ".repeat(minOf(10, maxOf(0, space)))
                    is String -> space.substring(0, minOf(10, space.length))
                    else -> ""
                }
        }

        fun serializeValue(
            value: Any?,
            indent: String = "",
        ): String =
            when (value) {
                null -> "null"
                is Boolean -> value.toString()
                is Number -> serializeNumber(value)
                is String -> serializeString(value)
                is Map<*, *> -> serializeObject(value as Map<Any?, Any?>, indent)
                is List<*> -> serializeArray(value, indent)
                is Array<*> -> serializeArray(value.toList(), indent)
                else -> "null" // Unsupported types are serialized as null
            }

        private fun serializeNumber(value: Number): String =
            when (value) {
                is Double -> {
                    when {
                        value.isNaN() -> "NaN"
                        value == Double.POSITIVE_INFINITY -> "Infinity"
                        value == Double.NEGATIVE_INFINITY -> "-Infinity"
                        else -> value.toString()
                    }
                }
                is Float -> {
                    when {
                        value.isNaN() -> "NaN"
                        value == Float.POSITIVE_INFINITY -> "Infinity"
                        value == Float.NEGATIVE_INFINITY -> "-Infinity"
                        else -> value.toString()
                    }
                }
                else -> value.toString()
            }

        /**
         * Optimized string serialization with reduced allocations.
         * Pre-calculates required capacity and uses efficient character handling.
         */
        private fun serializeString(value: String): String {
            // Fast path for simple strings that don't need escaping
            if (value.none {
                    it < ' ' ||
                        it == '\\' ||
                        it == '\'' ||
                        it == '"' ||
                        it == '\b' ||
                        it == '\u000C' ||
                        it == '\n' ||
                        it == '\r' ||
                        it == '\t' ||
                        it == '\u000B' ||
                        it == '\u0000' ||
                        it == '\u2028' ||
                        it == '\u2029'
                }
            ) {
                val quote = if (value.contains('\'') && !value.contains('"')) '"' else '\''
                return "$quote$value$quote"
            }

            val quote = if (value.contains('\'') && !value.contains('"')) '"' else '\''
            // Pre-allocate with estimated capacity to reduce resizing
            val sb = StringBuilder(value.length + 10)
            sb.append(quote)

            for (char in value) {
                when (char) {
                    '\\' -> sb.append("\\\\")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\u000B' -> sb.append("\\v")
                    '\u0000' -> sb.append("\\0")
                    '\u2028' -> sb.append("\\u2028")
                    '\u2029' -> sb.append("\\u2029")
                    else -> {
                        when {
                            char == quote -> sb.append("\\").append(quote)
                            char < ' ' -> {
                                val hexString = char.code.toString(16)
                                sb.append("\\x")
                                if (hexString.length == 1) sb.append("0")
                                sb.append(hexString)
                            }
                            else -> sb.append(char)
                        }
                    }
                }
            }

            sb.append(quote)
            return sb.toString()
        }

        /**
         * Optimized object serialization with reduced allocations and faster property handling.
         * Performance improvements:
         * - Pre-sized ArrayList with capacity for better memory usage
         * - Optimized string building for properties
         * - Reduced intermediate string allocations
         */
        private fun serializeObject(
            obj: Map<Any?, Any?>,
            indent: String,
        ): String {
            if (obj.isEmpty()) return "{}"

            // Check for circular references
            if (stack.any { it === obj }) {
                throw JSON5Exception("Converting circular structure to JSON5", 0, 0)
            }

            stack.add(obj)

            val newIndent =
                if (gap.isNotEmpty()) {
                    indent + gap
                } else {
                    indent
                }

            // Pre-allocate list with exact size for better performance
            val properties = ArrayList<String>(obj.size)

            // Pre-calculate separators for efficiency
            val colonSeparator = if (gap.isNotEmpty()) ": " else ":"
            val linePrefix = if (gap.isNotEmpty()) newIndent else ""

            for ((key, value) in obj) {
                val keyStr = key.toString()
                val propName = serializePropertyName(keyStr)
                val propValue = serializeValue(value, newIndent)

                // Build property string more efficiently
                val property = if (gap.isNotEmpty()) {
                    "$linePrefix$propName$colonSeparator$propValue"
                } else {
                    "$propName$colonSeparator$propValue"
                }
                properties.add(property)
            }

            val joined = if (gap.isNotEmpty()) {
                properties.joinToString(",\n")
            } else {
                properties.joinToString(",")
            }

            stack.removeAt(stack.size - 1)

            return if (gap.isNotEmpty()) {
                "{\n$joined\n$indent}"
            } else {
                "{$joined}"
            }
        }

        private fun serializePropertyName(key: String): String {
            // If the key is a valid identifier, we can use it as is
            if (isValidIdentifier(key)) {
                return key
            }

            // Otherwise, we need to quote it
            return serializeString(key)
        }

        private fun isValidIdentifier(str: String): Boolean {
            if (str.isEmpty()) return false

            val firstChar = str[0]
            if (!(firstChar.isLetter() || firstChar == '_' || firstChar == '$')) {
                return false
            }

            for (i in 1 until str.length) {
                val ch = str[i]
                if (!(ch.isLetterOrDigit() || ch == '_' || ch == '$' || ch == '\u200C' || ch == '\u200D')) {
                    return false
                }
            }

            return true
        }

        /**
         * Optimized array serialization with reduced allocations.
         */
        private fun serializeArray(
            array: List<*>,
            indent: String,
        ): String {
            if (array.isEmpty()) return "[]"

            // Check for circular references
            if (stack.any { it === array }) {
                throw JSON5Exception("Converting circular structure to JSON5", 0, 0)
            }

            stack.add(array)

            val newIndent =
                if (gap.isNotEmpty()) {
                    indent + gap
                } else {
                    indent
                }

            // Pre-allocate list with known size for better performance
            val elements = ArrayList<String>(array.size)

            for (value in array) {
                val serialized = serializeValue(value, newIndent)
                val element =
                    if (gap.isNotEmpty()) {
                        "$newIndent$serialized"
                    } else {
                        serialized
                    }
                elements.add(element)
            }

            val joined =
                if (gap.isNotEmpty()) {
                    elements.joinToString(",\n")
                } else {
                    elements.joinToString(",")
                }

            stack.removeAt(stack.size - 1)

            return if (gap.isNotEmpty()) {
                "[\n$joined\n$indent]"
            } else {
                "[$joined]"
            }
        }
    }
}
