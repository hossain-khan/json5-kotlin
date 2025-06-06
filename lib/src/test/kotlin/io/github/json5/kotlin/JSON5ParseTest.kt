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
}
