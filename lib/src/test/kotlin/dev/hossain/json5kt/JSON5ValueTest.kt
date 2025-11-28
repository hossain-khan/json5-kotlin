package dev.hossain.json5kt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for JSON5Value parsing functionality.
 * This class tests the strongly-typed JSON5Value API.
 */
@DisplayName("JSON5.parse with JSON5Value")
class JSON5ValueTest {
    @Test
    fun `should parse empty object to JSON5Value`() {
        val result = JSON5.parse("{}")
        result.shouldBeInstanceOf<JSON5Value.Object>()
        val obj = result as JSON5Value.Object
        obj.value shouldBe emptyMap<String, JSON5Value>()
    }

    @Test
    fun `should parse simple object with string value to JSON5Value`() {
        val result = JSON5.parse("""{"key": "value"}""")
        result.shouldBeInstanceOf<JSON5Value.Object>()
        val obj = result as JSON5Value.Object
        obj.value["key"].shouldBeInstanceOf<JSON5Value.String>()
        val str = obj.value["key"] as JSON5Value.String
        str.value shouldBe "value"
    }

    @Test
    fun `should parse simple object with number value to JSON5Value`() {
        val result = JSON5.parse("""{"key": 42}""")
        result.shouldBeInstanceOf<JSON5Value.Object>()
        val obj = result as JSON5Value.Object
        obj.value["key"].shouldBeInstanceOf<JSON5Value.Number.Decimal>()
        val num = obj.value["key"] as JSON5Value.Number.Decimal
        num.value shouldBe 42.0
    }

    @Test
    fun `should parse simple object with boolean value to JSON5Value`() {
        val result = JSON5.parse("""{"key": true}""")
        result.shouldBeInstanceOf<JSON5Value.Object>()
        val obj = result as JSON5Value.Object
        obj.value["key"].shouldBeInstanceOf<JSON5Value.Boolean>()
        val bool = obj.value["key"] as JSON5Value.Boolean
        bool.value shouldBe true
    }

    @Test
    fun `should parse simple object with null value to JSON5Value`() {
        val result = JSON5.parse("""{"key": null}""")
        result.shouldBeInstanceOf<JSON5Value.Object>()
        val obj = result as JSON5Value.Object
        obj.value["key"] shouldBe JSON5Value.Null
    }

    @Test
    fun `should parse simple array to JSON5Value`() {
        val result = JSON5.parse("[1, 2, 3]")
        result.shouldBeInstanceOf<JSON5Value.Array>()
        val arr = result as JSON5Value.Array
        arr.value.size shouldBe 3
        arr.value.forEach { it.shouldBeInstanceOf<JSON5Value.Number.Decimal>() }
        (arr.value[0] as JSON5Value.Number.Decimal).value shouldBe 1.0
        (arr.value[1] as JSON5Value.Number.Decimal).value shouldBe 2.0
        (arr.value[2] as JSON5Value.Number.Decimal).value shouldBe 3.0
    }

    @Test
    fun `should parse special number values to JSON5Value`() {
        var result = JSON5.parse("NaN")
        result shouldBe JSON5Value.Number.NaN

        result = JSON5.parse("Infinity")
        result shouldBe JSON5Value.Number.PositiveInfinity

        result = JSON5.parse("-Infinity")
        result shouldBe JSON5Value.Number.NegativeInfinity
    }

    @Test
    fun `should convert JSON5Value to raw objects using toAny helper`() {
        val result = JSON5.parse("""{"key": "value"}""").toAny()
        result.shouldBeInstanceOf<Map<String, Any?>>()
        val map = result as Map<String, Any?>
        map["key"] shouldBe "value"
    }

    @Test
    fun `should convert Any to JSON5Value using from method`() {
        val kotlinObj = mapOf("key" to "value", "number" to 42.0, "bool" to true, "nullValue" to null)
        val result = JSON5Value.from(kotlinObj)
        result.shouldBeInstanceOf<JSON5Value.Object>()
        val obj = result as JSON5Value.Object

        obj.value["key"].shouldBeInstanceOf<JSON5Value.String>()
        (obj.value["key"] as JSON5Value.String).value shouldBe "value"

        obj.value["number"].shouldBeInstanceOf<JSON5Value.Number.Decimal>()
        (obj.value["number"] as JSON5Value.Number.Decimal).value shouldBe 42.0

        obj.value["bool"].shouldBeInstanceOf<JSON5Value.Boolean>()
        (obj.value["bool"] as JSON5Value.Boolean).value shouldBe true

        obj.value["nullValue"] shouldBe JSON5Value.Null
    }

    @Test
    fun `should throw exception for unsupported types`() {
        shouldThrow<IllegalArgumentException> {
            JSON5Value.from(Thread())
        }
    }

