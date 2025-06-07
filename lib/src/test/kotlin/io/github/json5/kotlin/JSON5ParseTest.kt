package io.github.json5.kotlin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.Double.Companion.NaN
import kotlin.test.assertTrue

@DisplayName("JSON5.parse")
class JSON5ParseTest {

    @Test
    fun `should parse empty object`() {
        JSON5.parse("{}") shouldBe emptyMap<String, Any?>()
    }

    @Test
    fun `should parse simple object with string value`() {
        JSON5.parse("""{"key": "value"}""") shouldBe mapOf("key" to "value")
    }

    @Test
    fun `should parse simple object with number value`() {
        JSON5.parse("""{"key": 42}""") shouldBe mapOf("key" to 42.0)
    }

    @Test
    fun `should parse simple object with boolean value`() {
        JSON5.parse("""{"key": true}""") shouldBe mapOf("key" to true)
    }

    @Test
    fun `should parse simple object with null value`() {
        JSON5.parse("""{"key": null}""") shouldBe mapOf("key" to null)
    }

    @Test
    fun `should parse empty array`() {
        JSON5.parse("[]") shouldBe emptyList<Any?>()
    }

    @Test
    fun `should parse array with values`() {
        JSON5.parse("""[1, "string", true, null]""") shouldBe listOf(1.0, "string", true, null)
    }

    // Additional object tests

    @Test
    fun `should parse double quoted string property names`() {
        JSON5.parse("""{"a":1}""") shouldBe mapOf("a" to 1.0)
    }

    @Test
    fun `should parse single quoted string property names`() {
        JSON5.parse("""{'a':1}""") shouldBe mapOf("a" to 1.0)
    }

    @Test
    fun `should parse unquoted property names`() {
        JSON5.parse("""{a:1}""") shouldBe mapOf("a" to 1.0)
    }

    @Test
    fun `should parse special character property names`() {
        JSON5.parse("""{\$\_:1,_$:2,a\u200C:3}""") shouldBe mapOf("\$_" to 1.0, "_$" to 2.0, "a\u200C" to 3.0)
    }

    @Test
    fun `should parse unicode property names`() {
        JSON5.parse("""{ùńîċõďë:9}""") shouldBe mapOf("ùńîċõďë" to 9.0)
    }

    @Test
    fun `should parse escaped property names`() {
        JSON5.parse("""{\\u0061\\u0062:1,\\u0024\\u005F:2,\\u005F\\u0024:3}""") shouldBe mapOf("ab" to 1.0, "\$_" to 2.0, "_$" to 3.0)
    }

    @Test
    fun `should preserve __proto__ property names`() {
        val result = JSON5.parse("""{"__proto__":1}""") as Map<*, *>
        result["__proto__"] shouldBe 1.0
    }

    @Test
    fun `should parse multiple properties`() {
        JSON5.parse("""{abc:1,def:2}""") shouldBe mapOf("abc" to 1.0, "def" to 2.0)
    }

    @Test
    fun `should parse nested objects`() {
        JSON5.parse("""{a:{b:2}}""") shouldBe mapOf("a" to mapOf("b" to 2.0))
    }

    // Additional array tests

    @Test
    fun `should parse multiple array values`() {
        JSON5.parse("[1,2]") shouldBe listOf(1.0, 2.0)
    }

    @Test
    fun `should parse nested arrays`() {
        JSON5.parse("[1,[2,3]]") shouldBe listOf(1.0, listOf(2.0, 3.0))
    }

    // Number tests

    @Test
    fun `should parse leading zeroes`() {
        JSON5.parse("[0,0.,0e0]") shouldBe listOf(0.0, 0.0, 0.0)
    }

    @Test
    fun `should parse integers`() {
        JSON5.parse("[1,23,456,7890]") shouldBe listOf(1.0, 23.0, 456.0, 7890.0)
    }

    @Test
    fun `should parse signed numbers`() {
        JSON5.parse("[-1,+2,-.1,-0]") shouldBe listOf(-1.0, 2.0, -0.1, -0.0)
    }

    @Test
    fun `should parse leading decimal points`() {
        JSON5.parse("[.1,.23]") shouldBe listOf(0.1, 0.23)
    }

    @Test
    fun `should parse fractional numbers`() {
        JSON5.parse("[1.0,1.23]") shouldBe listOf(1.0, 1.23)
    }

    @Test
    fun `should parse exponents`() {
        JSON5.parse("[1e0,1e1,1e01,1.e0,1.1e0,1e-1,1e+1]") shouldBe
            listOf(1.0, 10.0, 10.0, 1.0, 1.1, 0.1, 10.0)
    }

