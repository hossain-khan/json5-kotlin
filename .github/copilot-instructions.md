This is a Kotlin based repository with a JSON5 library for parsing and serializing JSON5 data. It is primarily responsible for handling configuration files and data interchange. Please follow these guidelines when contributing:

[//]: # (Source: https://docs.github.com/en/enterprise-cloud@latest/copilot/using-github-copilot/coding-agent/best-practices-for-using-copilot-to-work-on-tasks)

## Architecture Overview

### Project Structure
- `lib/` - Core JSON5 library module with kotlinx.serialization integration
  - `src/main/kotlin/dev/hossain/json5kt/` - Main library code
  - `src/test/kotlin/dev/hossain/json5kt/` - Unit tests (JUnit 5 + Kotest)
- `app/` - Demo application showing JSON5 library usage
- `benchmark/` - Performance comparison benchmarks vs standard JSON and external libraries
- `buildSrc/` - Gradle convention plugins for shared build logic
- `.github/` - GitHub workflows and Copilot instructions

### Core Components
- `JSON5` - Main entry point for JSON5 serialization/deserialization
- `JSON5Parser` - Parses JSON5 text into structured data
- `JSON5Lexer` - Tokenizes JSON5 input for parsing
- `JSON5Serializer` - Converts objects to JSON5 format
- `JSON5Value` - Represents parsed JSON5 values
- `JSON5Exception` - Handles parsing and serialization errors

## Development Flow

### Essential Commands
- **Build**: `./gradlew build` - Compiles all modules and runs tests
- **Test**: `./gradlew check` - Runs tests, linting, and code coverage
- **Format**: `./gradlew formatKotlin` - Formats Kotlin code using ktlint
- **Run benchmark**: `./gradlew :benchmark:run` - Performance comparison tests

### Build System
- **Gradle 8.14.2** with Kotlin DSL
- **Java 21** toolchain requirement
- **Version catalogs** in `gradle/libs.versions.toml` for dependency management
- **Convention plugins** in `buildSrc/` for shared build configuration

### Dependencies
- **Kotlin 2.1.20** - Target language
- **kotlinx.serialization** - Core serialization framework
- **JUnit 5 + Kotest** - Testing framework
- **Kotlinter** - Code formatting (ktlint wrapper)
- **Kover** - Code coverage analysis

## Code Standards

### Kotlin Best Practices
1. Follow Kotlin idioms and conventions
2. **No wildcard imports** - explicit imports only
3. Use data classes for value objects
4. Prefer immutable data structures
5. Use sealed classes for representing state/results
6. Follow kotlinx.serialization patterns for JSON handling

### Code Organization
- Keep related functionality grouped in the same file/package
- Separate parsing logic from serialization logic
- Use clear, descriptive naming conventions
- Maintain consistent error handling patterns

### Testing Strategy
- **Unit tests** for all public APIs
- **Property-based testing** with Kotest for edge cases
- **Large file tests** for performance validation
- **Benchmark tests** for performance regression detection
- Test both successful parsing and error conditions

### Performance Considerations
- JSON5 parsing is inherently slower than standard JSON
- Optimize for common use cases (config files, small to medium data)
- Use efficient string handling and avoid unnecessary allocations
- Benchmark against external libraries for comparison

## Repository Guidelines

### File Structure
- Keep source files focused on single responsibilities
- Group related test files with source files
- Use descriptive package names following reverse domain notation
- Maintain clear separation between public API and internal implementation

### Documentation
- Update README.md for public API changes
- Add KDoc comments for public APIs
- Include usage examples in documentation
- Keep benchmark README.md updated with latest performance data

### Git Workflow
- Format code before committing: `./gradlew formatKotlin`
- Run full test suite: `./gradlew check`
- Follow conventional commit messages
- Update documentation for API changes

## Troubleshooting

### Common Build Issues
- **Java version**: Ensure Java 21 is available (required by buildSrc)
- **Gradle daemon**: Use `./gradlew --stop` to reset if builds hang
- **Dependencies**: Check `gradle/libs.versions.toml` for version conflicts

### Testing Issues
- Large file tests may be slow - this is expected
- Benchmark tests require consistent system performance
- Property tests may find edge cases - add specific regression tests

### Performance Notes
- JSON5 is ~3-4x slower than standard JSON (expected trade-off)
- External JSON5 libraries may be faster but lack kotlinx.serialization integration
- Focus on correctness first, then optimize hot paths

## Quick Start for Contributors
1. Clone repository
2. Ensure Java 21 is installed
3. Run `./gradlew build` to verify setup
4. Run `./gradlew formatKotlin` before making changes
5. Add tests for new functionality
6. Run `./gradlew check` before submitting PR

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed contribution guidelines.
