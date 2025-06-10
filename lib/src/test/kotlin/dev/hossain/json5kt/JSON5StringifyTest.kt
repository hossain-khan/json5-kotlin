package dev.hossain.json5kt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for the `JSON5.stringify()` method.
 * These tests verify that various Kotlin objects and values are correctly converted
 * into their JSON5 string representations, including handling of JSON5-specific
 * features like unquoted keys, single quotes for strings, and special numeric values.
 */
@DisplayName("JSON5.stringify")
class JSON5StringifyTest {
    /**
     * Tests stringifying an empty Kotlin Map.
     * Expected output is an empty JSON5 object: `{}`.
     */
    @Test
    fun `should stringify empty object`() {
        JSON5.stringify(mapOf<String, Any?>()) shouldBe "{}"
    }

    /**
     * Tests stringifying a simple Kotlin Map with a string value.
     * Expected output is a JSON5 object with an unquoted key and a single-quoted string value: `{key:'value'}`.
     */
    @Test
    fun `should stringify simple object with string value`() {
        JSON5.stringify(mapOf("key" to "value")) shouldBe "{key:'value'}"
    }

    /**
     * Tests stringifying a simple Kotlin Map with integer and floating-point number values.
     * Expected output uses unquoted keys and standard number representations: `{key:42}` and `{key:42.5}`.
     */
    @Test
    fun `should stringify simple object with number value`() {
        JSON5.stringify(mapOf("key" to 42)) shouldBe "{key:42}"
        JSON5.stringify(mapOf("key" to 42.5)) shouldBe "{key:42.5}"
    }

    /**
     * Tests stringifying a simple Kotlin Map with a boolean value.
     * Expected output uses an unquoted key and the literal `true`: `{key:true}`.
     */
    @Test
    fun `should stringify simple object with boolean value`() {
        JSON5.stringify(mapOf("key" to true)) shouldBe "{key:true}"
    }

    /**
     * Tests stringifying a simple Kotlin Map with a null value.
     * Expected output uses an unquoted key and the literal `null`: `{key:null}`.
     */
    @Test
    fun `should stringify simple object with null value`() {
        JSON5.stringify(mapOf("key" to null)) shouldBe "{key:null}"
    }

    /**
     * Tests stringifying an empty Kotlin List.
     * Expected output is an empty JSON5 array: `[]`.
     */
    @Test
    fun `should stringify empty array`() {
        JSON5.stringify(emptyList<Any?>()) shouldBe "[]"
    }

    /**
     * Tests stringifying a Kotlin List with various primitive values.
     * Expected output is a JSON5 array with numbers, a single-quoted string, and literals: `[1,'string',true,null]`.
     */
    @Test
    fun `should stringify array with values`() {
        JSON5.stringify(listOf(1, "string", true, null)) shouldBe "[1,'string',true,null]"
    }

    /**
     * Tests stringifying a Kotlin Map containing nested objects and arrays.
     * Verifies that complex structures are correctly represented in JSON5.
     * Expected: `{object:{key:'value'},array:[1,2,3]}`.
     */
    @Test
    fun `should stringify nested objects and arrays`() {
        val nested =
            mapOf(
                "object" to mapOf("key" to "value"),
                "array" to listOf(1, 2, 3),
            )
        JSON5.stringify(nested) shouldBe "{object:{key:'value'},array:[1,2,3]}"
    }

    /**
     * Tests stringifying a Kotlin Map where keys are not valid ECMAScript 5.1 identifiers
     * (e.g., contain hyphens or spaces).
     * Expected output encloses such keys in single quotes: `{'special-key':1,' ':2}`.
     */
    @Test
    fun `should stringify object with non-identifier keys`() {
        val obj = mapOf("special-key" to 1, " " to 2)
        JSON5.stringify(obj) shouldBe "{'special-key':1,' ':2}"
    }

    /**
     * Tests stringifying special numeric values: `Infinity`, `-Infinity`, and `NaN`.
     * JSON5 allows these literals directly.
     * Expected outputs: `"Infinity"`, `"-Infinity"`, `"NaN"`.
     */
    @Test
    fun `should stringify special number values`() {
        JSON5.stringify(Double.POSITIVE_INFINITY) shouldBe "Infinity"
        JSON5.stringify(Double.NEGATIVE_INFINITY) shouldBe "-Infinity"
        JSON5.stringify(Double.NaN) shouldBe "NaN"
    }

    /**
     * Tests that attempting to stringify an object with circular references throws a [JSON5Exception].
     * Circular references cannot be represented in JSON or JSON5.
     */
    @Test
    fun `should throw on circular references`() {
        val circular = mutableMapOf<String, Any?>()
        circular["self"] = circular

        shouldThrow<JSON5Exception> {
            JSON5.stringify(circular)
        }
    }

    /**
     * Tests stringifying a Kotlin Map with indentation.
     * When a `space` argument (number of spaces) is provided, the output JSON5 string should be pretty-printed.
     * Expected output for `mapOf("key" to "value")` with `space = 2`: `{\n  key: 'value'\n}`.
     */
    @Test
    fun `should stringify with indentation when space is provided`() {
        JSON5.stringify(mapOf("key" to "value"), space = 2) shouldBe "{\n  key: 'value'\n}"
    }
}
