package org.json5.app

import dev.hossain.json5kt.JSON5
import dev.hossain.json5kt.JSON5Value
import kotlinx.serialization.Serializable

/**
 * Main application entry point for testing JSON5 parsing and serialization.
 */
fun main() {
    // Test serialization and deserialization of Employee model
    testEmployeeSerialization()

    // Run README sample code validation tests
    println("\n=== README Sample Code Validation ===")
    testBasicParsingAndStringifying()
    testKotlinxSerializationIntegration()
    testAdvancedFeatures()
    testMigrationCompatibility()

    // Test sample JSON5 files from resources
    testSampleJson5Files()
}

private fun testSampleJson5Files() {
    val json5files =
        listOf(
            "simple-object.json5",
            "array-example.json5",
            "numeric-formats.json5",
            "string-and-identifiers.json5",
            "root-string.json5",
        )

    json5files.forEach { fileName ->
        println("\n=== Processing file: $fileName ===")
        // Load the file from resources using the classloader
        val resourceStream = object {}.javaClass.getResourceAsStream("/$fileName")

        if (resourceStream != null) {
            val content = resourceStream.bufferedReader().use { it.readText() }
            println("Content:\n$content")
            try {
                JSON5.parse(content).also { parsed ->
                    println("\nParsed JSON5: $parsed")
                }
            } catch (e: Exception) {
                println("\n⚠️ Error parsing JSON5: ${e.message}")
            }
        } else {
            println("⚠️ Error: Could not find resource: $fileName")
        }
        println("\n===============================\n\n")
    }
}

fun testEmployeeSerialization() {
    val fileName = "employee-example.json5"
    val resourceStream = object {}.javaClass.getResourceAsStream("/$fileName")
    if (resourceStream != null) {
        val content = resourceStream.bufferedReader().use { it.readText() }
        println("\n=== Employee Serialization/Deserialization Test ===")
        println("Original JSON5:\n$content")
        try {
            val employee: Employee = JSON5.decodeFromString(Employee.serializer(), content)
            println("\nDeserialized Employee object: $employee")
            val serialized: String = JSON5.encodeToString(Employee.serializer(), employee)
            println("\nSerialized back to JSON5:\n$serialized")
        } catch (e: Exception) {
            println("\n⚠️ Error during Employee serialization/deserialization: ${e.message}")
        }
        println("\n===============================================\n")
    } else {
        println("⚠️ Error: Could not find resource: $fileName")
    }
}

/**
 * Tests basic parsing and stringifying functionality from README
 */
