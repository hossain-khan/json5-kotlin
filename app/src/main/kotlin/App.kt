package org.json5.app

import io.github.json5.kotlin.JSON5

fun main() {
    val json5files = listOf(
        "empty-json.json5",
    )

    json5files.forEach { fileName ->
        println("Processing file: $fileName")
        // Load the file from resources using the classloader
        val resourceStream = object {}.javaClass.getResourceAsStream("/$fileName")

        if (resourceStream != null) {
            val content = resourceStream.bufferedReader().use { it.readText() }
            JSON5.parse(content).also { parsed ->
                println("Parsed JSON5: $parsed")
            }
        } else {
            println("Error: Could not find resource: $fileName")
        }
    }
}
