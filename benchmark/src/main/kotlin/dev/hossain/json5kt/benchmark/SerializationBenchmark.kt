package dev.hossain.json5kt.benchmark

import dev.hossain.json5kt.JSON5
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
import kotlin.system.measureNanoTime
import at.syntaxerror.json5.*

/**
 * Benchmark class for comparing JSON5 and standard JSON performance.
 */
class SerializationBenchmark {
    
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }
    
    data class BenchmarkResult(
        val operation: String,
        val dataType: String,
        val format: String,
        val iterations: Int,
        val totalTimeNanos: Long,
        val averageTimeNanos: Long,
        val averageTimeMicros: Double,
        val averageTimeMillis: Double
    ) {
        fun toCsvRow(): String {
            return "$operation,$dataType,$format,$iterations,$totalTimeNanos,$averageTimeNanos,$averageTimeMicros,$averageTimeMillis"
        }
        
        companion object {
            fun csvHeader(): String {
                return "Operation,DataType,Format,Iterations,TotalTimeNanos,AverageTimeNanos,AverageTimeMicros,AverageTimeMillis"
            }
        }
    }
    
    /**
     * Converts a kotlinx.serialization object to a plain object that can be used with the external JSON5 library.
     * This is done by serializing to JSON with kotlinx.serialization and then parsing with external JSON5 library.
     */
    private fun <T> convertToPlainObject(data: T, serializer: kotlinx.serialization.SerializationStrategy<T>): Any? {
        val jsonString = json.encodeToString(serializer, data)
        val parser = JSONParser(jsonString)
        return parser.nextValue()
    }
    
    /**
     * Converts a JSON5 string to a plain object using the external JSON5 library.
     */
    private fun parseWithExternalLibrary(json5String: String): Any? {
        val parser = JSONParser(json5String)
        return parser.nextValue()
    }
    
    /**
     * Converts a plain object to JSON5 string using the external JSON5 library.
     */
    private fun stringifyWithExternalLibrary(obj: Any?): String {
        return when (obj) {
            is JSONObject -> JSONStringify.toString(obj, 0)
            is JSONArray -> JSONStringify.toString(obj, 0)
            else -> obj?.toString() ?: "null"
        }
    }
    
    /**
     * Runs serialization benchmark for the given data and serializer.
     */
    fun <T> benchmarkSerialization(
        data: T,
        dataTypeName: String,
        iterations: Int = 1000,
        serializer: kotlinx.serialization.SerializationStrategy<T>
    ): List<BenchmarkResult> {
        val results = mutableListOf<BenchmarkResult>()
        
        // Benchmark JSON5 serialization
        val json5SerializationTime = measureNanoTime {
            repeat(iterations) {
                JSON5.encodeToString(serializer, data)
            }
        }
        
        results.add(
            BenchmarkResult(
                operation = "Serialization",
                dataType = dataTypeName,
                format = "JSON5",
                iterations = iterations,
                totalTimeNanos = json5SerializationTime,
                averageTimeNanos = json5SerializationTime / iterations,
                averageTimeMicros = (json5SerializationTime / iterations) / 1000.0,
                averageTimeMillis = (json5SerializationTime / iterations) / 1_000_000.0
            )
        )
        
        // Benchmark standard JSON serialization
        val jsonSerializationTime = measureNanoTime {
            repeat(iterations) {
                json.encodeToString(serializer, data)
            }
        }
        
        results.add(
            BenchmarkResult(
                operation = "Serialization",
                dataType = dataTypeName,
                format = "JSON",
                iterations = iterations,
                totalTimeNanos = jsonSerializationTime,
                averageTimeNanos = jsonSerializationTime / iterations,
                averageTimeMicros = (jsonSerializationTime / iterations) / 1000.0,
                averageTimeMillis = (jsonSerializationTime / iterations) / 1_000_000.0
            )
        )
        
        // Benchmark external JSON5 library serialization
        val externalJson5Object = convertToPlainObject(data, serializer)
        val externalJson5SerializationTime = measureNanoTime {
            repeat(iterations) {
                stringifyWithExternalLibrary(externalJson5Object)
            }
        }
        
        results.add(
            BenchmarkResult(
                operation = "Serialization",
                dataType = dataTypeName,
                format = "External-JSON5",
                iterations = iterations,
                totalTimeNanos = externalJson5SerializationTime,
                averageTimeNanos = externalJson5SerializationTime / iterations,
                averageTimeMicros = (externalJson5SerializationTime / iterations) / 1000.0,
                averageTimeMillis = (externalJson5SerializationTime / iterations) / 1_000_000.0
            )
        )
        
        return results
    }
    
    /**
     * Runs deserialization benchmark for the given JSON string and type.
     */
    fun <T> benchmarkDeserialization(
        jsonString: String,
        json5String: String,
        dataTypeName: String,
        iterations: Int = 1000,
        deserializer: kotlinx.serialization.DeserializationStrategy<T>
    ): List<BenchmarkResult> {
        val results = mutableListOf<BenchmarkResult>()
        
        // Benchmark JSON5 deserialization
        val json5DeserializationTime = measureNanoTime {
            repeat(iterations) {
                JSON5.decodeFromString(deserializer, json5String)
            }
        }
        
        results.add(
            BenchmarkResult(
                operation = "Deserialization",
                dataType = dataTypeName,
                format = "JSON5",
                iterations = iterations,
                totalTimeNanos = json5DeserializationTime,
                averageTimeNanos = json5DeserializationTime / iterations,
                averageTimeMicros = (json5DeserializationTime / iterations) / 1000.0,
                averageTimeMillis = (json5DeserializationTime / iterations) / 1_000_000.0
            )
        )
        
        // Benchmark standard JSON deserialization
        val jsonDeserializationTime = measureNanoTime {
            repeat(iterations) {
                json.decodeFromString(deserializer, jsonString)
            }
        }
        
        results.add(
            BenchmarkResult(
                operation = "Deserialization",
                dataType = dataTypeName,
                format = "JSON",
                iterations = iterations,
                totalTimeNanos = jsonDeserializationTime,
                averageTimeNanos = jsonDeserializationTime / iterations,
                averageTimeMicros = (jsonDeserializationTime / iterations) / 1000.0,
                averageTimeMillis = (jsonDeserializationTime / iterations) / 1_000_000.0
            )
        )
        
        // Benchmark external JSON5 library deserialization
        val externalJson5DeserializationTime = measureNanoTime {
            repeat(iterations) {
                parseWithExternalLibrary(json5String)
            }
        }
        
        results.add(
            BenchmarkResult(
                operation = "Deserialization",
                dataType = dataTypeName,
                format = "External-JSON5",
                iterations = iterations,
                totalTimeNanos = externalJson5DeserializationTime,
                averageTimeNanos = externalJson5DeserializationTime / iterations,
                averageTimeMicros = (externalJson5DeserializationTime / iterations) / 1000.0,
                averageTimeMillis = (externalJson5DeserializationTime / iterations) / 1_000_000.0
            )
        )
        
        return results
    }
    
    /**
     * Runs complete benchmark (both serialization and deserialization) for given data.
     */
    fun <T> benchmarkComplete(
        data: T,
        dataTypeName: String,
        iterations: Int = 1000,
        serializer: kotlinx.serialization.KSerializer<T>
    ): List<BenchmarkResult> {
        val results = mutableListOf<BenchmarkResult>()
        
        // First serialize to get strings for deserialization benchmark
        val json5String = JSON5.encodeToString(serializer, data)
        val jsonString = json.encodeToString(serializer, data)
        
        // Run serialization benchmarks
        results.addAll(benchmarkSerialization(data, dataTypeName, iterations, serializer))
        
        // Run deserialization benchmarks
        results.addAll(benchmarkDeserialization(jsonString, json5String, dataTypeName, iterations, serializer))
        
        return results
    }
}