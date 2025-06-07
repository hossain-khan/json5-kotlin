package io.github.json5.kotlin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain // Added this import
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.Double.Companion.NaN
import kotlin.test.assertTrue
import kotlin.math.pow // Added import

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
        JSON5.parse("""{ \${'$'}_: 1, _\$: 2, a\u200C: 3 }""") shouldBe mapOf(
            "\${\$}_" to 1.0,
            "_\$" to 2.0,
            "a\u200C" to 3.0
        )
    }

    @Test
    fun `should parse unicode property names`() {
        JSON5.parse("""{ùńîċõďë:9}""") shouldBe mapOf("ùńîċõďë" to 9.0)
    }

    @Test
    fun `should parse escaped property names`() {
        // Note: The double backslashes in the test string become single backslashes in the actual string
        JSON5.parse("""{ \\u0061\\u0062: 1, \\u0024\\u005F: 2, \\u005F\\u0024: 3 }""") shouldBe mapOf(
            "ab" to 1.0,
            "\$_" to 2.0,
            "_\$" to 3.0
        )
    }

    @Test
    @DisplayName("should parse diverse identifiers correctly")
    fun `parse diverse identifiers`() {
        JSON5.parse("{ _: 1 }") shouldBe mapOf("_" to 1.0)
        JSON5.parse("{ \$: 2 }") shouldBe mapOf("\$" to 2.0)
        JSON5.parse("{ _ident: 3 }") shouldBe mapOf("_ident" to 3.0)
        JSON5.parse("{ \$ident: 4 }") shouldBe mapOf("\$ident" to 4.0)
        JSON5.parse("{ ident_: 5 }") shouldBe mapOf("ident_" to 5.0)
        JSON5.parse("{ ident\$: 6 }") shouldBe mapOf("ident\$" to 6.0)
        JSON5.parse("{ üñîçødé: 7 }") shouldBe mapOf("üñîçødé" to 7.0) // Already covered but good to have
        JSON5.parse("{ \\u0061b\\u0063: 8 }") shouldBe mapOf("abc" to 8.0) // Multiple consecutive escapes
        JSON5.parse("{ id\\u0024ent: 9 }") shouldBe mapOf("id\$ent" to 9.0) // Escape resolves to $
        JSON5.parse("{ \\u005fid\\u005f: 10 }") shouldBe mapOf("_id_" to 10.0) // Escape resolves to _
        JSON5.parse("{ \\u0061: 11 }") shouldBe mapOf("a" to 11.0) // Identifier is a single escape
        JSON5.parse("{ \\u0061\\u0062c: 12 }") shouldBe mapOf("abc" to 12.0) // Starts with escapes, then normal char
    }

    @Test
    @DisplayName("should handle invalid unicode escapes in identifiers")
    fun `parse invalid unicode escapes in identifiers`() {
        val ex1 = shouldThrow<JSON5Exception> {
            JSON5.parse("{ \\u002G: 1 }") // Invalid hex G
        }
        ex1.message shouldBe "Invalid character 'G' at line 1, column 10"

        val ex2 = shouldThrow<JSON5Exception> {
            JSON5.parse("{ \\u123: 1 }") // Incomplete escape
        }
        ex2.message shouldBe "Invalid character ':' at line 1, column 10"


        val ex3 = shouldThrow<JSON5Exception> {
            JSON5.parse("{ ab\\u002Gcd: 1 }") // Invalid hex G in middle
        }
        ex3.message shouldBe "Invalid character 'G' at line 1, column 12"

        val ex4 = shouldThrow<JSON5Exception> {
            JSON5.parse("{ ab\\u123cd: 1 }") // Incomplete escape in middle
        }
        ex4.message shouldBe "Invalid character 'c' at line 1, column 12"

        val ex5 = shouldThrow<JSON5Exception> {
            JSON5.parse("""{ \a: 1 }""") // \a is not a valid escape for identifiers
        }
        ex5.message shouldBe "Invalid character 'a' at line 1, column 5"

        val ex6 = shouldThrow<JSON5Exception> {
            JSON5.parse("""{ \\: 1 }""") // Dangling backslash in identifier
        }
        // Depending on how the parser handles this, the message might vary.
        // It could be "Invalid end of input" if it expects 'u' or "Invalid character"
         ex6.message shouldContain "Invalid character" // More general check
         ex6.lineNumber shouldBe 1
         ex6.columnNumber shouldBe 5
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
        JSON5.parse("[0x1,0x10,0xff,0xFF, +0x10, -0xABC]") shouldBe listOf(1.0, 16.0, 255.0, 255.0, 16.0, -2748.0)
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
        // 0x0123456789abcdefABCDEF is 13117684674637903202078735
        // As a double, this is approximately 1.3117684674637903E25
        JSON5.parse("-0x0123456789abcdefABCDEF") shouldBe -1.3117684674637903E25
        JSON5.parse("+0xff") shouldBe 255.0
    }

    @Test
    @DisplayName("should parse large hexadecimal numbers")
    fun `parse large hexadecimal numbers`() {
        // Max Long as hex is 7fffffffffffffff
        // 0x1fffffffffffffff in decimal is 2305843009213693951
        JSON5.parse("0x1fffffffffffffff") shouldBe 2.305843009213694E18 // Might lose some precision
        // 0x2000000000000000 in decimal is 2305843009213693952
        JSON5.parse("0x2000000000000000") shouldBe 2.305843009213694E18 // Might be same as above due to double precision
        // A very large hex number
        JSON5.parse("0x123456789abcdef123456789abcdef123456789abcdef") shouldBe 3.777995208190904E49
        JSON5.parse("-0x123456789abcdef123456789abcdef123456789abcdef") shouldBe -3.777995208190904E49

        // Hex representation of Double.MAX_VALUE (0x1.fffffffffffffp+1023)
        // This is tricky because JSON5 hex are integers.
        // The largest exact integer a double can represent is 2^53.
        // 0x1FFFFFFFFFFFFF is 2^53 - 1
        JSON5.parse("0x1FFFFFFFFFFFFF") shouldBe (2.0.pow(53.0) - 1)
        // 0x20000000000000 is 2^53
        JSON5.parse("0x20000000000000") shouldBe 2.0.pow(53.0)
        // One larger than 2^53 will not be exact
        JSON5.parse("0x20000000000001") shouldBe (2.0.pow(53.0) + 2) // Due to rounding for doubles
    }

    @Test
    @DisplayName("should handle invalid hexadecimal numbers")
    fun `parse invalid hexadecimal numbers`() {
        val ex1 = shouldThrow<JSON5Exception> {
            JSON5.parse("0x")
        }
        ex1.message shouldBe "Invalid character ' ' at line 1, column 3" // Assuming EOF or space follows

        val ex2 = shouldThrow<JSON5Exception> {
            JSON5.parse("-0x")
        }
        ex2.message shouldBe "Invalid character ' ' at line 1, column 4" // Assuming EOF or space follows

        val ex3 = shouldThrow<JSON5Exception> {
            JSON5.parse("0xG")
        }
        ex3.message shouldBe "Invalid character 'G' at line 1, column 3"

        val ex4 = shouldThrow<JSON5Exception> {
            JSON5.parse("+0xG")
        }
        ex4.message shouldBe "Invalid character 'G' at line 1, column 4"

        val ex5 = shouldThrow<JSON5Exception> {
            JSON5.parse("0x12G")
        }
        ex5.message shouldBe "Invalid character 'G' at line 1, column 5"
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
        // Expected string after fixes:
        // \b -> \u0008 (Backspace)
        // \f -> \u000C (Form Feed)
        // \n -> \u000A (Line Feed)
        // \r -> \u000D (Carriage Return)
        // \t -> \u0009 (Horizontal Tab)
        // \v -> \u000B (Vertical Tab)
        // \0 -> \u0000 (Null character)
        // \x0f -> \u000F (Shift In)
        // \u01fF -> \u01FF (Latin Small Letter N With Grave with Acute)
        // \\\n -> line continuation, disappears
        // \\\r\n -> line continuation, disappears
        // \\\r -> line continuation, disappears
        // \\\u2028 -> line continuation, disappears
        // \\\u2029 -> line continuation, disappears
        // \\a -> a (literal 'a')
        // \\' -> ' (single quote)
        // \\" -> " (double quote)
        JSON5.parse("""'\\b\\f\\n\\r\\t\\v\\0\\x0f\\u01fF\\\n\\\r\n\\\r\\\u2028\\\u2029\\a\\\'\\\"'""") shouldBe
            "\u0008\u000C\n\r\t\u000B\u0000\u000F\u01FFa'\""
    }

    @Test
    @DisplayName("should parse line continuations correctly")
    fun `parse line continuations`() {
        JSON5.parse("'ab\\\ncd'") shouldBe "abcd"
        JSON5.parse("'ab\\\r\ncd'") shouldBe "abcd"
        JSON5.parse("'ab\\\rcd'") shouldBe "abcd" // \r is also a line terminator
        JSON5.parse("'ab\\\u2028cd'") shouldBe "abcd"
        JSON5.parse("'ab\\\u2029cd'") shouldBe "abcd"
    }

    @Test
    @DisplayName("should parse unrecognized simple escapes as the character itself")
    fun `parse unrecognized simple escapes`() {
        JSON5.parse("'\\a'") shouldBe "a"
        JSON5.parse("'\\c'") shouldBe "c"
        JSON5.parse("'\\/'") shouldBe "/"
        JSON5.parse("'\\1'") shouldBe "1" // \1 is not an octal escape in JSON5
        JSON5.parse("'\\ '") shouldBe " " // \ followed by space
    }

    @Test
    @DisplayName("should handle invalid and edge case escapes")
    fun `parse invalid and edge case escapes`() {
        // Invalid octal-like escapes
        val ex1 = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\07'")
        }
        ex1.message shouldBe "Invalid character '7' at line 1, column 5"


        // Invalid hex escapes
        val ex2 = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\x0G'") // G is not a hex digit
        }
        ex2.message shouldBe "Invalid character 'G' at line 1, column 6"

        val ex3 = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\u000G'") // G is not a hex digit
        }
        ex3.message shouldBe "Invalid character 'G' at line 1, column 8"

        // Incomplete hex escapes
        val ex4 = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\x0'") // Missing one hex digit
        }
        ex4.message shouldBe "Invalid character ''' at line 1, column 5" // Reports quote as it's expecting another hex

        val ex5 = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\u000'") // Missing one hex digit
        }
        ex5.message shouldBe "Invalid character ''' at line 1, column 7" // Reports quote

        val ex6 = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\x'") // Missing two hex digits
        }
        ex6.message shouldBe "Invalid character ''' at line 1, column 4"

        val ex7 = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\u'") // Missing four hex digits
        }
        ex7.message shouldBe "Invalid character ''' at line 1, column 4"

        val ex8 = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\") // Dangling backslash
        }
        ex8.message shouldBe "Invalid end of input at line 1, column 3"
    }


    @Test
    fun `should parse line and paragraph separators`() {
        // Note: JSON5 spec (Section 5.2) states that while U+2028 and U+2029 are allowed unescaped in strings,
        // parsers SHOULD produce a warning. This implementation currently allows them without a warning mechanism.
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
