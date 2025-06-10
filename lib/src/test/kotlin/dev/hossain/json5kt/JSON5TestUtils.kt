package dev.hossain.json5kt

/**
 * Test utilities for JSON5 parsing.
 * Contains helper functions for testing compatibility between the new type-safe API
 * and the legacy parseToAny behavior.
 */

/**
 * Converts a JSON5Value to a raw Any? object for testing compatibility.
 * This helper function maintains compatibility with the behavior of the deprecated parseToAny API.
 */
fun JSON5Value.toAny(): Any? =
    when (this) {
        is JSON5Value.Null -> null
        is JSON5Value.Boolean -> this.value
        is JSON5Value.String -> this.value
        is JSON5Value.Number.Integer -> this.value.toDouble() // Convert to Double for consistency with parseToAny
        is JSON5Value.Number.Decimal -> this.value
        is JSON5Value.Number.Hexadecimal -> this.value.toDouble() // Convert to Double for consistency
        is JSON5Value.Number.PositiveInfinity -> Double.POSITIVE_INFINITY
        is JSON5Value.Number.NegativeInfinity -> Double.NEGATIVE_INFINITY
        is JSON5Value.Number.NaN -> Double.NaN
        is JSON5Value.Object -> this.value.mapValues { it.value.toAny() }
        is JSON5Value.Array -> this.value.map { it.toAny() }
    }
