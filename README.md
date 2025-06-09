# json5-kotlin
JSON5 implementation for Kotlin/JVM

A robust JSON5 parser and serializer for Kotlin that extends JSON with helpful features like comments, trailing commas, and unquoted keys while maintaining full backward compatibility with JSON.

## Features

JSON5 extends JSON with the following features:

- **Comments**: `// line comments` and `/* block comments */`
- **Trailing commas**: In objects and arrays
- **Unquoted keys**: Object keys can be unquoted if they're valid identifiers
- **Single quotes**: Strings can use single quotes
- **Multi-line strings**: Strings can span multiple lines with `\` at line end
- **Numbers**: Hexadecimal numbers, leading/trailing decimal points, explicit positive signs, infinity, and NaN
- **Backward compatible**: All valid JSON is valid JSON5

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.json5:json5-kotlin:VERSION")
}
```

## Usage

### Basic Parsing and Stringifying

```kotlin
import io.github.json5.kotlin.JSON5

// Parse JSON5 to Kotlin objects
val json5 = """
{
    // Configuration for my app
    name: 'MyApp',
    version: 2,
    features: ['auth', 'analytics',], // trailing comma
}
"""

val parsed = JSON5.parse(json5)
// Returns: Map<String, Any?>

// Stringify Kotlin objects to JSON5
val data = mapOf(
    "name" to "MyApp", 
    "version" to 2,
    "enabled" to true
)
val json5String = JSON5.stringify(data)
// Returns: {name:'MyApp',version:2,enabled:true}
```

### Integration with kotlinx.serialization

```kotlin
import io.github.json5.kotlin.JSON5
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val appName: String,
    val version: Int,
    val features: List<String>,
    val settings: Map<String, String>
)

// Serialize to JSON5
val config = Config(
    appName = "MyApp",
    version = 2,
    features = listOf("auth", "analytics"),
    settings = mapOf("theme" to "dark", "lang" to "en")
)

val json5 = JSON5.encodeToString(Config.serializer(), config)
// Result: {appName:'MyApp',version:2,features:['auth','analytics'],settings:{theme:'dark',lang:'en'}}

// Deserialize from JSON5 (with comments and formatting)
val json5WithComments = """
{
    // Application configuration
    appName: 'MyApp',
    version: 2, // current version
    features: [
        'auth',
        'analytics', // trailing comma OK
    ],
    settings: {
        theme: 'dark',
        lang: 'en',
    }
}
"""

val decoded = JSON5.decodeFromString(Config.serializer(), json5WithComments)
// Returns: Config instance
```

### Advanced Features

```kotlin
// JSON5 supports various number formats
val numbers = JSON5.parse("""
{
    hex: 0xDECAF,
    leadingDot: .8675309,
    trailingDot: 8675309.,
    positiveSign: +1,
    scientific: 6.02e23,
    infinity: Infinity,
    negativeInfinity: -Infinity,
    notANumber: NaN
}
""")

// Multi-line strings and comments
val complex = JSON5.parse("""
{
    multiLine: "This is a \
multi-line string",
    /* Block comment
       spanning multiple lines */
    singleQuoted: 'Can contain "double quotes"',
    unquoted: 'keys work too'
}
""")
```

## Building the Project

This project uses [Gradle](https://gradle.org/) with Java 21:

```bash
./gradlew build    # Build the library
./gradlew test     # Run tests
./gradlew check    # Run all checks including tests
```

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
