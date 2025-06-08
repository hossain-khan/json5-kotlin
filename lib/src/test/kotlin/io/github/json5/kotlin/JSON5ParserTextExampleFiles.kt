package io.github.json5.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class JSON5ParserTextExampleFiles {
    @Test
    fun testParseSimpleObjectJson5() {
        val path = Paths.get("src/test/resources/simple-object.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)

        val expected = mapOf(
            "name" to "John Doe",
            "age" to 30.0,
            "isEmployed" to true,
            "salary" to 75000.50,
            "address" to mapOf(
                "street" to "123 Main St",
                "city" to "New York",
                "zipCode" to 10001.0
            )
        )

        assertEquals(expected, result)
    }

    @Test
    fun testParseArrayExampleJson5() {
        val path = Paths.get("src/test/resources/array-example.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)

        val expected = listOf(
            42.0,
            "Hello World",
            true,
            null,
            mapOf(
                "name" to "Nested object",
                "active" to true
            ),
            listOf(1.0, 2.0, 3.0),
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NaN
        )

        assertEquals(expected.size, (result as List<*>).size)
        for (i in expected.indices) {
            val exp = expected[i]
            val act = result[i]
            if (exp is Double && exp.isNaN()) {
                assert((act as Double).isNaN())
            } else {
                assertEquals(exp, act)
            }
        }
    }

    @Test
    fun testParseEmptyJson5() {
        val path = Paths.get("src/test/resources/empty-json.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)
        val expected = emptyMap<String, Any?>()
        assertEquals(expected, result)
    }

    @Test
    fun testParseNumericFormatsJson5() {
        val path = Paths.get("src/test/resources/numeric-formats.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)
        val expected = mapOf(
            "integer" to 42.0,
            "negative" to -17.0,
            "float" to 3.14159,
            "leadingDecimal" to 0.25,
            "trailingDecimal" to 5.0,
            "positiveSign" to 42.0,
            "hex" to 12648430.0,
            "smallHex" to 255.0,
            "negativeHex" to -123.0,
            "scientific" to 6.02e23,
            "negativeExp" to 1e-10,
            "positiveExp" to 1.5e4,
            "infinite" to Double.POSITIVE_INFINITY,
            "negativeInfinite" to Double.NEGATIVE_INFINITY,
            "notANumber" to Double.NaN
        )
        assertEquals(expected.size, (result as Map<*, *>).size)
        for ((k, v) in expected) {
            val actual = result[k]
            if (v is Double && v.isNaN()) {
                assert((actual as Double).isNaN())
            } else {
                assertEquals(v, actual)
            }
        }
    }

    @Test
    fun testParseStringAndIdentifiersJson5() {
        val path = Paths.get("src/test/resources/string-and-identifiers.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)
        val expected = mapOf(
            "escapes" to """Escaped characters: \b \f \n \r \t \u000B \u0000 \u000F \u00A9""",
            "quotes" to "He said, \"Hello!\"",
            "singleQuotes" to "It's working",
            "multiline" to "This string spans multiple lines",
            "lineSeparators" to "Line\u2028and\u2029paragraph separators",
            "\$special_key" to "Dollar sign prefix",
            "_under_score" to "Underscore prefix",
            "ùñîçødë" to "Unicode property name",
            "abc" to "abc via Unicode escapes",
            "emptyObject" to emptyMap<String, Any?>(),
            "emptyArray" to emptyList<Any?>(),
            "indentedProperty" to 42.0
        )
        val resultMap = result as Map<*, *>
        assertEquals(expected.size, resultMap.size)
        for ((k, v) in expected) {
            val actual = resultMap[k]
            if (v is Double && v.isNaN()) {
                assert((actual as Double).isNaN())
            } else {
                assertEquals(v, actual)
            }
        }
    }

    @Test
    fun testParseRootStringJson5() {
        val path = Paths.get("src/test/resources/root-string.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)
        val expected = "This is a valid root-level string in JSON5"
        assertEquals(expected, result)
    }
}
