package dev.hossain.json5kt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

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
    fun `should parse using parseToAny method`() {
        val result = JSON5.parseToAny("""{"key": "value"}""")
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
}