# JSON5 vs JSON Performance Benchmark

This module provides benchmarking tools to compare the performance of JSON5 serialization/deserialization against standard JSON using kotlinx.serialization.

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
- Contains human-readable performance comparisons
- Shows which format is faster and by how much
- Includes overall statistics

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

Based on typical runs, JSON standard library generally performs 2-6x faster than JSON5 for both serialization and deserialization, with the performance gap being larger for more complex data structures.

Example output:
```
SimplePerson Serialization: JSON5=0.027ms, JSON=0.013ms
ComplexPerson Serialization: JSON5=0.083ms, JSON=0.015ms  
Company Serialization: JSON5=0.200ms, JSON=0.032ms
```