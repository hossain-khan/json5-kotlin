package io.github.json5.kotlin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JSON5.parse errors")
class JSON5ParseErrorsTest {

    @Test
    fun `should throw on empty documents`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1
    }

    @Test
    fun `should throw on documents with only comments`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("//a")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // After reading "//a", the cursor is at the end of input
    }

    @Test
    fun `should throw on incomplete single line comments`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("/a")
        }
        exception.message shouldContain "invalid character '/'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1
    }

    @Test
    fun `should throw on unterminated multiline comments`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("/*")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2 // Position of the '*'
    }

    @Test
    fun `should throw on unterminated multiline comment closings`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("/**")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Position of the second '*'
    }

    @Test
    fun `should throw on invalid characters in values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("a")
        }
        exception.message shouldContain "Unexpected identifier: a" // Adjusted message
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1
    }

    @Test
    fun `should throw on invalid characters in identifier start escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{\\a:1}")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    @Test
    fun `should throw on invalid identifier start characters`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{\\u0021:1}")
        }
        exception.message shouldContain "invalid identifier character"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    @Test
    fun `should throw on invalid characters in identifier continue escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a\\a:1}")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Parser reported error at col 3
    }

    @Test
    fun `should throw on invalid identifier continue characters`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a\\u0021:1}") // ! is not a valid identifier part
        }
        exception.message shouldContain "invalid identifier character"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // The column where '\u0021' starts
    }

    @Test
    @DisplayName("Object: should throw for invalid keys")
    fun `object invalid keys`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("{null: 1}") }
        ex1.message shouldContain "invalid character 'n'" // Lexer sees 'n', expects identifier or string key
        ex1.lineNumber shouldBe 1
        // ex1.columnNumber shouldBe 2 // 'n'

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("{true: 1}") }
        ex2.message shouldContain "invalid character 't'"
        ex2.lineNumber shouldBe 1
        // ex2.columnNumber shouldBe 2 // 't'

        val ex3 = shouldThrow<JSON5Exception> { JSON5.parse("{123: 1}") }
        ex3.message shouldContain "invalid character '1'"
        ex3.lineNumber shouldBe 1
        // ex3.columnNumber shouldBe 2 // '1'
    }

    @Test
    @DisplayName("Object: should throw for comma issues")
    fun `object comma issues`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("{a:1,,}") }
        ex1.message shouldContain "invalid character '}'" // Expects a key after comma
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 7

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("{,a:1}") }
        ex2.message shouldContain "invalid character ','" // Cannot start with comma
        ex2.lineNumber shouldBe 1
        ex2.columnNumber shouldBe 2
    }

    @Test
    @DisplayName("Object: should throw for structure issues")
    fun `object structure issues`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("{a:1 b:2}") } // Missing comma
        ex1.message shouldContain "invalid character 'b'" // Expects comma or }
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 6

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("{\"a\":1 \"b\":2}") } // Missing comma, string keys
        ex2.message shouldContain "invalid character '\"'" // Expects comma or }
        ex2.lineNumber shouldBe 1
        ex2.columnNumber shouldBe 8

        val ex3 = shouldThrow<JSON5Exception> { JSON5.parse("{a:}") } // Missing value
        ex3.message shouldContain "invalid character '}'" // Expects a value
        ex3.lineNumber shouldBe 1
        ex3.columnNumber shouldBe 4

        val ex4 = shouldThrow<JSON5Exception> { JSON5.parse("{a:1, :2}") } // Missing key
        ex4.message shouldContain "invalid character ':'" // Expects a key
        ex4.lineNumber shouldBe 1
        ex4.columnNumber shouldBe 7
    }

    @Test
    @DisplayName("Array: should throw for comma issues")
    fun `array comma issues`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("[1,2,,]") }
        ex1.message shouldContain "invalid character ']'" // Expects a value after comma
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 7

        val ex2 = shouldThrow<JSON5Exception> { JSON5.parse("[,1,2]") }
        ex2.message shouldContain "invalid character ','" // Cannot start with comma
        ex2.lineNumber shouldBe 1
        ex2.columnNumber shouldBe 2

        val ex3 = shouldThrow<JSON5Exception> { JSON5.parse("[1,,2]") } // Elision not allowed by spec
        ex3.message shouldContain "invalid character ','" // Expects value, finds comma
        ex3.lineNumber shouldBe 1
        ex3.columnNumber shouldBe 4
    }

    @Test
    @DisplayName("Array: should throw for structure issues")
    fun `array structure issues`() {
        val ex1 = shouldThrow<JSON5Exception> { JSON5.parse("[1 2]") } // Missing comma
        ex1.message shouldContain "invalid character '2'" // Expects comma or ]
        ex1.lineNumber shouldBe 1
        ex1.columnNumber shouldBe 4
    }

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

    @Test
    @DisplayName("String: should throw for invalid unescaped newline")
    fun `string invalid unescaped newline`() {
        val jsonStringWithUnescapedLF = "'abc\ndef'" // Kotlin makes this a literal LF
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse(jsonStringWithUnescapedLF)
        }
        exception.message shouldContain "invalid character '\\x0a'" // LF
        exception.lineNumber shouldBe 1 // Error is on the first line where string starts
        exception.columnNumber shouldBe 5 // After 'abc'
    }


    @Test
    fun `should throw on invalid characters following a sign`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("-a")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    @Test
    fun `should throw on invalid characters following a leading decimal point`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse(".a")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    @Test
    fun `should throw on invalid characters following an exponent indicator`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("1ea")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    @Test
    fun `should throw on invalid characters following an exponent sign`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("1e-a")
        }
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
    }

    @Test
    fun `should throw on invalid characters following a hexadecimal indicator`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("0xg")
        }
        exception.message shouldContain "invalid character 'g'" // Corrected, was 'x'
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    @Test
    @DisplayName("Number: should throw for various invalid formats")
    fun `number invalid formats`() {
        shouldThrow<JSON5Exception> { JSON5.parse("1.2.3") }
            .let { it.message shouldContain "invalid character '.'" }
        shouldThrow<JSON5Exception> { JSON5.parse("1e") } // Missing exponent digits
            .let { it.message shouldBe "Invalid character ' ' at line 1, column 3" } // Assuming EOF
        shouldThrow<JSON5Exception> { JSON5.parse("1e+") } // Missing exponent digits after sign
            .let { it.message shouldBe "Invalid character ' ' at line 1, column 4" } // Assuming EOF
        shouldThrow<JSON5Exception> { JSON5.parse("1.e+") } // Missing exponent digits after sign
            .let { it.message shouldBe "Invalid character ' ' at line 1, column 5" } // Assuming EOF
        shouldThrow<JSON5Exception> { JSON5.parse("InfinityX") }
            .let {
                it.message shouldContain "invalid character 'X'"
                it.lineNumber shouldBe 1
                it.columnNumber shouldBe 9
            }
        shouldThrow<JSON5Exception> { JSON5.parse("NaNX") }
            .let {
                it.message shouldContain "invalid character 'X'"
                it.lineNumber shouldBe 1
                it.columnNumber shouldBe 4
            }
        shouldThrow<JSON5Exception> { JSON5.parse("123a") } // trailing invalid char
            .let {
                it.message shouldContain "invalid character 'a'"
                it.lineNumber shouldBe 1
                it.columnNumber shouldBe 4
            }
        shouldThrow<JSON5Exception> { JSON5.parse("+.") }
            .let {
                it.message shouldContain "invalid character '.'"
                it.lineNumber shouldBe 1
                it.columnNumber shouldBe 2
            }
        shouldThrow<JSON5Exception> { JSON5.parse("-NaNX") }
            .let {
                it.message shouldContain "invalid character 'X'"
                it.lineNumber shouldBe 1
                it.columnNumber shouldBe 5
            }
    }

    @Test
    fun `should throw on invalid new lines in strings`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\n\"")
        }
        exception.message shouldContain "invalid character '\\x0a'" // Match actual message
        exception.lineNumber shouldBe 2
        exception.columnNumber shouldBe 1 // In JavaScript, the column resets to 0, but Kotlin uses 1-indexed
    }

    @Test
    fun `should throw on unterminated strings`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    @Test
    fun `should throw on invalid identifier start characters in property names`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{!:1}")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    @Test
    fun `should throw on invalid characters following a property name`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a!1}")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    @Test
    fun `should throw on invalid characters following a property value`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a:1!}")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 5
    }

    @Test
    fun `should throw on invalid characters following an array value`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("[1!]")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    @Test
    fun `should throw on invalid characters in literals`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("tru!")
        }
        exception.message shouldContain "invalid character '!'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
    }

    @Test
    fun `should throw on unterminated escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\\") // Dangling backslash in string
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        // The column number here refers to the point where the input unexpectedly ends.
        // If the input is just "\"\\", then after reading '\', currentchar is null.
        // The error "invalid end of input" is reported at line 1, col 3 (pos after last char).
        exception.columnNumber shouldBe 3
    }

    // Note: Invalid hex/unicode escapes in strings are covered by JSON5ParseTest's
    // `parse invalid and edge case escapes` test, as they throw exceptions.

    @Test
    fun `should throw on invalid first digits in hexadecimal escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\\xg\"")
        }
        exception.message shouldContain "invalid character 'g'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
    }

    @Test
    fun `should throw on invalid second digits in hexadecimal escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\\x0g\"")
        }
        exception.message shouldContain "invalid character 'g'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 5
    }

    @Test
    fun `should throw on invalid unicode escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\\u000g\"")
        }
        exception.message shouldContain "invalid character 'g'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 7
    }

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

    @Test
    fun `should throw on octal escapes in strings`() { // Clarified name
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\01'") // \0 followed by digit is error
        }
        exception.message shouldContain "invalid character '1'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
    }

    @Test
    @DisplayName("Comment: should throw for unterminated multiline comment with content")
    fun `comment unterminated multiline with content`() {
        val ex = shouldThrow<JSON5Exception> {
            JSON5.parse("/* abc")
        }
        ex.message shouldContain "invalid end of input"
        ex.lineNumber shouldBe 1
        ex.columnNumber shouldBe 7 // After "/* abc"
    }

    @Test
    fun `should throw on multiple values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("1 2")
        }
        exception.message shouldContain "invalid character '2'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    @Test
    fun `should throw with control characters escaped in the message`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\u0001")
        }
        exception.message shouldContain "invalid character '\\x01'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1
    }

    @Test
    fun `should throw on unclosed objects before property names`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1 // Position of the "{"
    }

    @Test
    fun `should throw on unclosed objects after property names`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2 // Position of the "a"
    }

    @Test
    fun `should throw on unclosed objects before property values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a:")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Position of the ":"
    }

    @Test
    fun `should throw on unclosed objects after property values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a:1")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4 // Position of the "1"
    }

    @Test
    fun `should throw on unclosed arrays before values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("[")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 1 // Position of the "["
    }

    @Test
    fun `should throw on unclosed arrays after values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("[1")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2 // Position of the "1"
    }
}
