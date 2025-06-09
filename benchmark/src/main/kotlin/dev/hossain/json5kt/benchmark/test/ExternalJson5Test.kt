package dev.hossain.json5kt.benchmark.test

import at.syntaxerror.json5.*

fun main() {
    println("Testing external JSON5 library API...")
    
    try {
        val testJson = "{name: 'John', age: 30}"
        
        // Try JSONParser
        try {
            // Parse JSON5 
            val parser = JSONParser(testJson)
            val parsed = parser.nextValue()
            println("Parsed with JSONParser: $parsed")
            
            // Create JSONObject and stringify
            val testObj = JSONObject()
            testObj.set("name", "Jane")
            testObj.set("age", 25)
            
            val stringified = JSONStringify.toString(testObj, 0)
            println("Stringified with JSONStringify: $stringified")
            
        } catch (e: Exception) {
            println("JSONParser/JSONStringify approach failed: ${e.message}")
            e.printStackTrace()
        }
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}