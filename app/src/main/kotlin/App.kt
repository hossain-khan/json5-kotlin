package org.json5.app

import io.github.json5.kotlin.JSON5

fun main() {
    val json5files = listOf(
        "empty-json.json5",
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
}
