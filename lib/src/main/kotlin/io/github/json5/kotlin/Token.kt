package io.github.json5.kotlin

/**
 * Represents a token in the JSON5 syntax
 */
sealed class Token(val type: TokenType, val value: Any?, val line: Int, val column: Int) {
    class NullToken(line: Int, column: Int) : Token(TokenType.NULL, null, line, column)
    class BooleanToken(val boolValue: Boolean, line: Int, column: Int) : Token(TokenType.BOOLEAN, boolValue, line, column)
    class StringToken(val stringValue: String, line: Int, column: Int) : Token(TokenType.STRING, stringValue, line, column)
    class NumericToken(val numberValue: Double, line: Int, column: Int) : Token(TokenType.NUMERIC, numberValue, line, column)
    class PunctuatorToken(val punctuator: String, line: Int, column: Int) : Token(TokenType.PUNCTUATOR, punctuator, line, column)
    class IdentifierToken(val identifierValue: String, line: Int, column: Int) : Token(TokenType.IDENTIFIER, identifierValue, line, column)
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
