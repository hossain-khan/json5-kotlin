package io.github.json5.kotlin

/**
 * Represents a token in the JSON5 syntax.
 * Each token has a type, a value (if applicable), and its line and column number in the source.
 *
 * Example usage:
 * ```
 * // Null token: null
 * val nullToken = Token.NullToken(1, 5)
 *
 * // Boolean token: true
 * val trueToken = Token.BooleanToken(true, 2, 3)
 *
 * // String token: "hello"
 * val stringToken = Token.StringToken("hello", 3, 7)
 *
 * // Numeric token: 42, 3.14, 0xFF, Infinity, NaN
 * val intToken = Token.NumericToken(42.0, 4, 2)
 * val floatToken = Token.NumericToken(3.14, 4, 7)
 * val hexToken = Token.NumericToken(255.0, 4, 12)
 * val infToken = Token.NumericToken(Double.POSITIVE_INFINITY, 5, 1)
 * val nanToken = Token.NumericToken(Double.NaN, 5, 10)
 *
 * // Punctuator token: { } [ ] : ,
 * val leftBrace = Token.PunctuatorToken("{", 6, 1)
 * val comma = Token.PunctuatorToken(",", 6, 2)
 *
 * // Identifier token: keyName, _foo, $bar
 * val identToken = Token.IdentifierToken("keyName", 7, 3)
 *
 * // End of file token
 * val eofToken = Token.EOFToken(8, 1)
 * ```
 */
sealed class Token(val type: TokenType, val value: Any?, val line: Int, val column: Int) {
    /**
     * Represents a null literal token.
     * Example: `null`
     */
    class NullToken(line: Int, column: Int) : Token(TokenType.NULL, null, line, column)

    /**
     * Represents a boolean literal token.
     * Example: `true`, `false`
     * @param boolValue The boolean value (true or false)
     */
    class BooleanToken(val boolValue: Boolean, line: Int, column: Int) : Token(TokenType.BOOLEAN, boolValue, line, column)

    /**
     * Represents a string literal token.
     * Example: `"hello"`, `'world'`
     * @param stringValue The string value
     */
    class StringToken(val stringValue: String, line: Int, column: Int) : Token(TokenType.STRING, stringValue, line, column)

    /**
     * Represents a numeric literal token.
     * Example: `123`, `3.14`, `0xFF`, `Infinity`, `NaN`
     * @param numberValue The numeric value as Double
     */
    class NumericToken(val numberValue: Double, line: Int, column: Int) : Token(TokenType.NUMERIC, numberValue, line, column)

    /**
     * Represents a punctuator token.
     * Example: `{`, `}`, `[`, `]`, `:`, `,`
     * @param punctuator The punctuator character as string
     */
    class PunctuatorToken(val punctuator: String, line: Int, column: Int) : Token(TokenType.PUNCTUATOR, punctuator, line, column)

    /**
     * Represents an identifier token (unquoted property names).
     * Example: `foo`, `_bar`, `$baz`
     * @param identifierValue The identifier string
     */
    class IdentifierToken(val identifierValue: String, line: Int, column: Int) : Token(TokenType.IDENTIFIER, identifierValue, line, column)

    /**
     * Represents the end of the input stream.
     * Example: (no more tokens)
     */
    class EOFToken(line: Int, column: Int) : Token(TokenType.EOF, null, line, column)
}

/**
 * Defines the types of tokens in JSON5
 */
enum class TokenType {
    NULL,
    BOOLEAN,
    STRING,
    NUMERIC,
    PUNCTUATOR,
    IDENTIFIER,
    EOF
}
