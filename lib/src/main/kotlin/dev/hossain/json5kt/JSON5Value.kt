package dev.hossain.json5kt

/**
 * Represents a JSON5 value as described in the JSON5 specification.
 * JSON5 values can be objects, arrays, strings, numbers, booleans, or null.
 *
 * @see <a href="https://spec.json5.org/">JSON5 Specification</a>
 */
sealed class JSON5Value {
    /**
     * Represents a JSON5 object, which is a collection of name/value pairs.
     *
     * @property value The map of property names to JSON5 values
     * @see <a href="https://spec.json5.org/#objects">JSON5 Objects</a>
     */
    data class Object(
        val value: Map<kotlin.String, JSON5Value>,
    ) : JSON5Value() {
        override fun toString(): kotlin.String = value.toString()
    }

    /**
     * Represents a JSON5 array, which is an ordered sequence of JSON5 values.
     *
     * @property value The list of JSON5 values
     * @see <a href="https://spec.json5.org/#arrays">JSON5 Arrays</a>
     */
    data class Array(
        val value: List<JSON5Value>,
    ) : JSON5Value() {
        override fun toString(): kotlin.String = value.toString()
    }

    /**
     * Represents a JSON5 string value.
     *
     * @property value The string value
     * @see <a href="https://spec.json5.org/#strings">JSON5 Strings</a>
     */
    data class String(
        val value: kotlin.String,
    ) : JSON5Value() {
        override fun toString(): kotlin.String = "\"$value\""
    }

    /**
     * Represents a JSON5 number value, which can be an integer, float, hex, infinity, or NaN.
     *
     * @see <a href="https://spec.json5.org/#numbers">JSON5 Numbers</a>
     */
    sealed class Number : JSON5Value() {
        /**
         * Represents an integer number in JSON5.
         *
         * @property value The integer value
         */
        data class Integer(
            val value: Long,
        ) : Number() {
            override fun toString(): kotlin.String = value.toString()
        }

        /**
         * Represents a decimal number in JSON5.
         *
         * @property value The decimal value
         */
        data class Decimal(
            val value: Double,
        ) : Number() {
            override fun toString(): kotlin.String = value.toString()
        }

        /**
         * Represents a hexadecimal number in JSON5 (0x prefix).
         *
         * @property value The integer value represented by the hex
         */
        data class Hexadecimal(
            val value: Long,
        ) : Number() {
            override fun toString(): kotlin.String = "0x${value.toString(16)}"
        }

        /**
         * Represents positive infinity in JSON5.
         */
        object PositiveInfinity : Number() {
            override fun toString(): kotlin.String = "Infinity"
        }

        /**
         * Represents negative infinity in JSON5.
         */
        object NegativeInfinity : Number() {
            override fun toString(): kotlin.String = "-Infinity"
        }

        /**
         * Represents NaN (Not a Number) in JSON5.
         */
        object NaN : Number() {
            override fun toString(): kotlin.String = "NaN"
        }
    }

    /**
     * Represents a JSON5 boolean value.
     *
     * @property value The boolean value
     * @see <a href="https://spec.json5.org/#primitives">JSON5 Primitives</a>
     */
    data class Boolean(
        val value: kotlin.Boolean,
    ) : JSON5Value() {
        override fun toString(): kotlin.String = value.toString()
    }

    /**
     * Represents a JSON5 null value.
     *
     * @see <a href="https://spec.json5.org/#primitives">JSON5 Primitives</a>
     */
    object Null : JSON5Value() {
        override fun toString(): kotlin.String = "null"
    }

    /**
     * Helper methods to convert primitive Kotlin types to JSON5Value objects.
     */
    companion object {
        /**
         * Creates a JSON5Value from a Kotlin object.
         *
         * @param value The Kotlin object to convert
         * @return The corresponding JSON5Value
         * @throws IllegalArgumentException if the value type is not supported
         */
        fun from(value: Any?): JSON5Value =
            when (value) {
                null -> Null
                is Map<*, *> -> {
                    val jsonMap = mutableMapOf<kotlin.String, JSON5Value>()
                    value.forEach { (k, v) ->
                        if (k is kotlin.String) {
                            jsonMap[k] = from(v)
                        }
                    }
                    Object(jsonMap)
                }
                is List<*> -> {
                    val jsonList = value.map { from(it) }
                    Array(jsonList)
                }
                is kotlin.String -> String(value)
                is Int -> Number.Integer(value.toLong())
                is Long -> Number.Integer(value)
                is Float -> Number.Decimal(value.toDouble())
                is Double -> {
                    when {
                        value.isNaN() -> Number.NaN
                        value.isInfinite() && value > 0 -> Number.PositiveInfinity
                        value.isInfinite() && value < 0 -> Number.NegativeInfinity
                        else -> Number.Decimal(value)
                    }
                }
                is kotlin.Boolean -> Boolean(value)
                else -> throw IllegalArgumentException("Unsupported type: ${value::class.java}")
            }
    }
}