fun testBasicParsingAndStringifying() {
    println("\n--- Testing Basic Parsing and Stringifying ---")

    try {
        // Parse JSON5 to strongly-typed JSON5Value objects
        val json5 =
            """
            {
                // Configuration for my app
                name: 'MyApp',
                version: 2,
                features: ['auth', 'analytics',], // trailing comma
            }
            """.trimIndent()

        val parsed = JSON5.parse(json5)
        println("✓ Parsed JSON5 successfully: $parsed")

        // Access values in a type-safe way
        when (parsed) {
            is JSON5Value.Object -> {
                val name = parsed.value["name"] as? JSON5Value.String
                val version = parsed.value["version"] as? JSON5Value.Number
                val features = parsed.value["features"] as? JSON5Value.Array

                println("✓ App name: ${name?.value}") // "MyApp"
                println(
                    "✓ Version: ${(version as? JSON5Value.Number.Integer)?.value ?: (version as? JSON5Value.Number.Decimal)?.value?.toInt()}",
                ) // 2
                println("✓ Features: ${features?.value?.map { (it as JSON5Value.String).value }}") // ["auth", "analytics"]
            }
            else -> println("Parsed value is not an object")
        }

        // Stringify Kotlin objects to JSON5
        val data =
            mapOf(
                "name" to "MyApp",
                "version" to 2,
                "enabled" to true,
            )
        val json5String = JSON5.stringify(data)
        println("✓ Stringified to JSON5: $json5String")
    } catch (e: Exception) {
        println("⚠️ Error in basic parsing and stringifying test: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Tests kotlinx.serialization integration from README
 */
fun testKotlinxSerializationIntegration() {
    println("\n--- Testing kotlinx.serialization Integration ---")

    try {
        @Serializable
        data class Config(
            val appName: String,
            val version: Int,
            val features: List<String>,
            val settings: Map<String, String>,
        )

        // Serialize to JSON5
        val config =
            Config(
                appName = "MyApp",
                version = 2,
                features = listOf("auth", "analytics"),
                settings = mapOf("theme" to "dark", "lang" to "en"),
            )

        val json5 = JSON5.encodeToString(Config.serializer(), config)
        println("✓ Serialized to JSON5: $json5")

        // Deserialize from JSON5 (with comments and formatting)
        val json5WithComments =
            """
            {
                // Application configuration
                appName: 'MyApp',
                version: 2, // current version
                features: [
                    'auth',
                    'analytics', // trailing comma OK
                ],
                settings: {
                    theme: 'dark',
                    lang: 'en',
                }
            }
            """.trimIndent()

        val decoded = JSON5.decodeFromString(Config.serializer(), json5WithComments)
        println("✓ Deserialized from JSON5 with comments: $decoded")
    } catch (e: Exception) {
        println("⚠️ Error in kotlinx.serialization integration test: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Tests advanced features from README
 */
fun testAdvancedFeatures() {
    println("\n--- Testing Advanced Features ---")

    try {
        // JSON5 supports various number formats
        val numbers =
            JSON5.parse(
                """
                {
                    hex: 0xDECAF,
                    leadingDot: .8675309,
                    trailingDot: 8675309.,
                    positiveSign: +1,
                    scientific: 6.02e23,
                    infinity: Infinity,
                    negativeInfinity: -Infinity,
                    notANumber: NaN
                }
                """.trimIndent(),
            )

        println("✓ Parsed numbers JSON5 successfully: $numbers")

        // Access different number types
        when (numbers) {
            is JSON5Value.Object -> {
                val hex = numbers.value["hex"] as? JSON5Value.Number
                val infinity = numbers.value["infinity"] as? JSON5Value.Number.PositiveInfinity
                val nan = numbers.value["notANumber"] as? JSON5Value.Number.NaN

                println(
                    "✓ Hex value: ${(hex as? JSON5Value.Number.Hexadecimal)?.value ?: (hex as? JSON5Value.Number.Decimal)?.value?.toLong()}",
                ) // 912559
                println("✓ Is infinity: ${infinity != null}") // true
                println("✓ Is NaN: ${nan != null}") // true
            }
            else -> println("Numbers value is not an object")
        }

        // Multi-line strings and comments
        val complex =
            JSON5.parse(
                """
                {
                    multiLine: "This is a \
                multi-line string",
                    /* Block comment
                       spanning multiple lines */
                    singleQuoted: 'Can contain "double quotes"',
                    unquoted: 'keys work too'
                }
                """.trimIndent(),
            )

        println("✓ Parsed complex JSON5 successfully: $complex")

        // Working with the parsed result
        when (complex) {
            is JSON5Value.Object -> {
                val multiLine = complex.value["multiLine"] as? JSON5Value.String
                val singleQuoted = complex.value["singleQuoted"] as? JSON5Value.String

                println("✓ Multi-line: ${multiLine?.value}")
                println("✓ Single quoted: ${singleQuoted?.value}")
            }
            else -> println("Complex value is not an object")
        }
    } catch (e: Exception) {
        println("⚠️ Error in advanced features test: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Tests migration compatibility helpers from README
 */
fun testMigrationCompatibility() {
    println("\n--- Testing Migration Compatibility ---")

    try {
        // New API - Type-safe approach (recommended)
        val result = JSON5.parse("""{"key": "value"}""")
        when (result) {
            is JSON5Value.Object -> {
                val key = result.value["key"] as? JSON5Value.String
                println("✓ New API result: ${key?.value}") // "value"
            }
            else -> println("Result is not an object")
        }

        // Alternative: Convert to raw objects when needed
        fun JSON5Value.toRawObject(): Any? =
            when (this) {
                is JSON5Value.Null -> null
                is JSON5Value.Boolean -> this.value
                is JSON5Value.String -> this.value
                is JSON5Value.Number.Integer -> this.value.toDouble()
                is JSON5Value.Number.Decimal -> this.value
                is JSON5Value.Number.Hexadecimal -> this.value.toDouble()
                is JSON5Value.Number.PositiveInfinity -> Double.POSITIVE_INFINITY
                is JSON5Value.Number.NegativeInfinity -> Double.NEGATIVE_INFINITY
                is JSON5Value.Number.NaN -> Double.NaN
                is JSON5Value.Object -> this.value.mapValues { it.value.toRawObject() }
                is JSON5Value.Array -> this.value.map { it.toRawObject() }
            }

        // Using the helper for compatibility
        val rawResult = JSON5.parse("""{"key": "value"}""").toRawObject()
        val map = rawResult as Map<String, Any?>
        println("✓ Compatibility helper result: ${map["key"]}") // "value"
    } catch (e: Exception) {
        println("⚠️ Error in migration compatibility test: ${e.message}")
        e.printStackTrace()
    }
}
