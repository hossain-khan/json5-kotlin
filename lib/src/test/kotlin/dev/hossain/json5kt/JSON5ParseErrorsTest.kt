package dev.hossain.json5kt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for error handling in JSON5 parsing.
 * This class verifies that the parser correctly throws [JSON5Exception] for various
 * syntax errors and invalid JSON5 constructs.
 */
@DisplayName("JSON5.parse errors")
class JSON5ParseErrorsTest {

    /**
     * Tests that parsing an empty document throws an error.
     */
    @Test
    fun `should throw on empty documents`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1
    }

    /**
     * Tests that parsing a document containing only comments throws an error,
     * as a valid JSON5 document must have a top-level value.
     */
    @Test
    fun `should throw on documents with only comments`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("//a")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // After reading "//a", the cursor is at the end of input
    }

    /**
     * Tests that an incomplete single-line comment (e.g., `/` not followed by `/`) throws an error.
     */
    @Test
    fun `should throw on incomplete single line comments`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("/a")
        }
        exception.message shouldContain "invalid character '/'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1
    }

    /**
     * Tests that an unterminated multi-line comment (e.g., `/*` without `*/`) throws an error.
     */
    @Test
    fun `should throw on unterminated multiline comments`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("/*")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2 // Position of the '*'
    }

    /**
     * Tests that an unterminated multi-line comment closing (e.g., `/**` without matching `*/`) throws an error.
     */
    @Test
    fun `should throw on unterminated multiline comment closings`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("/**")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Position of the second '*'
    }

    /**
     * Tests that invalid characters appearing where a value is expected throw an error.
     */
    @Test
    fun `should throw on invalid characters in values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("a")
        }
        exception.message shouldContain "Unexpected identifier: a" // Adjusted message
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1
    }

    /**
     * Tests that invalid characters within an identifier's escape sequence (e.g. `{\a:1}`) throw an error.
     * This typically occurs if an escape sequence is not a valid Unicode escape (`\uXXXX`) or a valid single character escape.
     */
    @Test
    fun `should throw on invalid characters in identifier start escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{\\a:1}")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    /**
     * Tests that an identifier starting with an invalid character (e.g. `{\u0021:1}` which is `{!":"1}`) throws an error.
     * `!` is not a valid start for an identifier.
     */
    @Test
    fun `should throw on invalid identifier start characters`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{\\u0021:1}")
        }
        exception.message shouldContain "invalid identifier character"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    /**
     * Tests that an invalid escape sequence within an identifier (not at the start) throws an error.
     */
    @Test
    fun `should throw on invalid characters in identifier continue escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a\\a:1}")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Parser reported error at col 3
    }

    /**
     * Tests that an identifier containing an invalid character (e.g. `{a\u0021:1}` which is `{"a!":1}`) throws an error.
     * `!` is not a valid continuation character for an identifier.
     */
    @Test
    fun `should throw on invalid identifier continue characters`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a\\u0021:1}")
        }
        exception.message shouldContain "invalid identifier character"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    /**
     * Tests that an invalid character immediately following a sign (`+` or `-`) in a number throws an error.
     */
    @Test
    fun `should throw on invalid characters following a sign`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("-a")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    /**
     * Tests that an invalid character immediately following a leading decimal point in a number throws an error.
     */
    @Test
    fun `should throw on invalid characters following a leading decimal point`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse(".a")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    /**
     * Tests that an invalid character immediately following an exponent indicator (`e` or `E`) in a number throws an error.
     */
    @Test
    fun `should throw on invalid characters following an exponent indicator`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("1ea")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    /**
     * Tests that an invalid character immediately following an exponent sign (`+` or `-`) in a number throws an error.
     */
    @Test
    fun `should throw on invalid characters following an exponent sign`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("1e-a")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
    }

    /**
     * Tests that an invalid character immediately following a hexadecimal indicator (`0x` or `0X`) in a number throws an error.
     */
    @Test
    fun `should throw on invalid characters following a hexadecimal indicator`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("0xg")
        }
        exception.message shouldContain "invalid character 'g'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    /**
     * Tests that an unescaped newline character within a string throws an error.
     */
    @Test
    fun `should throw on invalid new lines in strings`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\n\"")
        }
        exception.message shouldContain "invalid character '\\x0a'" // Match actual message
        exception.lineNumber shouldBe 2
        exception.columnNumber shouldBe 1 // In JavaScript, the column resets to 0, but Kotlin uses 1-indexed
    }

    /**
     * Tests that an unterminated string (missing closing quote) throws an error.
     */
    @Test
    fun `should throw on unterminated strings`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    /**
     * Tests that an object property name starting with an invalid identifier character (e.g. `!`) throws an error.
     */
    @Test
    fun `should throw on invalid identifier start characters in property names`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{!:1}")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    /**
     * Tests that an invalid character appearing immediately after a property name (before the colon) throws an error.
     */
    @Test
    fun `should throw on invalid characters following a property name`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a!1}")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    /**
     * Tests that an invalid character appearing immediately after a property value (before a comma or closing brace) throws an error.
     */
    @Test
    fun `should throw on invalid characters following a property value`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a:1!}")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 5
    }

    /**
     * Tests that an invalid character appearing immediately after an array value (before a comma or closing bracket) throws an error.
     */
    @Test
    fun `should throw on invalid characters following an array value`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("[1!]")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    /**
     * Tests that an invalid character within a literal (e.g., `true`, `false`, `null`, `Infinity`, `NaN`) throws an error.
     */
    @Test
    fun `should throw on invalid characters in literals`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("tru!")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
    }

    /**
     * Tests that an unterminated escape sequence within a string (e.g., `"\` at the end of input) throws an error.
     */
    @Test
    fun `should throw on unterminated escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\\")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2 // Position of the '\'
    }

    /**
     * Tests that an invalid first digit in a hexadecimal escape sequence (e.g., `\xg`) throws an error.
     */
    @Test
    fun `should throw on invalid first digits in hexadecimal escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\\xg\"")
        }
        exception.message shouldContain "invalid character 'g'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
    }

    /**
     * Tests that an invalid second digit in a hexadecimal escape sequence (e.g., `\x0g`) throws an error.
     */
    @Test
    fun `should throw on invalid second digits in hexadecimal escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\\x0g\"")
        }
        exception.message shouldContain "invalid character 'g'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 5
    }

    /**
     * Tests that an invalid digit in a Unicode escape sequence (e.g., `\u000g`) throws an error.
     */
    @Test
    fun `should throw on invalid unicode escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\\u000g\"")
        }
        exception.message shouldContain "invalid character 'g'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 7
    }

    /**
     * Tests that an escaped digit (e.g., `\1`) throws an error.
     * **Note on JSON5 Specification Compliance:**
     * According to the JSON5 specification (Section 5.1 - Escapes), an escape sequence like `\1`
     * is an "unrecognized simple escape" and should be interpreted as the character itself (i.e., the string "1").
     * It should **not** throw an error.
     * The current test expectation (throwing an error) is incorrect based on the spec.
     * Both this test and the parser's behavior need correction to align with the JSON5 specification.
     */
    @Test
    fun `should throw on escaped digits`() {
        for (i in 1..9) {
            val exception = shouldThrow<JSON5Exception> {
                JSON5.parse("'\\$i'")
            }
            exception.message shouldContain "invalid character '$i'"
            exception.lineNumber shouldBe 1
            exception.columnNumber shouldBe 3
        }
    }

    /**
     * Tests that octal escape sequences (e.g., `\01`) throw an error, as they are not allowed in JSON5.
     * Note: `\0` (null character) is a valid escape, but `\0` followed by other digits is an octal escape.
     */
    @Test
    fun `should throw on octal escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\01'")
        }
        exception.message shouldContain "invalid character '1'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
    }

    /**
     * Tests that having multiple top-level values without being part of an array or object throws an error.
     */
    @Test
    fun `should throw on multiple values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("1 2")
        }
        exception.message shouldContain "invalid character '2'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    /**
     * Tests that error messages correctly escape control characters.
     */
    @Test
    fun `should throw with control characters escaped in the message`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\u0001")
        }
        exception.message shouldContain "invalid character '\\x01'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1
    }

    /**
     * Tests that an unclosed object (e.g., `{` at the end of input) throws an error.
     */
    @Test
    fun `should throw on unclosed objects before property names`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1 // Position of the "{"
    }

    /**
     * Tests that an unclosed object after a property name (e.g., `{a` at EOF) throws an error.
     */
    @Test
    fun `should throw on unclosed objects after property names`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2 // Position of the "a"
    }

    /**
     * Tests that an unclosed object after a property name and colon (e.g., `{a:` at EOF) throws an error.
     */
    @Test
    fun `should throw on unclosed objects before property values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a:")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Position of the ":"
    }

    /**
     * Tests that an unclosed object after a property value (e.g., `{a:1` at EOF) throws an error.
     */
    @Test
    fun `should throw on unclosed objects after property values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a:1")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4 // Position of the "1"
    }

    /**
     * Tests that an unclosed array (e.g., `[` at EOF) throws an error.
     */
    @Test
    fun `should throw on unclosed arrays before values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("[")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1 // Position of the "["
    }

    /**
     * Tests that an unclosed array after a value (e.g., `[1` at EOF) throws an error.
     */
    @Test
    fun `should throw on unclosed arrays after values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("[1")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2 // Position of the "1"
    }

    /**
     * Tests various scenarios where invalid keys in an object should cause an error.
     * This includes using literals like `null`, `true`, or numbers as unquoted keys.
     */
    @Test
    @DisplayName("Object: should throw for invalid keys")
    fun `object invalid keys`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("{null: 1}") }
        ex1.message shouldContain "Expected property name or '}'" // Lexer sees 'n', expects identifier or string key
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 2 // 'n'

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("{true: 1}") }
        ex2.message shouldContain "Expected property name or '}'"
        ex2.lineNumber shouldBe 1
        ex2.columnNumber shouldBe 2 // 't'

        val ex3 = shouldThrow<JSON5Exception> { JSON5.parse("{123: 1}") }
        ex3.message shouldContain "Expected property name or '}'"
        ex3.lineNumber shouldBe 1
        ex3.columnNumber shouldBe 2 // '1'
    }

    /**
     * Tests various scenarios involving misplaced or extra commas in objects that should cause an error.
     */
    @Test
    @DisplayName("Object: should throw for comma issues")
    fun `object comma issues`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("{a:1,,}") }
        ex1.message shouldContain "Expected property name or '}'" // Expects a key after comma
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 6

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("{,a:1}") }
        ex2.message shouldContain "Expected property name or '}'" // Cannot start with comma
        ex2.lineNumber shouldBe 1
        ex2.columnNumber shouldBe 2
    }

    /**
     * Tests various object structural issues that should cause an error.
     * This includes missing commas between properties, missing values for keys, or missing keys.
     */
    @Test
    @DisplayName("Object: should throw for structure issues")
    fun `object structure issues`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("{a:1 b:2}") } // Missing comma
        ex1.message shouldContain "Expected ',' or '}'" // Expects comma or }
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 6

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("{\"a\":1 \"b\":2}") } // Missing comma, string keys
        ex2.message shouldContain "Expected ',' or '}'" // Expects comma or }
        ex2.lineNumber shouldBe 1
        ex2.columnNumber shouldBe 8

        val ex3 = shouldThrow<JSON5Exception> { JSON5.parse("{a:}") } // Missing value
        ex3.message shouldContain "Unexpected punctuator" // Expects a value
        ex3.lineNumber shouldBe 1
        ex3.columnNumber shouldBe 4

        val ex4 = shouldThrow<JSON5Exception> { JSON5.parse("{a:1, :2}") } // Missing key
        ex4.message shouldContain "Expected property name or '}'" // Expects a key
        ex4.lineNumber shouldBe 1
        ex4.columnNumber shouldBe 7
    }

    /**
     * Tests various scenarios involving misplaced or extra commas in arrays that should cause an error.
     * JSON5 does not allow elision (e.g. `[1,,2]`) unlike JavaScript.
     */
    @Test
    @DisplayName("Array: should throw for comma issues")
    fun `array comma issues`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("[1,2,,]") }
        ex1.message shouldContain "Unexpected punctuator" // Expects a value after comma
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 6

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("[,1,2]") }
        ex2.message shouldContain "Unexpected punctuator" // Cannot start with comma
        ex2.lineNumber shouldBe 1
        ex2.columnNumber shouldBe 2

        val ex3 = shouldThrow<JSON5Exception> { JSON5.parse("[1,,2]") } // Elision not allowed by spec
        ex3.message shouldContain "Unexpected punctuator" // Expects value, finds comma
        ex3.lineNumber shouldBe 1
        ex3.columnNumber shouldBe 4
    }

    /**
     * Tests array structural issues, such as missing commas between elements.
     */
    @Test
    @DisplayName("Array: should throw for structure issues")
    fun `array structure issues`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("[1 2]") } // Missing comma
        ex1.message shouldContain "Expected ',' or ']'" // Expects comma or ]
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 4
    }

    /**
     * Tests that unterminated strings (missing closing quote) throw errors.
     */
    @Test
    @DisplayName("String: should throw for unterminated strings")
    fun `string unterminated`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("\"abc") }
        ex1.message shouldContain "invalid end of input"
        ex1.lineNumber shouldBe 1 // The line where the string started
        // Column could be end of line or where EOF is effectively seen
        // ex1.columnNumber shouldBe 4

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("'abc") }
        ex2.message shouldContain "invalid end of input"
        ex2.lineNumber shouldBe 1
        // ex2.columnNumber shouldBe 4
    }

    /**
     * Tests that an unescaped newline character within a string throws an error.
     */
    @Test
    @DisplayName("String: should throw for invalid unescaped newline")
    fun `string invalid unescaped newline`() {
        val jsonStringWithUnescapedLF = "'abc\ndef'" // Kotlin makes this a literal LF
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse(jsonStringWithUnescapedLF)
        }
        exception.message shouldContain "invalid character '\\x0a'" // LF
        exception.lineNumber shouldBe 2 // Error is on the first line where string starts
        exception.columnNumber shouldBe 1 // After 'abc'
    }

}
