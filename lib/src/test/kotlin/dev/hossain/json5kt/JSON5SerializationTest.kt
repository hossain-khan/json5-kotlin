package dev.hossain.json5kt

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for JSON5 kotlinx.serialization integration.
 * This class tests that @Serializable data classes can be encoded/decoded
 * to/from JSON5 format using this library.
 */
@DisplayName("JSON5 kotlinx.serialization")
class JSON5SerializationTest {
    @Serializable
    data class Person(
        val name: String,
        val age: Int,
        val isActive: Boolean = true,
    )

    @Serializable
    data class NestedData(
        val person: Person,
        val tags: List<String>,
    )

    @Serializable
    data class NumberTypes(
        val intValue: Int,
        val longValue: Long,
        val doubleValue: Double,
        val floatValue: Float,
    )

    /**
     * Tests basic serialization of a simple data class to JSON5.
     */
    @Test
    fun `should serialize simple data class to JSON5`() {
        val person = Person("Alice", 30)

        val json5String = JSON5.encodeToString(Person.serializer(), person)

        // JSON5 should use unquoted keys where possible and single quotes for strings
        json5String shouldBe "{name:'Alice',age:30,isActive:true}"
    }

    /**
     * Tests basic deserialization of JSON5 to a data class.
     */
    @Test
    fun `should deserialize JSON5 to data class`() {
        val json5String = "{name:'Bob',age:25,isActive:false}"

        val person = JSON5.decodeFromString(Person.serializer(), json5String)

        person shouldBe Person("Bob", 25, false)
    }

    /**
     * Tests serialization with nested objects and arrays.
     */
    @Test
    fun `should handle nested objects and arrays`() {
        val nested =
            NestedData(
                person = Person("Charlie", 35),
                tags = listOf("dev", "kotlin"),
            )

        val json5String = JSON5.encodeToString(NestedData.serializer(), nested)
        val decoded = JSON5.decodeFromString(NestedData.serializer(), json5String)

        decoded shouldBe nested
    }

    /**
     * Tests various number types to ensure proper handling.
     */
    @Test
    fun `should handle different number types`() {
        val numbers =
            NumberTypes(
                intValue = 42,
                longValue = 1234567890123L,
                doubleValue = 3.14159,
                floatValue = 2.718f,
            )

        val json5String = JSON5.encodeToString(NumberTypes.serializer(), numbers)
        val decoded = JSON5.decodeFromString(NumberTypes.serializer(), json5String)

        decoded shouldBe numbers
    }

    /**
     * Tests that JSON5-specific features like comments are properly ignored during deserialization.
     */
    @Test
    fun `should handle JSON5 features like comments`() {
        val json5String =
            """
            {
                // This is a comment
                name: 'Dave', /* another comment */
                age: 40,
                isActive: true
            }
            """.trimIndent()

        val person = JSON5.decodeFromString(Person.serializer(), json5String)

        person shouldBe Person("Dave", 40, true)
    }

    /**
     * Tests JSON5 trailing commas support.
     */
    @Test
    fun `should handle trailing commas`() {
        val json5String =
            """
            {
                name: 'Eve',
                age: 28,
                isActive: false,
            }
            """.trimIndent()

        val person = JSON5.decodeFromString(Person.serializer(), json5String)

        person shouldBe Person("Eve", 28, false)
    }

    /**
     * Tests JSON5 unquoted property names.
     */
    @Test
    fun `should handle unquoted property names`() {
        val json5String = "{name:'Frank',age:33,isActive:true}"

        val person = JSON5.decodeFromString(Person.serializer(), json5String)

        person shouldBe Person("Frank", 33, true)
    }

    /**
     * Tests decoding with hexadecimal numbers in JSON5.
     */
    @Test
    fun `should handle hexadecimal numbers`() {
        val json5String = "{intValue:0x2A,longValue:0x123456789,doubleValue:3.14,floatValue:2.718}"

        val numbers = JSON5.decodeFromString(NumberTypes.serializer(), json5String)

        numbers.intValue shouldBe 42 // 0x2A = 42
        numbers.longValue shouldBe 0x123456789L
    }

    /**
     * Tests decoding with scientific notation numbers.
     */
    @Test
    fun `should handle scientific notation numbers`() {
        val json5String = "{intValue:1e2,longValue:1e10,doubleValue:1.5e-3,floatValue:2.5e2}"

        val numbers = JSON5.decodeFromString(NumberTypes.serializer(), json5String)

        numbers.intValue shouldBe 100
        numbers.doubleValue shouldBe 0.0015
    }

    /**
     * Tests encoding and decoding with null values.
     */
    @Serializable
    data class NullableData(
        val value: String?,
        val number: Int?,
    )

    @Test
    fun `should handle nullable values`() {
        val data = NullableData(null, null)
        val json5String = JSON5.encodeToString(NullableData.serializer(), data)
        val decoded = JSON5.decodeFromString(NullableData.serializer(), json5String)

        decoded shouldBe data
    }

    /**
     * Tests encoding with empty collections.
     */
    @Serializable
    data class CollectionData(
        val list: List<String>,
        val map: Map<String, Int>,
    )

    @Test
    fun `should handle empty collections`() {
        val data = CollectionData(emptyList(), emptyMap())
        val json5String = JSON5.encodeToString(CollectionData.serializer(), data)
        val decoded = JSON5.decodeFromString(CollectionData.serializer(), json5String)

        decoded shouldBe data
    }

    /**
     * Tests that numbers without decimal points are parsed as integers when expected.
     */
    @Test
    fun `should parse whole numbers as integers`() {
        val json5String = "{intValue:42,longValue:123456789012,doubleValue:100,floatValue:50}"

        val numbers = JSON5.decodeFromString(NumberTypes.serializer(), json5String)

        numbers.intValue shouldBe 42
        numbers.longValue shouldBe 123456789012L
        numbers.doubleValue shouldBe 100.0
        numbers.floatValue shouldBe 50f
    }

    /**
     * Tests decoding with leading decimal point numbers.
     */
    @Test
    fun `should handle leading decimal point numbers`() {
        val json5String = "{intValue:1,longValue:2,doubleValue:.5,floatValue:.25}"

        val numbers = JSON5.decodeFromString(NumberTypes.serializer(), json5String)

        numbers.doubleValue shouldBe 0.5
        numbers.floatValue shouldBe 0.25f
    }

    /**
     * Tests decoding with positive sign prefix.
     */
    @Test
    fun `should handle positive sign prefix`() {
        val json5String = "{intValue:+42,longValue:+100,doubleValue:+3.14,floatValue:+2.5}"

        val numbers = JSON5.decodeFromString(NumberTypes.serializer(), json5String)

        numbers.intValue shouldBe 42
        numbers.doubleValue shouldBe 3.14
    }
}