    @Test
    fun `should handle integer types correctly`() {
        // Test with various integer types
        val intResult = JSON5Value.from(42)
        intResult.shouldBeInstanceOf<JSON5Value.Number.Integer>()
        (intResult as JSON5Value.Number.Integer).value shouldBe 42L

        val longResult = JSON5Value.from(42L)
        longResult.shouldBeInstanceOf<JSON5Value.Number.Integer>()
        (longResult as JSON5Value.Number.Integer).value shouldBe 42L
    }

    @Test
    fun `should handle array conversion correctly`() {
        val kotlinList = listOf("hello", 42.0, true, null)
        val result = JSON5Value.from(kotlinList)
        result.shouldBeInstanceOf<JSON5Value.Array>()
        val arr = result as JSON5Value.Array

        arr.value.size shouldBe 4
        arr.value[0].shouldBeInstanceOf<JSON5Value.String>()
        (arr.value[0] as JSON5Value.String).value shouldBe "hello"

        arr.value[1].shouldBeInstanceOf<JSON5Value.Number.Decimal>()
        (arr.value[1] as JSON5Value.Number.Decimal).value shouldBe 42.0

        arr.value[2].shouldBeInstanceOf<JSON5Value.Boolean>()
        (arr.value[2] as JSON5Value.Boolean).value shouldBe true

        arr.value[3] shouldBe JSON5Value.Null
    }

    /**
     * Tests that Float values are correctly converted to JSON5Value.Number.Decimal.
     */
    @Test
    fun `should handle Float types correctly`() {
        val floatResult = JSON5Value.from(3.14f)
        floatResult.shouldBeInstanceOf<JSON5Value.Number.Decimal>()
        (floatResult as JSON5Value.Number.Decimal).value shouldBe 3.14f.toDouble()
    }

    /**
     * Tests that special Double values (Infinity, NaN) are correctly converted to their JSON5Value equivalents.
     */
    @Test
    fun `should handle special Double values correctly`() {
        val posInfResult = JSON5Value.from(Double.POSITIVE_INFINITY)
        posInfResult shouldBe JSON5Value.Number.PositiveInfinity

        val negInfResult = JSON5Value.from(Double.NEGATIVE_INFINITY)
        negInfResult shouldBe JSON5Value.Number.NegativeInfinity

        val nanResult = JSON5Value.from(Double.NaN)
        nanResult shouldBe JSON5Value.Number.NaN
    }

    /**
     * Tests the toString() methods of various JSON5Value types.
     */
    @Test
    fun `should have correct toString representations`() {
        JSON5Value.Object(mapOf("key" to JSON5Value.String("value"))).toString() shouldBe "{key=\"value\"}"
        JSON5Value.Array(listOf(JSON5Value.Number.Decimal(1.0))).toString() shouldBe "[1.0]"
        JSON5Value.String("hello").toString() shouldBe "\"hello\""
        JSON5Value.Number.Integer(42L).toString() shouldBe "42"
        JSON5Value.Number.Decimal(3.14).toString() shouldBe "3.14"
        JSON5Value.Number.Hexadecimal(255L).toString() shouldBe "0xff"
        JSON5Value.Number.PositiveInfinity.toString() shouldBe "Infinity"
        JSON5Value.Number.NegativeInfinity.toString() shouldBe "-Infinity"
        JSON5Value.Number.NaN.toString() shouldBe "NaN"
        JSON5Value.Boolean(true).toString() shouldBe "true"
        JSON5Value.Boolean(false).toString() shouldBe "false"
        JSON5Value.Null.toString() shouldBe "null"
    }

    /**
     * Tests that Boolean values are properly converted.
     */
    @Test
    fun `should handle Boolean conversion from Kotlin boolean`() {
        val trueResult = JSON5Value.from(true)
        trueResult shouldBe JSON5Value.Boolean(true)

        val falseResult = JSON5Value.from(false)
        falseResult shouldBe JSON5Value.Boolean(false)
    }

    /**
     * Tests conversion of nested Map structures.
     */
    @Test
    fun `should handle nested Map conversion`() {
        val nested = mapOf("outer" to mapOf("inner" to "value"))
        val result = JSON5Value.from(nested)
        result.shouldBeInstanceOf<JSON5Value.Object>()
        val obj = result as JSON5Value.Object
        obj.value["outer"].shouldBeInstanceOf<JSON5Value.Object>()
        val innerObj = obj.value["outer"] as JSON5Value.Object
        (innerObj.value["inner"] as JSON5Value.String).value shouldBe "value"
    }

    /**
     * Tests that non-string keys in maps are skipped during conversion.
     */
    @Test
    fun `should skip non-string keys in map conversion`() {
        val mapWithNonStringKey = mapOf(1 to "value", "key" to "otherValue") as Map<Any?, Any?>
        val result = JSON5Value.from(mapWithNonStringKey)
        result.shouldBeInstanceOf<JSON5Value.Object>()
        val obj = result as JSON5Value.Object
        obj.value.size shouldBe 1
        obj.value.containsKey("key") shouldBe true
    }
}
