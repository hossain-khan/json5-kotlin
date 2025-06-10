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
}
