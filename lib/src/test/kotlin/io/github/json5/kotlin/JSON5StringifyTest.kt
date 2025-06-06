package io.github.json5.kotlin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("JSON5.stringify")
class JSON5StringifyTest {

    @Test
    fun `should stringify empty object`() {
        JSON5.stringify(mapOf<String, Any?>()) shouldBe "{}"
    }

    @Test
    fun `should stringify simple object with string value`() {
        JSON5.stringify(mapOf("key" to "value")) shouldBe "{key:'value'}"
    }

    @Test
    fun `should stringify simple object with number value`() {
        JSON5.stringify(mapOf("key" to 42)) shouldBe "{key:42}"
        JSON5.stringify(mapOf("key" to 42.5)) shouldBe "{key:42.5}"
    }

    @Test
    fun `should stringify simple object with boolean value`() {
        JSON5.stringify(mapOf("key" to true)) shouldBe "{key:true}"
    }

    @Test
    fun `should stringify simple object with null value`() {
        JSON5.stringify(mapOf("key" to null)) shouldBe "{key:null}"
    }

    @Test
    fun `should stringify empty array`() {
        JSON5.stringify(emptyList<Any?>()) shouldBe "[]"
    }

    @Test
    fun `should stringify array with values`() {
        JSON5.stringify(listOf(1, "string", true, null)) shouldBe "[1,'string',true,null]"
    }

    @Test
    fun `should stringify nested objects and arrays`() {
        val nested = mapOf(
            "object" to mapOf("key" to "value"),
            "array" to listOf(1, 2, 3)
        )
        JSON5.stringify(nested) shouldBe "{object:{key:'value'},array:[1,2,3]}"
    }

    @Test
    fun `should stringify object with non-identifier keys`() {
        val obj = mapOf("special-key" to 1, " " to 2)
        JSON5.stringify(obj) shouldBe "{'special-key':1,' ':2}"
    }

    @Test
    fun `should stringify special number values`() {
        JSON5.stringify(Double.POSITIVE_INFINITY) shouldBe "Infinity"
        JSON5.stringify(Double.NEGATIVE_INFINITY) shouldBe "-Infinity"
        JSON5.stringify(Double.NaN) shouldBe "NaN"
    }

    @Test
    fun `should throw on circular references`() {
        val circular = mutableMapOf<String, Any?>()
        circular["self"] = circular

        shouldThrow<JSON5Exception> {
            JSON5.stringify(circular)
        }
    }

    @Test
    fun `should stringify with indentation when space is provided`() {
        JSON5.stringify(mapOf("key" to "value"), space = 2) shouldBe "{\n  key: 'value'\n}"
    }
}
