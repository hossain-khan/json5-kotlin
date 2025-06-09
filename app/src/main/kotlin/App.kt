package org.json5.app

import dev.hossain.json5kt.JSON5

/**
 * Main application entry point for testing JSON5 parsing and serialization.
 */
fun main() {
    val json5files = listOf(
        "simple-object.json5",
        "array-example.json5",
        "numeric-formats.json5",
        "string-and-identifiers.json5",
        "root-string.json5"
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

    // Test serialization and deserialization of Employee model
    testEmployeeSerialization()
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