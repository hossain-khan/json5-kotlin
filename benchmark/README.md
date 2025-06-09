# JSON5 Performance Benchmark - Three-way Comparison

This module provides benchmarking tools to compare the performance of JSON5 serialization/deserialization across three different implementations:

1. **JSON5** (this project) - Uses kotlinx.serialization  
2. **JSON** (kotlinx.serialization) - Standard JSON baseline
3. **External-JSON5** (at.syntaxerror.json5:2.1.0) - External JSON5 library

## Running the Benchmark

To run the complete benchmark suite:

```bash
./gradlew :benchmark:run
```

This will:
1. Warm up the JVM
2. Run serialization and deserialization benchmarks for various data types
3. Generate CSV and summary reports

## Generated Reports

The benchmark generates two types of reports:

### CSV Report
- File: `benchmark_results_YYYY-MM-DD_HH-mm-ss.csv`
- Contains detailed timing data for each test
- Suitable for importing into spreadsheet applications or data analysis tools
- Columns: Operation, DataType, Format, Iterations, TotalTimeNanos, AverageTimeNanos, AverageTimeMicros, AverageTimeMillis

### Summary Report
- File: `benchmark_summary_YYYY-MM-DD_HH-mm-ss.txt`
- Contains human-readable performance comparisons across all three libraries
- Shows relative performance comparisons between each pair of libraries
- Includes overall statistics and rankings

## Test Data Types

The benchmark tests the following data structures:

- **SimplePerson**: Basic data class with name, age, and boolean
- **ComplexPerson**: Complex nested object with address, phone numbers, skills, etc.
- **Company**: Large nested structure with employees and departments
- **NumberTypes**: Various numeric types (int, long, double, float, byte, short)
- **CollectionTypes**: Lists, maps, and nested collections
- **Lists**: Collections of 50-100 complex objects

## Configuration

You can modify the benchmark parameters in `BenchmarkRunner.kt`:

- `iterations`: Number of operations per test (default: 1000)
- `warmupIterations`: Number of warmup iterations (default: 100)

## Running Tests

To run the benchmark module tests:

```bash
./gradlew :benchmark:test
```

## Sample Results

Based on typical runs across the three libraries:

- **JSON** (kotlinx.serialization) is consistently the fastest
- **External-JSON5** performs better than this project's JSON5 implementation  
- **JSON5** (this project) offers kotlinx.serialization integration but with slower performance

Example output:
```
SimplePerson Serialization: JSON5=0.028ms, JSON=0.010ms, External-JSON5=0.011ms
ComplexPerson Serialization: JSON5=0.073ms, JSON=0.018ms, External-JSON5=0.018ms  
Company Serialization: JSON5=0.163ms, JSON=0.031ms, External-JSON5=0.054ms
```

### Benchmark Result Snapshot

| Case                | Type            | JSON5 (ms) | JSON (ms) | External-JSON5 (ms) | Performance Ranking |
| ------------------- | --------------- | ---------- | --------- | ------------------- | ------------------- |
| SimplePerson        | Serialization   | 0.028      | 0.010     | 0.011               | JSON > Ext-JSON5 > JSON5 |
| SimplePerson        | Deserialization | 0.049      | 0.014     | 0.017               | JSON > Ext-JSON5 > JSON5 |
| ComplexPerson       | Serialization   | 0.073      | 0.018     | 0.018               | JSON ≈ Ext-JSON5 > JSON5 |
| ComplexPerson       | Deserialization | 0.101      | 0.021     | 0.020               | Ext-JSON5 > JSON > JSON5 |
| Company             | Serialization   | 0.163      | 0.031     | 0.054               | JSON > Ext-JSON5 > JSON5 |
| Company             | Deserialization | 0.200      | 0.081     | 0.117               | JSON > Ext-JSON5 > JSON5 |

**Overall Performance Comparison:**
- **JSON** is **3.90×** faster than **JSON5** and **2.75×** faster than **External-JSON5**
- **External-JSON5** is **1.42×** faster than **JSON5**

## Key Insights

- **kotlinx.serialization JSON** remains the performance leader
- **External JSON5 library** provides a good balance of JSON5 features with reasonable performance  
- **This project's JSON5** offers seamless kotlinx.serialization integration but at a performance cost
- Choose based on your priorities: performance (JSON), JSON5 features with good performance (External-JSON5), or kotlinx.serialization integration (this project)
