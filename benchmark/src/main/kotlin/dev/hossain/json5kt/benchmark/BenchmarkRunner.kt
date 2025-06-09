package dev.hossain.json5kt.benchmark

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main class to run JSON5 vs JSON serialization benchmarks and generate CSV reports.
 */
object BenchmarkRunner {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("Starting JSON5 vs JSON Serialization Benchmarks...")
        println("===============================================")
        
        val benchmark = SerializationBenchmark()
        val allResults = mutableListOf<SerializationBenchmark.BenchmarkResult>()
        
        // Configuration
        val iterations = 1000
        val warmupIterations = 100
        
        println("Configuration:")
        println("- Iterations per test: $iterations")
        println("- Warmup iterations: $warmupIterations")
        println()
        
        // Warmup JVM
        println("Warming up JVM...")
        runWarmup(benchmark, warmupIterations)
        
        // Run benchmarks for different data types
        println("Running benchmarks...")
        
        // Simple Person
        println("Benchmarking SimplePerson...")
        val simplePerson = TestDataGenerator.createSimplePerson()
        allResults.addAll(benchmark.benchmarkComplete(simplePerson, "SimplePerson", iterations, SimplePerson.serializer()))
        
        // Complex Person
        println("Benchmarking ComplexPerson...")
        val complexPerson = TestDataGenerator.createComplexPerson()
        allResults.addAll(benchmark.benchmarkComplete(complexPerson, "ComplexPerson", iterations, ComplexPerson.serializer()))
        
        // Company (large nested object)
        println("Benchmarking Company...")
        val company = TestDataGenerator.createCompany()
        allResults.addAll(benchmark.benchmarkComplete(company, "Company", iterations, Company.serializer()))
        
        // Number Types
        println("Benchmarking NumberTypes...")
        val numberTypes = TestDataGenerator.createNumberTypes()
        allResults.addAll(benchmark.benchmarkComplete(numberTypes, "NumberTypes", iterations, NumberTypes.serializer()))
        
        // Collection Types
        println("Benchmarking CollectionTypes...")
        val collectionTypes = TestDataGenerator.createCollectionTypes()
        allResults.addAll(benchmark.benchmarkComplete(collectionTypes, "CollectionTypes", iterations, CollectionTypes.serializer()))
        
        // List of Simple Persons (100 items)
        println("Benchmarking List of 100 SimplePersons...")
        val simplePersonList = TestDataGenerator.createSimplePersonList(100)
        allResults.addAll(benchmark.benchmarkComplete(simplePersonList, "SimplePersonList100", iterations, kotlinx.serialization.builtins.ListSerializer(SimplePerson.serializer())))
        
        // List of Complex Persons (50 items)
        println("Benchmarking List of 50 ComplexPersons...")
        val complexPersonList = TestDataGenerator.createComplexPersonList(50)
        allResults.addAll(benchmark.benchmarkComplete(complexPersonList, "ComplexPersonList50", iterations, kotlinx.serialization.builtins.ListSerializer(ComplexPerson.serializer())))
        
        // Generate reports
        println()
        println("Generating reports...")
        generateCsvReport(allResults)
        generateSummaryReport(allResults)
        
        println("Benchmarks completed!")
    }
    
    private fun runWarmup(benchmark: SerializationBenchmark, warmupIterations: Int) {
        val warmupData = TestDataGenerator.createSimplePerson()
        benchmark.benchmarkComplete(warmupData, "Warmup", warmupIterations, SimplePerson.serializer())
    }
    
    private fun generateCsvReport(results: List<SerializationBenchmark.BenchmarkResult>) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val filename = "benchmark_results_$timestamp.csv"
        
        File(filename).writeText(
            buildString {
                appendLine(SerializationBenchmark.BenchmarkResult.csvHeader())
                results.forEach { result ->
                    appendLine(result.toCsvRow())
                }
            }
        )
        
        println("CSV report generated: $filename")
    }
    
    private fun generateSummaryReport(results: List<SerializationBenchmark.BenchmarkResult>) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val filename = "benchmark_summary_$timestamp.txt"
        
        val report = buildString {
            appendLine("JSON5 vs JSON Serialization Benchmark Summary")
            appendLine("===========================================")
            appendLine("Generated: ${LocalDateTime.now()}")
            appendLine()
            
            // Group results by data type and operation
            val groupedResults = results.groupBy { "${it.dataType}_${it.operation}" }
            
            groupedResults.forEach { (key, resultList) ->
                val (dataType, operation) = key.split("_")
                appendLine("$dataType - $operation:")
                
                val json5Result = resultList.find { it.format == "JSON5" }
                val jsonResult = resultList.find { it.format == "JSON" }
                
                if (json5Result != null && jsonResult != null) {
                    appendLine("  JSON5: ${String.format("%.3f", json5Result.averageTimeMillis)} ms avg")
                    appendLine("  JSON:  ${String.format("%.3f", jsonResult.averageTimeMillis)} ms avg")
                    
                    val speedup = json5Result.averageTimeMillis / jsonResult.averageTimeMillis
                    if (speedup > 1.0) {
                        appendLine("  → JSON is ${String.format("%.2f", speedup)}x faster than JSON5")
                    } else {
                        appendLine("  → JSON5 is ${String.format("%.2f", 1.0 / speedup)}x faster than JSON")
                    }
                }
                appendLine()
            }
            
            // Overall statistics
            appendLine("Overall Statistics:")
            val json5Results = results.filter { it.format == "JSON5" }
            val jsonResults = results.filter { it.format == "JSON" }
            
            val avgJson5Time = json5Results.map { it.averageTimeMillis }.average()
            val avgJsonTime = jsonResults.map { it.averageTimeMillis }.average()
            
            appendLine("Average JSON5 time: ${String.format("%.3f", avgJson5Time)} ms")
            appendLine("Average JSON time:  ${String.format("%.3f", avgJsonTime)} ms")
            
            val overallSpeedup = avgJson5Time / avgJsonTime
            if (overallSpeedup > 1.0) {
                appendLine("Overall: JSON is ${String.format("%.2f", overallSpeedup)}x faster than JSON5")
            } else {
                appendLine("Overall: JSON5 is ${String.format("%.2f", 1.0 / overallSpeedup)}x faster than JSON")
            }
        }
        
        File(filename).writeText(report)
        println("Summary report generated: $filename")
        
        // Also print summary to console
        println()
        println("=== BENCHMARK SUMMARY ===")
        results.groupBy { "${it.dataType}_${it.operation}" }.forEach { (key, resultList) ->
            val (dataType, operation) = key.split("_")
            val json5Result = resultList.find { it.format == "JSON5" }
            val jsonResult = resultList.find { it.format == "JSON" }
            
            if (json5Result != null && jsonResult != null) {
                println("$dataType $operation: JSON5=${String.format("%.3f", json5Result.averageTimeMillis)}ms, JSON=${String.format("%.3f", jsonResult.averageTimeMillis)}ms")
            }
        }
    }
}