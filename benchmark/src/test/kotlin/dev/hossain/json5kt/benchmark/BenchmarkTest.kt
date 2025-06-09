package dev.hossain.json5kt.benchmark

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for the benchmark module to ensure it works correctly.
 */
class BenchmarkTest {

    @Test
    fun `test data generation works`() {
        val simplePerson = TestDataGenerator.createSimplePerson()
        assertNotNull(simplePerson)
        assertEquals("John Doe", simplePerson.name)
        assertEquals(30, simplePerson.age)
        assertTrue(simplePerson.isActive)
        
        val complexPerson = TestDataGenerator.createComplexPerson()
        assertNotNull(complexPerson)
        assertEquals("Jane", complexPerson.firstName)
        assertNotNull(complexPerson.address)
        assertTrue(complexPerson.phoneNumbers.isNotEmpty())
        
        val company = TestDataGenerator.createCompany()
        assertNotNull(company)
        assertEquals("TechCorp Inc.", company.name)
        assertEquals(10, company.employees.size)
        assertEquals(3, company.departments.size)
    }
    
    @Test
    fun `test benchmark can run without errors`() {
        val benchmark = SerializationBenchmark()
        val testData = TestDataGenerator.createSimplePerson()
        
        // Run a small benchmark to ensure no errors
        val results = benchmark.benchmarkComplete(
            testData, 
            "TestPerson", 
            10, // Small number of iterations for test
            SimplePerson.serializer()
        )
        
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        
        // Should have 4 results: JSON5 and JSON for both serialization and deserialization
        assertEquals(4, results.size)
        
        val json5SerializationResult = results.find { it.format == "JSON5" && it.operation == "Serialization" }
        val jsonSerializationResult = results.find { it.format == "JSON" && it.operation == "Serialization" }
        val json5DeserializationResult = results.find { it.format == "JSON5" && it.operation == "Deserialization" }
        val jsonDeserializationResult = results.find { it.format == "JSON" && it.operation == "Deserialization" }
        
        assertNotNull(json5SerializationResult)
        assertNotNull(jsonSerializationResult)
        assertNotNull(json5DeserializationResult)
        assertNotNull(jsonDeserializationResult)
        
        // Verify basic properties
        assertEquals("TestPerson", json5SerializationResult!!.dataType)
        assertEquals(10, json5SerializationResult.iterations)
        assertTrue(json5SerializationResult.totalTimeNanos > 0)
        assertTrue(json5SerializationResult.averageTimeNanos > 0)
    }
    
    @Test
    fun `test benchmark result CSV generation`() {
        val result = SerializationBenchmark.BenchmarkResult(
            operation = "Serialization",
            dataType = "TestType",
            format = "JSON5",
            iterations = 100,
            totalTimeNanos = 1000000,
            averageTimeNanos = 10000,
            averageTimeMicros = 10.0,
            averageTimeMillis = 0.01
        )
        
        val csvRow = result.toCsvRow()
        assertEquals("Serialization,TestType,JSON5,100,1000000,10000,10.0,0.01", csvRow)
        
        val csvHeader = SerializationBenchmark.BenchmarkResult.csvHeader()
        assertEquals("Operation,DataType,Format,Iterations,TotalTimeNanos,AverageTimeNanos,AverageTimeMicros,AverageTimeMillis", csvHeader)
    }
    
    @Test
    fun `test collection data generators`() {
        val simplePersonList = TestDataGenerator.createSimplePersonList(5)
        assertEquals(5, simplePersonList.size)
        
        val complexPersonList = TestDataGenerator.createComplexPersonList(3)
        assertEquals(3, complexPersonList.size)
        
        val numberTypes = TestDataGenerator.createNumberTypes()
        assertNotNull(numberTypes)
        assertEquals(42, numberTypes.intValue)
        
        val collectionTypes = TestDataGenerator.createCollectionTypes()
        assertNotNull(collectionTypes)
        assertTrue(collectionTypes.stringList.isNotEmpty())
        assertTrue(collectionTypes.stringMap.isNotEmpty())
    }
}