    @Test
    fun `should parse hexadecimal numbers`() {
        JSON5.parse("[0x1,0x10,0xff,0xFF]") shouldBe listOf(1.0, 16.0, 255.0, 255.0)
    }

    @Test
    fun `should parse infinity values`() {
        JSON5.parse("[Infinity,-Infinity]") shouldBe listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
    }

    @Test
    fun `should parse NaN`() {
        val result = JSON5.parse("NaN")
        result.shouldBeInstanceOf<Double>()
        assertTrue((result as Double).isNaN())
    }

    @Test
    fun `should parse signed NaN`() {
        val result = JSON5.parse("-NaN")
        result.shouldBeInstanceOf<Double>()
        assertTrue((result as Double).isNaN())
    }

    @Test
    fun `should parse bare numbers`() {
        JSON5.parse("1") shouldBe 1.0
        JSON5.parse("+1.23e100") shouldBe 1.23e100
    }

    @Test
    fun `should parse bare hexadecimal numbers`() {
        JSON5.parse("0x1") shouldBe 1.0
        JSON5.parse("-0x0123456789abcdefABCDEF") shouldBe -0x0123456789abcdefL.toDouble()
    }

    // String tests

    @Test
    fun `should parse double quoted strings`() {
        JSON5.parse("\"abc\"") shouldBe "abc"
    }

    @Test
    fun `should parse single quoted strings`() {
        JSON5.parse("'abc'") shouldBe "abc"
    }

    @Test
    fun `should parse quotes in strings`() {
        JSON5.parse("""['"',"'"]""") shouldBe listOf("\"", "'")
    }

    @Test
    fun `should parse escaped characters`() {
        JSON5.parse("""'\\b\\f\\n\\r\\t\\v\\0\\x0f\\u01fF\\\n\\\r\n\\\r\\\u2028\\\u2029\\a\\\'\\\"'""") shouldBe
            "\b\u000C\n\r\t\u000B\u0000\u000F\u01FF\u0007'\""
    }

    @Test
    fun `should parse line and paragraph separators`() {
        JSON5.parse("'\u2028\u2029'") shouldBe "\u2028\u2029"
    }

    // Comments tests

    @Test
    fun `should parse single-line comments`() {
        JSON5.parse("{//comment\n}") shouldBe emptyMap<String, Any?>()
    }

    @Test
    fun `should parse single-line comments at end of input`() {
        JSON5.parse("{}//comment") shouldBe emptyMap<String, Any?>()
    }

    @Test
    fun `should parse multi-line comments`() {
        JSON5.parse("{/*comment\n** */}") shouldBe emptyMap<String, Any?>()
    }

    // Whitespace tests

    @Test
    fun `should parse whitespace`() {
        JSON5.parse("{\t\u000B\u000C \u00A0\uFEFF\n\r\u2028\u2029\u2003}") shouldBe emptyMap<String, Any?>()
    }

    // Reviver tests

    @Test
    fun `should modify property values using reviver`() {
        JSON5.parse("{a:1,b:2}") { k, v -> if (k == "a") "revived" else v } shouldBe mapOf("a" to "revived", "b" to 2.0)
    }

    @Test
    fun `should modify nested object property values using reviver`() {
        JSON5.parse("{a:{b:2}}") { k, v -> if (k == "b") "revived" else v } shouldBe mapOf("a" to mapOf("b" to "revived"))
    }

    @Test
    fun `should delete property values using reviver`() {
        JSON5.parse("{a:1,b:2}") { k, v -> if (k == "a") null else v } shouldBe mapOf("b" to 2.0)
    }

    @Test
    fun `should modify array values using reviver`() {
        JSON5.parse("[0,1,2]") { k, v -> if (k == "1") "revived" else v } shouldBe listOf(0.0, "revived", 2.0)
    }

    @Test
    fun `should modify nested array values using reviver`() {
        JSON5.parse("[0,[1,2,3]]") { k, v -> if (k == "2") "revived" else v } shouldBe listOf(0.0, listOf(1.0, 2.0, "revived"))
    }

    @Test
    fun `should delete array values using reviver`() {
        val result = JSON5.parse("[0,1,2]") { k, v -> if (k == "1") null else v } as List<*>
        result[0] shouldBe 0.0
        result[1] shouldBe null
        result[2] shouldBe 2.0
    }

    @Test
    fun `should modify the root value using reviver`() {
        JSON5.parse("1") { k, v -> if (k == "") "revived" else v } shouldBe "revived"
    }

    @Test
    fun `should throw exception for invalid JSON5`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{invalid}")
        }
        exception.lineNumber shouldBe 1
    }
}
