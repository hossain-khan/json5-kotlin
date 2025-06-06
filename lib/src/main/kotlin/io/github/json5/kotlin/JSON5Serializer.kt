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
        val visitor = SerializerVisitor(space)
        return visitor.serializeValue(value)
    }

    private class SerializerVisitor(space: Any?) {
        private val stack = mutableListOf<Any>()
        private val gap: String

        init {
            gap = when (space) {
                is Int -> " ".repeat(minOf(10, maxOf(0, space)))
                is String -> space.substring(0, minOf(10, space.length))
                else -> ""
            }
        }

        fun serializeValue(value: Any?, indent: String = ""): String {
            return when (value) {
                null -> "null"
                is Boolean -> value.toString()
                is Number -> serializeNumber(value)
                is String -> serializeString(value)
                is Map<*, *> -> serializeObject(value as Map<Any?, Any?>, indent)
                is List<*> -> serializeArray(value, indent)
                is Array<*> -> serializeArray(value.toList(), indent)
                else -> "null" // Unsupported types are serialized as null
            }
        }

        private fun serializeNumber(value: Number): String {
            return when(value) {
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
        }

        private fun serializeString(value: String): String {
            val sb = StringBuilder()
            val quote = if (value.contains('\'') && !value.contains('"')) '"' else '\''

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
                                sb.append("\\x").append("0".repeat(2 - hexString.length)).append(hexString)
                            }
                            else -> sb.append(char)
                        }
                    }
                }
            }

            sb.append(quote)
            return sb.toString()
        }

        private fun serializeObject(obj: Map<Any?, Any?>, indent: String): String {
            if (obj.isEmpty()) return "{}"

            // Check for circular references
            if (stack.any { it === obj }) {
                throw JSON5Exception("Converting circular structure to JSON5")
            }

            stack.add(obj)

            val newIndent = if (gap.isNotEmpty()) {
                indent + gap
            } else {
                indent
            }

            val properties = obj.entries.map { (key, value) ->
                val keyStr = key.toString()
                val propName = serializePropertyName(keyStr)
                val propValue = serializeValue(value, newIndent)

                if (gap.isNotEmpty()) {
                    // This is the fix: Use exactly one space after the colon when formatting
                    "$newIndent$propName: $propValue"
                } else {
                    "$propName:$propValue"
                }
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

        private fun serializeArray(array: List<*>, indent: String): String {
            if (array.isEmpty()) return "[]"

            // Check for circular references
            if (stack.any { it === array }) {
                throw JSON5Exception("Converting circular structure to JSON5")
            }

            stack.add(array)

            val newIndent = if (gap.isNotEmpty()) {
                indent + gap
            } else {
                indent
            }

            val elements = array.map { value ->
                val serialized = serializeValue(value, newIndent)
                if (gap.isNotEmpty()) {
                    "$newIndent$serialized"
                } else {
                    serialized
                }
            }

            val joined = if (gap.isNotEmpty()) {
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
