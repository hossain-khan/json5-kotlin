# Development Guide

This guide provides detailed information for developers working on the json5-kotlin project.

## Table of Contents

- [Environment Setup](#environment-setup)
- [Build System](#build-system)
- [Module Architecture](#module-architecture)
- [Development Tools](#development-tools)
- [Debugging](#debugging)
- [Common Development Tasks](#common-development-tasks)
- [Troubleshooting](#troubleshooting)

## Environment Setup

### Required Software

1. **Java Development Kit (JDK) 21**
   - Download from [Eclipse Temurin](https://adoptium.net/) or use your package manager
   - Verify installation: `java -version`

2. **Git**
   - Required for version control and dependency management (git submodules)
   - Verify installation: `git --version`

### IDE Setup

#### IntelliJ IDEA (Recommended)

1. **Import Project**
   - Open IntelliJ IDEA
   - Select "Open" and choose the project root directory
   - IDEA will automatically detect the Gradle project

2. **Configure SDK**
   - Go to File → Project Structure → Project
   - Set Project SDK to Java 21
   - Set Project language level to 21

3. **Gradle Configuration**
   - Go to File → Settings → Build, Execution, Deployment → Build Tools → Gradle
   - Set "Gradle JVM" to Java 21
   - Use "Gradle Wrapper" for builds

#### VS Code

1. **Install Extensions**
   - Kotlin Language Support
   - Gradle for Java
   - Test Runner for Java

2. **Configure Java Home**
   - Set `java.home` in settings to your Java 21 installation

### Initial Verification

```bash
# Clone the repository
git clone https://github.com/hossain-khan/json5-kotlin.git
cd json5-kotlin

# Verify build works
./gradlew build

# Run all checks
./gradlew check

# Verify formatting
./gradlew formatKotlin
```

## Build System

### Gradle Configuration

The project uses **Gradle 8.14.2** with Kotlin DSL for build configuration.

#### Key Files

- `settings.gradle.kts` - Project structure and repository configuration
- `gradle/libs.versions.toml` - Centralized dependency version management
- `buildSrc/` - Convention plugins for shared build logic
- `gradle.properties` - Gradle configuration properties

#### Version Catalog

Dependencies are managed in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.1.20"
kotlinxSerialization = "1.7.3"
junit = "5.10.1"

[libraries]
kotlinxSerialization = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
kotlinPluginSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

#### Convention Plugins

Shared build logic is in `buildSrc/src/main/kotlin/`:

- `kotlin-jvm.gradle.kts` - Common Kotlin/JVM configuration
- Sets Java 21 toolchain
- Configures test logging
- Applies common plugins

### Gradle Tasks

| Task | Description |
|------|-------------|
| `build` | Compile all modules and run tests |
| `check` | Run tests, linting, and code coverage |
| `formatKotlin` | Format Kotlin code using ktlint |
| `lintKotlin` | Check Kotlin code style |
| `test` | Run unit tests |
| `koverHtmlReport` | Generate code coverage report |
| `publish` | Publish to GitHub Packages (requires credentials) |

## Module Architecture

### lib/ - Core Library

**Source Structure:**
```
lib/src/main/kotlin/dev/hossain/json5kt/
├── JSON5.kt                    # Main API entry point
├── JSON5Parser.kt              # JSON5 text parsing logic
├── JSON5Lexer.kt              # Tokenization
├── JSON5Serializer.kt         # Object to JSON5 serialization
├── JSON5Value.kt              # Parsed value representation
├── JSON5Exception.kt          # Error handling
├── JSON5Format.kt             # Serialization format configuration
└── Token.kt                   # Lexer token definitions
```

**Key Components:**

1. **JSON5** - Main API facade
   - `parseToJsonElement()` - Parse JSON5 to JsonElement
   - `encodeToString()` - Serialize objects to JSON5
   - `decodeFromString()` - Deserialize JSON5 to objects

2. **JSON5Parser** - Core parsing engine
   - Handles JSON5 syntax: comments, unquoted keys, trailing commas
   - Converts tokens to structured data
   - Provides detailed error reporting

3. **JSON5Lexer** - Tokenization
   - Breaks JSON5 text into tokens
   - Handles all JSON5 lexical features
   - Tracks position for error reporting

### app/ - Demo Application

Simple demonstration of library usage with practical examples.

### benchmark/ - Performance Testing

Compares performance against:
- Standard JSON (kotlinx.serialization)
- External JSON5 library (at.syntaxerror.json5)

## Development Tools

### Code Formatting - Kotlinter

Uses [ktlint](https://ktlint.github.io/) via the [Kotlinter Gradle plugin](https://github.com/jeremymailen/kotlinter-gradle).

```bash
# Check formatting
./gradlew lintKotlin

# Auto-format code
./gradlew formatKotlin
```

### Code Coverage - Kover

Uses [Kover](https://github.com/Kotlin/kotlinx-kover) for code coverage analysis.

```bash
# Generate coverage report
./gradlew koverHtmlReport

# View report
open lib/build/reports/kover/html/index.html
```

### Testing Framework

- **JUnit 5** - Test framework
- **Kotest** - Assertions and property-based testing
- **kotlinx.serialization** - For testing serialization integration

## Debugging

### Common Debugging Scenarios

1. **Parsing Errors**
   ```kotlin
   try {
       val result = JSON5.parseToJsonElement(json5Text)
   } catch (e: JSON5Exception) {
       println("Parse error at position ${e.position}: ${e.message}")
   }
   ```

2. **Serialization Issues**
   - Enable debug logging for kotlinx.serialization
   - Check JSON5Format configuration
   - Verify serializer registration

3. **Performance Issues**
   - Use the benchmark module to identify bottlenecks
   - Profile with JProfiler or similar tools
   - Check string allocation patterns

### IDE Debugging Tips

- Set breakpoints in `JSON5Parser.parseValue()`
- Inspect token stream in `JSON5Lexer`
- Use "Evaluate Expression" to test parsing logic
- Enable exception breakpoints for `JSON5Exception`

## Common Development Tasks

### Adding New JSON5 Features

1. **Update Lexer** - Add token recognition in `JSON5Lexer.kt`
2. **Update Parser** - Handle new tokens in `JSON5Parser.kt`
3. **Add Tests** - Cover new functionality thoroughly
4. **Update Documentation** - README and KDoc comments

### Improving Performance

1. **Profile Current Code**
   ```bash
   ./gradlew :benchmark:run
   ```

2. **Identify Bottlenecks**
   - Use JVM profiler
   - Check string allocation patterns
   - Look for unnecessary object creation

3. **Optimize and Verify**
   - Make targeted improvements
   - Re-run benchmarks to verify improvements
   - Ensure tests still pass

### Adding Dependencies

1. **Update Version Catalog**
   ```toml
   # In gradle/libs.versions.toml
   [versions]
   newLibrary = "1.0.0"
   
   [libraries]
   newLibrary = { module = "com.example:library", version.ref = "newLibrary" }
   ```

2. **Add to Module**
   ```kotlin
   // In module's build.gradle.kts
   dependencies {
       implementation(libs.newLibrary)
   }
   ```

## Troubleshooting

### Build Issues

**Java Version Mismatch**
```
Error: Build requires Java 21
```
- Solution: Install Java 21 and update JAVA_HOME
- Verify: `./gradlew --version`

**Gradle Daemon Issues**
```
Error: Build hangs or behaves unexpectedly
```
- Solution: `./gradlew --stop` and retry
- Alternative: Use `--no-daemon` flag

**Dependency Resolution**
```
Error: Could not resolve dependency
```
- Check internet connection
- Verify version catalog entries
- Clear Gradle cache: `rm -rf ~/.gradle/caches`

### Test Issues

**Large File Tests Slow**
- Expected behavior - some test files are large
- Skip with `-x test` if needed for quick builds
- Consider running specific test classes

**Benchmark Inconsistency**
- System load affects benchmark results
- Run multiple times for consistent results
- Close other applications during benchmarking

### IDE Issues

**IntelliJ Not Recognizing Sources**
- File → Invalidate Caches and Restart
- Reimport Gradle project
- Check Project Structure → Modules

**VS Code Kotlin Support**
- Ensure Kotlin extension is installed and enabled
- Reload window after changes
- Check Java extension pack compatibility

### Git Submodule Issues

The project includes JSON5 specification as a submodule:

```bash
# Initialize submodules after cloning
git submodule update --init --recursive

# Update submodules
git submodule update --remote
```

## Advanced Development

### Custom Build Tasks

Add custom Gradle tasks in module `build.gradle.kts`:

```kotlin
tasks.register("customTask") {
    doLast {
        println("Custom build task")
    }
}
```

### Extending the Build

- Add new convention plugins in `buildSrc/`
- Create composite builds for related projects
- Integrate additional code quality tools

### Integration Testing

For testing with external systems:

```kotlin
@Test
fun `integration test with external service`() {
    // Use testcontainers or embedded services
    // Test real-world JSON5 parsing scenarios
}
```

For questions or issues not covered here, please open a GitHub issue or discussion.