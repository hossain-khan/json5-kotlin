This is a Kotlin based repository with a JSON5 library for parsing and serializing JSON5 data. It is primarily responsible for handling configuration files and data interchange. Please follow these guidelines when contributing:

[//]: # (Source: https://docs.github.com/en/enterprise-cloud@latest/copilot/using-github-copilot/coding-agent/best-practices-for-using-copilot-to-work-on-tasks)

## Code Standards

### Development Flow
- Build: `./gradlew build`
- Test: `./gradlew check`
- Format: `./gradlew formatKotlin`

## Repository Structure
- `lib` - Gradle module containing the core JSON5 library code with unit tests
- `app` - Gradle module containing the application code that uses the JSON5 library
- `benchmark` - Gradle module for performance benchmarks

## Key Guidelines
1. Follow Kotlin best practices and idiomatic patterns
2. Do not use wildcard imports
3. Maintain existing code structure and organization
4. Write unit tests for new functionality
5. Format code using `./gradlew formatKotlin` before committing
6. Update project README as needed to reflect changes
