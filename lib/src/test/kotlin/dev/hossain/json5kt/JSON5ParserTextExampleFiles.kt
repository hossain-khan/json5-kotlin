package dev.hossain.json5kt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Tests the JSON5 parser using various example `.json5` files from the test resources.
 * These tests validate the parser's ability to correctly interpret different JSON5 features
 * by comparing the parsed output against expected Kotlin data structures (Maps and Lists).
 */
class JSON5ParserTextExampleFiles {
    /**
     * Tests parsing of a basic JSON5 object from the `simple-object.json5` resource file.
     * This file includes common data types like strings, numbers, booleans, and a nested object,
     * primarily using quoted keys and standard JSON-like syntax.
     */
    @Test
    fun testParseSimpleObjectJson5() {
        val path = Paths.get("src/test/resources/simple-object.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)

        val expected =
            mapOf(
                "name" to "John Doe",
                "age" to 30.0,
                "isEmployed" to true,
                "salary" to 75000.50,
                "address" to
                    mapOf(
                        "street" to "123 Main St",
                        "city" to "New York",
                        "zipCode" to 10001.0,
                    ),
            )

        assertEquals(expected, result)
    }

    /**
     * Tests parsing of a JSON5 array containing various data types from `array-example.json5`.
     * This includes numbers, strings, booleans, null, a nested object, a nested array,
     * and special numeric values like Infinity and NaN.
     */
    @Test
    fun testParseArrayExampleJson5() {
        val path = Paths.get("src/test/resources/array-example.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)

        val expected =
            listOf(
                42.0,
                "Hello World",
                true,
                null,
                mapOf(
                    "name" to "Nested object",
                    "active" to true,
                ),
                listOf(1.0, 2.0, 3.0),
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NaN,
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

    /**
     * Tests parsing of an empty JSON5 object from `empty-json.json5`.
     * Verifies that an empty but valid JSON5 object string is parsed into an empty Kotlin Map.
     */
    @Test
    fun testParseEmptyJson5() {
        val path = Paths.get("src/test/resources/empty-json.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)
        val expected = emptyMap<String, Any?>()
        assertEquals(expected, result)
    }

    /**
     * Tests parsing of various numeric formats from `numeric-formats.json5`.
     * This includes integers, negative numbers, floats, leading/trailing decimal points,
     * explicit positive signs, hexadecimal numbers, scientific notation,
     * and special values like Infinity and NaN.
     */
    @Test
    fun testParseNumericFormatsJson5() {
        val path = Paths.get("src/test/resources/numeric-formats.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)
        val expected =
            mapOf(
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
                "notANumber" to Double.NaN,
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

    /**
     * Tests parsing of various string formats and identifier types from `string-and-identifiers.json5`.
     * This covers single and double quoted strings, multi-line strings, strings with line/paragraph separators,
     * unquoted identifiers with special characters ($, _), Unicode identifiers, and identifiers with Unicode escapes.
     * It also includes empty objects and arrays as values.
     */
    @Test
    fun testParseStringAndIdentifiersJson5() {
        val path = Paths.get("src/test/resources/string-and-identifiers.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)
        val expected =
            mapOf(
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
                "indentedProperty" to 42.0,
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

    /**
     * Tests parsing of a JSON5 document where the root value is a single string, from `root-string.json5`.
     * JSON5 allows any valid JSON5 value as the root of a document, not just objects or arrays.
     */
    @Test
    fun testParseRootStringJson5() {
        val path = Paths.get("src/test/resources/root-string.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)
        val expected = "This is a valid root-level string in JSON5"
        assertEquals(expected, result)
    }

    /**
     * Tests parsing of a comprehensive JSON5 example from `kitchen-sink.json5`.
     * This file includes a variety of JSON5 features: unquoted keys, single-quoted strings,
     * strings with line breaks, hexadecimal numbers, numbers with leading/trailing decimal points,
     * explicit positive signs, and trailing commas in objects and arrays.
     */
    @Test
    fun testParseKitchenSinkJson5() {
        val path = Paths.get("src/test/resources/kitchen-sink.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)

        val expected =
            mapOf(
                "unquoted" to "and you can quote me on that",
                "singleQuotes" to "I can use \"double quotes\" here",
                "lineBreaks" to "Look, Mom! No \\n's!",
                "hexadecimal" to 0xDECAF.toDouble(),
                "leadingDecimalPoint" to 0.8675309,
                "andTrailing" to 8675309.0,
                "positiveSign" to 1.0,
                "trailingComma" to "in objects",
                "andIn" to listOf("arrays"),
                "backwardsCompatible" to "with JSON",
            )

        assertEquals(expected, result)
    }

    /**
     * Tests parsing of a real-world HarmonyOS IDE hvigor build profile from `harmonyos-ide-hvigor-build-profile-V13.json5`.
     * This file represents a complex build configuration with nested objects, arrays,
     * and extensive use of JSON5 features like comments and trailing commas.
     */
    @Test
    fun testParseHarmonyOSBuildProfileJson5() {
        val path = Paths.get("src/test/resources/harmonyos-ide-hvigor-build-profile-V13.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)

        // Validate basic structure and key properties
        val resultMap = result as Map<*, *>
        assert(resultMap.containsKey("app")) { "Should contain 'app' key" }
        assert(resultMap.containsKey("modules")) { "Should contain 'modules' key" }

        val app = resultMap["app"] as Map<*, *>
        assert(app.containsKey("signingConfigs")) { "App should contain 'signingConfigs'" }
        assert(app.containsKey("products")) { "App should contain 'products'" }
        assert(app.containsKey("buildModeSet")) { "App should contain 'buildModeSet'" }

        val modules = resultMap["modules"] as List<*>
        assert(modules.isNotEmpty()) { "Modules should not be empty" }
        val firstModule = modules.first() as Map<*, *>
        assertEquals("entry", firstModule["name"]) { "First module name should be 'entry'" }
        assertEquals("./entry", firstModule["srcPath"]) { "First module srcPath should be './entry'" }
    }

    /**
     * Tests parsing of a real-world Blink renderer core frame settings from `blink-renderer-core-frame-settings.json5`.
     * This file represents a comprehensive configuration with parameters and data arrays,
     * demonstrating JSON5's capability to handle large, complex configuration files with comments.
     */
    @Test
    fun testParseBlinkRendererFrameSettingsJson5() {
        val path = Paths.get("src/test/resources/blink-renderer-core-frame-settings.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)

        // Validate basic structure and key properties
        val resultMap = result as Map<*, *>
        assert(resultMap.containsKey("parameters")) { "Should contain 'parameters' key" }
        assert(resultMap.containsKey("data")) { "Should contain 'data' key" }

        val parameters = resultMap["parameters"] as Map<*, *>
        assert(parameters.containsKey("type")) { "Parameters should contain 'type'" }
        assert(parameters.containsKey("include_paths")) { "Parameters should contain 'include_paths'" }
        assert(parameters.containsKey("initial")) { "Parameters should contain 'initial'" }
        assert(parameters.containsKey("invalidate")) { "Parameters should contain 'invalidate'" }

        val data = resultMap["data"] as List<*>
        assert(data.isNotEmpty()) { "Data should not be empty" }
        val firstDataItem = data.first() as Map<*, *>
        assertEquals("defaultTextEncodingName", firstDataItem["name"]) { "First data item name should be 'defaultTextEncodingName'" }
        assertEquals("String", firstDataItem["type"]) { "First data item type should be 'String'" }
    }
}
