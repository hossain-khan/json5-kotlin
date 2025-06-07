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
        exception.message shouldContain "invalid character 'a'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2
    }

    @Test
    fun `should throw on unterminated multiline comments`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("/*")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // This is because it counts the position after the last char
    }

    @Test
    fun `should throw on unterminated multiline comment closings`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("/**")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4 // Position after last character
    }

    @Test
    fun `should throw on invalid characters in values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("a")
        }
        exception.message shouldContain "invalid character 'a'"
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
        exception.columnNumber shouldBe 4
    }

    @Test
    fun `should throw on invalid identifier continue characters`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a\\u0021:1}")
        }
        exception.message shouldContain "invalid identifier character"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
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
        exception.message shouldContain "invalid character 'g'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3
    }

    @Test
    fun `should throw on invalid new lines in strings`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("\"\n\"")
        }
        exception.message shouldContain "invalid character '\\n'"
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
            JSON5.parse("\"\\")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Position after last character
    }

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
    fun `should throw on octal escapes`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("'\\01'")
        }
        exception.message shouldContain "invalid character '1'"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4
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
        exception.columnNumber shouldBe 2 // Position after the "{"
    }

    @Test
    fun `should throw on unclosed objects after property names`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Position after the "a"
    }

    @Test
    fun `should throw on unclosed objects before property values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a:")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 4 // Position after the ":"
    }

    @Test
    fun `should throw on unclosed objects after property values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{a:1")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 5 // Position after the "1"
    }

    @Test
    fun `should throw on unclosed arrays before values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("[")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 2 // Position after the "["
    }

    @Test
    fun `should throw on unclosed arrays after values`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("[1")
        }
        exception.message shouldContain "invalid end of input"
        exception.lineNumber shouldBe 1
        exception.columnNumber shouldBe 3 // Position after the "1" (cursor position after reading the number)
    }
}
