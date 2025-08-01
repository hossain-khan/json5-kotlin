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

### Interactive Visualization
- File: `benchmark_visualization.html`
- Self-contained HTML file with interactive charts using Chart.js
- Provides visual comparison of performance across all three libraries
- Includes multiple chart types: bar charts, doughnut charts, and performance ratios
- Can be opened directly in any web browser

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

## Performance Expectations

Based on typical runs across the three libraries:

- **JSON** (kotlinx.serialization) is consistently the fastest (~4-5x faster than JSON5 implementations)
- **External-JSON5** (at.syntaxerror.json5) provides good JSON5 performance (~2x faster than this project)
- **JSON5** (this project) prioritizes kotlinx.serialization integration over raw performance

This performance trade-off is expected because:
- JSON5 parsing requires additional processing for comments, flexible syntax, and extended number formats
- This project integrates with kotlinx.serialization which adds serialization overhead
- External JSON5 libraries may use optimized parsing techniques not compatible with kotlinx.serialization

Example output:
```
SimplePerson Serialization: JSON5=0.031ms, JSON=0.010ms, External-JSON5=0.011ms
ComplexPerson Serialization: JSON5=0.084ms, JSON=0.018ms, External-JSON5=0.033ms  
Company Serialization: JSON5=0.233ms, JSON=0.056ms, External-JSON5=0.073ms
```

### Benchmark Result Snapshot

| Case                | Type            | JSON5 (ms) | JSON (ms) | External-JSON5 (ms) | Performance Ranking |
| ------------------- | --------------- | ---------- | --------- | ------------------- | ------------------- |
| SimplePerson        | Serialization   | 0.031      | 0.010     | 0.011               | JSON > Ext-JSON5 > JSON5 |
| SimplePerson        | Deserialization | 0.045      | 0.013     | 0.017               | JSON > Ext-JSON5 > JSON5 |
| ComplexPerson       | Serialization   | 0.084      | 0.018     | 0.033               | JSON > Ext-JSON5 > JSON5 |
| ComplexPerson       | Deserialization | 0.093      | 0.028     | 0.030               | JSON > Ext-JSON5 > JSON5 |
| Company             | Serialization   | 0.233      | 0.056     | 0.073               | JSON > Ext-JSON5 > JSON5 |
| Company             | Deserialization | 0.242      | 0.082     | 0.143               | JSON > Ext-JSON5 > JSON5 |
| NumberTypes         | Serialization   | 0.031      | 0.004     | 0.008               | JSON > Ext-JSON5 > JSON5 |
| NumberTypes         | Deserialization | 0.019      | 0.002     | 0.009               | JSON > Ext-JSON5 > JSON5 |
| CollectionTypes     | Serialization   | 0.060      | 0.013     | 0.012               | Ext-JSON5 > JSON > JSON5 |
| CollectionTypes     | Deserialization | 0.055      | 0.013     | 0.030               | JSON > Ext-JSON5 > JSON5 |

**Overall Performance Comparison:**
- **JSON** is **4.45×** faster than **JSON5** and **2.51×** faster than **External-JSON5**
- **External-JSON5** is **1.77×** faster than **JSON5**

## Interpreting Results

When choosing between implementations, consider:

- **Use JSON** (kotlinx.serialization) if you need maximum performance and don't require JSON5 features
- **Use External-JSON5** if you need JSON5 features with good performance and don't need kotlinx.serialization integration
- **Use this JSON5 implementation** if you need seamless kotlinx.serialization integration with JSON5 features

The performance differences are most noticeable with:
- Large files (>1MB)
- High-frequency operations (>1000 operations/second)
- Resource-constrained environments

For typical configuration files and small-to-medium data, the performance difference may not be significant compared to the development benefits of kotlinx.serialization integration.

## Key Insights

- **kotlinx.serialization JSON** remains the performance leader for standard JSON operations
- **External JSON5 library** provides a good balance of JSON5 features with reasonable performance
- **This project's JSON5** offers seamless kotlinx.serialization integration but with a performance trade-off
- Performance differences are most significant in high-throughput scenarios
- Choose based on your priorities: performance (JSON), JSON5 features with good performance (External-JSON5), or kotlinx.serialization integration (this project)

> **Note**: These benchmarks focus on raw parsing/serialization performance. In real applications, the convenience and type safety of kotlinx.serialization integration may outweigh the performance difference for many use cases.
