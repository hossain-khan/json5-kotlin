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

    /**
     * Tests stringifying special Float values: Infinity, -Infinity, and NaN.
     * JSON5 allows these literals directly for Float types as well.
     */
    @Test
    fun `should stringify Float special values`() {
        JSON5.stringify(Float.POSITIVE_INFINITY) shouldBe "Infinity"
        JSON5.stringify(Float.NEGATIVE_INFINITY) shouldBe "-Infinity"
        JSON5.stringify(Float.NaN) shouldBe "NaN"
    }

    /**
     * Tests stringifying with a string-based indent instead of a number.
     * When a string is passed as the `space` argument, it is used directly for indentation.
     */
    @Test
    fun `should stringify with string indentation`() {
        JSON5.stringify(mapOf("key" to "value"), space = "\t") shouldBe "{\n\tkey: 'value'\n}"
    }

    /**
     * Tests that space values greater than 10 are capped at 10 spaces.
     * JSON5 spec limits indentation to a maximum of 10 characters.
     */
    @Test
    fun `should cap indentation at 10 spaces`() {
        val result = JSON5.stringify(mapOf("k" to "v"), space = 15)
        result shouldBe "{\n          k: 'v'\n}" // 10 spaces max
    }

    /**
     * Tests that string indentation is capped at 10 characters.
     * JSON5 spec limits indentation to a maximum of 10 characters.
     */
    @Test
    fun `should cap string indentation at 10 characters`() {
        val result = JSON5.stringify(mapOf("k" to "v"), space = "123456789012345")
        result shouldBe "{\n1234567890k: 'v'\n}" // First 10 chars only
    }

    /**
     * Tests stringifying arrays with indentation enabled.
     */
    @Test
    fun `should stringify arrays with indentation`() {
        val result = JSON5.stringify(listOf(1, 2, 3), space = 2)
        result shouldBe "[\n  1,\n  2,\n  3\n]"
    }

    /**
     * Tests stringifying nested structures with indentation.
     */
    @Test
    fun `should stringify nested structures with indentation`() {
        val nested = mapOf("obj" to mapOf("key" to "value"), "arr" to listOf(1))
        val result = JSON5.stringify(nested, space = 2)
        result shouldBe "{\n  obj: {\n    key: 'value'\n  },\n  arr: [\n    1\n  ]\n}"
    }

    /**
     * Tests that circular references in arrays throw an error.
     */
    @Test
    fun `should throw on circular array references`() {
        val circular = mutableListOf<Any?>()
        circular.add(circular)

        shouldThrow<JSON5Exception> {
            JSON5.stringify(circular)
        }
    }

    /**
     * Tests stringifying strings that contain special characters requiring escaping.
     */
    @Test
    fun `should stringify strings with escape characters`() {
        JSON5.stringify("\b") shouldBe "'\\b'"
        JSON5.stringify("\u000C") shouldBe "'\\f'"
        JSON5.stringify("\n") shouldBe "'\\n'"
        JSON5.stringify("\r") shouldBe "'\\r'"
        JSON5.stringify("\t") shouldBe "'\\t'"
        JSON5.stringify("\u000B") shouldBe "'\\v'"
        JSON5.stringify("\u0000") shouldBe "'\\0'"
        JSON5.stringify("\\") shouldBe "'\\\\'"
    }

    /**
     * Tests stringifying strings with Unicode line/paragraph separators.
     */
    @Test
    fun `should stringify strings with line separators`() {
        JSON5.stringify("\u2028") shouldBe "'\\u2028'"
        JSON5.stringify("\u2029") shouldBe "'\\u2029'"
    }

    /**
     * Tests stringifying strings with control characters.
     */
    @Test
    fun `should stringify strings with control characters`() {
        JSON5.stringify("\u0001") shouldBe "'\\x01'"
        JSON5.stringify("\u001F") shouldBe "'\\x1f'"
    }

    /**
     * Tests stringifying strings with quotes - prefers single quotes unless the string contains single quotes.
     */
    @Test
    fun `should use double quotes when string contains single quotes`() {
        JSON5.stringify("it's") shouldBe "\"it's\""
    }

    /**
     * Tests stringifying strings containing both quote types.
     */
    @Test
    fun `should escape single quotes when string contains both quote types`() {
        JSON5.stringify("it's a \"test\"") shouldBe "'it\\'s a \"test\"'"
    }

    /**
     * Tests stringifying Kotlin Array objects (not just Lists).
     */
    @Test
    fun `should stringify Kotlin arrays`() {
        JSON5.stringify(arrayOf(1, 2, 3)) shouldBe "[1,2,3]"
    }

    /**
     * Tests that unsupported types are serialized as null.
     */
    @Test
    fun `should serialize unsupported types as null`() {
        JSON5.stringify(Thread.currentThread()) shouldBe "null"
    }

    /**
     * Tests stringifying with negative space value (should be treated as 0).
     */
    @Test
    fun `should handle negative space value`() {
        JSON5.stringify(mapOf("key" to "value"), space = -5) shouldBe "{key:'value'}"
    }

    /**
     * Tests stringifying Long values.
     */
    @Test
    fun `should stringify Long values`() {
        JSON5.stringify(mapOf("key" to 9223372036854775807L)) shouldBe "{key:9223372036854775807}"
    }

    /**
     * Tests stringifying regular Float values (not special values).
     */
    @Test
    fun `should stringify regular Float values`() {
        JSON5.stringify(mapOf("key" to 3.14f)) shouldBe "{key:3.14}"
    }
}
