package io.github.json5.kotlin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

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

    @Test
    fun `should throw exception for invalid JSON5`() {
        val exception = shouldThrow<JSON5Exception> {
            JSON5.parse("{invalid}")
        }
        exception.lineNumber shouldBe 1
    }

    @Test
    fun `should parse Infinity values`() {
        JSON5.parse("Infinity") shouldBe Double.POSITIVE_INFINITY
        JSON5.parse("+Infinity") shouldBe Double.POSITIVE_INFINITY
        JSON5.parse("-Infinity") shouldBe Double.NEGATIVE_INFINITY
    }

    @Test
    fun `should parse NaN values`() {
        JSON5.parse("NaN") shouldBe Double.NaN
        JSON5.parse("+NaN") shouldBe Double.NaN
        JSON5.parse("-NaN") shouldBe Double.NaN
    }

    @Test
    fun `should parse Infinity and NaN values in an array`() {
        JSON5.parse("[Infinity, -Infinity, NaN, +Infinity, +NaN, -NaN]") shouldBe listOf(
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NaN,
            Double.NaN
        )
    }

    @Test
    fun `should parse strings with line continuations`() {
        JSON5.parse(""""abc\\\ndef"""") shouldBe "abcdef"
        JSON5.parse(""""xy\\\nz"""") shouldBe "xyz"
        JSON5.parse(""""uvw\\\rx"""") shouldBe "uvwx" // Carriage return
        JSON5.parse(""""ijk\\\r\nlmn"""") shouldBe "ijklmn" // CRLF
    }

    @Test
    fun `should parse strings with multiple line continuations`() {
        JSON5.parse(""""one\\\ntwo\\\nthree"""") shouldBe "onetwothree"
        JSON5.parse(""""alpha\\\rbeta\\\rgamma"""") shouldBe "alphabetagamma"
    }

    @Test
    fun `should parse strings with mixed line continuations and other escape sequences`() {
        JSON5.parse(""""error:\\\n\tFile not found"""") shouldBe "error:\tFile not found"
        JSON5.parse(""""a\\\nb\\tc"""") shouldBe "ab\tc"
        JSON5.parse(""""first line\\\nsecond line\\\n\bthird line"""") shouldBe "first linesecond line\bthird line"
    }

    @Test
    fun `should parse strings with line continuations in object values`() {
        JSON5.parse("""{"key": "value\\\ncontinuation"}""") shouldBe mapOf("key" to "valuecontinuation")
    }

    @Test
    fun `should parse strings with line continuations in array values`() {
        JSON5.parse("""["element1\\\npart2", "another"]""") shouldBe listOf("element1part2", "another")
    }

    @Test
    fun `should parse unquoted keys with Unicode letters`() {
        JSON5.parse("{π: 3.14}") shouldBe mapOf("π" to 3.14)
        JSON5.parse("{привет: \"world\"}") shouldBe mapOf("привет" to "world")
        JSON5.parse("{日本語: \"Japanese\"}") shouldBe mapOf("日本語" to "Japanese")
        JSON5.parse("{κόσμε: \"Greek\"}") shouldBe mapOf("κόσμε" to "Greek") // Greek
        JSON5.parse("{नमस्ते: \"Hindi\"}") shouldBe mapOf("नमस्ते" to "Hindi") // Devanagari
    }

    @Test
    fun `should parse unquoted keys with Unicode letter numbers (Nl)`() {
        JSON5.parse("{Ⅰ: 1}") shouldBe mapOf("Ⅰ" to 1.0) // Roman numeral one
        JSON5.parse("{Ⅱ: 2, Ⅲ: 3}") shouldBe mapOf("Ⅱ" to 2.0, "Ⅲ" to 3.0)
    }

    @Test
    fun `should parse unquoted keys with Unicode connector punctuation (Pc)`() {
        JSON5.parse("{my‿key: \"value\"}") shouldBe mapOf("my‿key" to "value") // U+203F (undertie)
        JSON5.parse("{other＿key: \"data\"}") shouldBe mapOf("other＿key" to "data") // U+FF3F (fullwidth low line)
    }

    @Test
    fun `should parse unquoted keys with Unicode combining marks (Mn, Mc)`() {
        JSON5.parse("{char̀: \"accented\"}") shouldBe mapOf("char̀" to "accented") // a + combining grave accent
        JSON5.parse("{öp: \"umlaut\"}") shouldBe mapOf("öp" to "umlaut") // o + combining diaeresis
        JSON5.parse("{a̱b: \"macron\"}") shouldBe mapOf("a̱b" to "macron") // a + combining macron below
        // Note: Combining marks cannot be at the start of an identifier.
    }

    @Test
    fun `should parse unquoted keys with mixed Unicode characters including dollar and underscore`() {
        JSON5.parse("{\$myKey_1π: \"value\"}") shouldBe mapOf("\$myKey_1π" to "value")
        JSON5.parse("{_привет_Ⅱ: \"mixed\"}") shouldBe mapOf("_привет_Ⅱ" to "mixed")
    }

    @Test
    fun `should parse strings with unescaped U+2028 Line Separator`() {
        JSON5.parse(""""Hello\u2028World"""") shouldBe "Hello\u2028World"
        JSON5.parse("""{"text": "Line one\u2028Line two"}""") shouldBe mapOf("text" to "Line one\u2028Line two")
        JSON5.parse("""["First part\u2028Second part"]""") shouldBe listOf("First part\u2028Second part")
    }

    @Test
    fun `should parse strings with unescaped U+2029 Paragraph Separator`() {
        JSON5.parse(""""Sentence one.\u2029Sentence two."""") shouldBe "Sentence one.\u2029Sentence two."
        JSON5.parse("""{"paragraph": "Content one.\u2029Content two."}""") shouldBe mapOf("paragraph" to "Content one.\u2029Content two.")
        JSON5.parse("""["Para1.\u2029Para2." , "Para3.\u2029Para4." ]""") shouldBe listOf("Para1.\u2029Para2.", "Para3.\u2029Para4.")
    }

    @Test
    fun `should parse strings with mixed unescaped U+2028 and U+2029`() {
        JSON5.parse(""""Line A\u2028Line B\u2029Paragraph C"""") shouldBe "Line A\u2028Line B\u2029Paragraph C"
    }

    @Test
    fun `should treat U+FEFF (BOM) as whitespace`() {
        JSON5.parse("""{\uFEFF"key"\uFEFF:\uFEFF"value"\uFEFF}""") shouldBe mapOf("key" to "value")
        JSON5.parse("""[\uFEFF1\uFEFF,\uFEFFtrue\uFEFF]""") shouldBe listOf(1.0, true)
        JSON5.parse("""\uFEFF{\uFEFF"a":\uFEFF1\uFEFF}\uFEFF""") shouldBe mapOf("a" to 1.0)
        JSON5.parse("""\uFEFF[\uFEFFtrue\uFEFF]\uFEFF""") shouldBe listOf(true)
    }

    @Test
    fun `should treat U+00A0 (Non-breaking space) as whitespace`() {
        JSON5.parse("""{\u00A0"key"\u00A0:\u00A0"value"\u00A0}""") shouldBe mapOf("key" to "value")
        JSON5.parse("""[\u00A01\u00A0,\u00A0true\u00A0]""") shouldBe listOf(1.0, true)
        JSON5.parse("""\u00A0{\u00A0"a":\u00A01\u00A0}\u00A0""") shouldBe mapOf("a" to 1.0)
        JSON5.parse("""\u00A0[\u00A0true\u00A0]\u00A0""") shouldBe listOf(true)
    }

    @Test
    fun `should treat mixed JSON5 whitespace characters correctly`() {
        // Mix of space, tab, LF, CR, BOM, NBSP
        JSON5.parse("{\t\"key\" \n:\r\n \"value\"\uFEFF,\u00A0\"next\"\t:\rtrue\n}") shouldBe mapOf("key" to "value", "next" to true)
    }
}
