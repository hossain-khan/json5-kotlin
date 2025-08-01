# JSON5 Demo Application

This module contains a demonstration application that showcases the json5-kotlin library's features and capabilities.

## What it demonstrates

- **Basic JSON5 parsing and stringification**
- **kotlinx.serialization integration** with custom data classes
- **Advanced JSON5 features** including:
  - Comments in JSON5 files
  - Trailing commas
  - Unquoted keys
  - Different number formats (hex, scientific notation, special values)
  - Multi-line strings
  - Various string quote styles
- **Real-world JSON5 file processing** from the `src/main/resources` directory
- **Error handling** and validation examples

## Running the demo

To run the demonstration application:

```bash
./gradlew :app:run
```

This will execute various examples showing:
1. Employee serialization/deserialization with kotlinx.serialization
2. All code examples from the README.md validation
3. Processing of sample JSON5 files with different features

## Sample JSON5 files

The `src/main/resources` directory contains example JSON5 files demonstrating:

- `simple-object.json5` - Basic object with comments and mixed quote styles
- `array-example.json5` - Arrays with different data types and trailing commas
- `numeric-formats.json5` - Various number formats supported by JSON5
- `string-and-identifiers.json5` - String escape sequences and special identifiers
- `root-string.json5` - Root-level string value (valid in JSON5)

## Use as a testing ground

This app module serves as:
- A **validation tool** to ensure the library works correctly
- A **reference implementation** showing best practices
- A **testing ground** for experimenting with JSON5 features
- **Living documentation** of the library's capabilities

The code in this module is designed to be readable and educational, providing practical examples of how to use the json5-kotlin library in real applications.