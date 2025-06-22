# Contributing to json5-kotlin

Thank you for your interest in contributing to json5-kotlin! This guide will help you get started with development and ensure your contributions align with the project's standards.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Code Style](#code-style)
- [Submitting Changes](#submitting-changes)
- [Performance Considerations](#performance-considerations)

## Getting Started

### Prerequisites

- **Java 21** - Required for building the project
- **Git** - For version control
- Basic understanding of Kotlin and kotlinx.serialization

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/hossain-khan/json5-kotlin.git
   cd json5-kotlin
   ```

2. **Verify your environment**
   ```bash
   ./gradlew build
   ```
   This should complete successfully and run all tests.

3. **Run formatting check**
   ```bash
   ./gradlew check
   ```

## Project Structure

```
json5-kotlin/
├── lib/                          # Core JSON5 library
│   ├── src/main/kotlin/          # Library source code
│   └── src/test/kotlin/          # Unit tests
├── app/                          # Demo application
├── benchmark/                    # Performance benchmarks
├── buildSrc/                     # Gradle convention plugins
├── .github/                      # GitHub workflows and documentation
│   ├── copilot-instructions.md   # Copilot agent guidelines
│   └── workflows/                # CI/CD workflows
└── gradle/                       # Gradle configuration
    └── libs.versions.toml        # Dependency version catalog
```

### Key Modules

- **lib**: Contains the core JSON5 parsing and serialization logic
- **app**: Demonstrates library usage with practical examples
- **benchmark**: Performance testing against standard JSON and external JSON5 libraries

## Development Workflow

### Making Changes

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Follow the existing code style and patterns
   - Add tests for new functionality
   - Update documentation as needed

3. **Format your code**
   ```bash
   ./gradlew formatKotlin
   ```

4. **Run tests and checks**
   ```bash
   ./gradlew check
   ```

5. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat: descriptive commit message"
   ```

### Essential Commands

| Command | Purpose |
|---------|---------|
| `./gradlew build` | Build all modules |
| `./gradlew check` | Run tests, linting, and coverage |
| `./gradlew formatKotlin` | Format code with ktlint |
| `./gradlew :lib:test` | Run only library tests |
| `./gradlew :benchmark:run` | Run performance benchmarks |
| `./gradlew koverHtmlReport` | Generate code coverage report |

## Testing

### Test Strategy

1. **Unit Tests**: Cover all public APIs and edge cases
2. **Property Tests**: Use Kotest for comprehensive input validation
3. **Integration Tests**: Test JSON5 features end-to-end
4. **Performance Tests**: Validate performance doesn't regress

### Writing Tests

- Place tests in `lib/src/test/kotlin/` with the same package structure as source
- Use descriptive test names that explain the behavior being tested
- Follow the existing test patterns using JUnit 5 and Kotest
- Test both success and error conditions

Example test structure:
```kotlin
class JSON5ParserTest {
    @Test
    fun `should parse valid JSON5 with comments`() {
        // Given
        val json5 = """
            {
                // A comment
                name: 'test',
                value: 42
            }
        """.trimIndent()
        
        // When
        val result = JSON5.parseToJsonElement(json5)
        
        // Then
        // assertions...
    }
}
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :lib:test

# Run with coverage
./gradlew koverHtmlReport
# View coverage at: lib/build/reports/kover/html/index.html
```

## Code Style

### Kotlin Guidelines

1. **Follow Kotlin coding conventions**
2. **No wildcard imports** - use explicit imports
3. **Use data classes** for value objects
4. **Prefer immutability** where possible
5. **Use meaningful names** for variables and functions

### Formatting

- Code is automatically formatted using **ktlint** via the Kotlinter plugin
- Run `./gradlew formatKotlin` before committing
- Configuration is in the Kotlinter plugin setup

### Documentation

- Add KDoc comments for public APIs
- Include usage examples for complex functionality
- Update README.md for significant changes
- Keep inline comments concise and helpful

## Submitting Changes

### Pull Request Process

1. **Ensure your branch is up to date**
   ```bash
   git fetch origin
   git rebase origin/main
   ```

2. **Verify all checks pass**
   ```bash
   ./gradlew check
   ```

3. **Push your branch**
   ```bash
   git push origin feature/your-feature-name
   ```

4. **Create a Pull Request**
   - Use a descriptive title
   - Include a detailed description of changes
   - Reference any related issues
   - Request review from maintainers

### PR Requirements

- [ ] All tests pass
- [ ] Code is formatted (`./gradlew formatKotlin`)
- [ ] New functionality has tests
- [ ] Documentation is updated if needed
- [ ] No breaking changes without discussion

## Performance Considerations

### JSON5 vs JSON Performance

JSON5 parsing is inherently slower than standard JSON due to additional features:
- Comments processing
- Flexible key syntax (unquoted keys)
- Trailing commas
- Extended number formats

### Optimization Guidelines

1. **Focus on correctness first**, then optimize
2. **Avoid unnecessary string allocations** in hot paths
3. **Use efficient data structures** for parsing state
4. **Profile before optimizing** - use the benchmark module

### Benchmarking

Run benchmarks to ensure changes don't introduce performance regressions:

```bash
./gradlew :benchmark:run
```

The benchmark compares against:
- Standard JSON (kotlinx.serialization)
- External JSON5 library (at.syntaxerror.json5)

## Questions and Support

- **Issues**: Open a GitHub issue for bugs or feature requests
- **Discussions**: Use GitHub Discussions for questions
- **Documentation**: Check existing documentation and code comments

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT License).