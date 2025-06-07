package io.github.json5.kotlin

/**
 * Parser for JSON5 syntax
 * Converts JSON5 text into Kotlin objects
 */
internal object JSON5Parser {
    /**
     * Parses a JSON5 string into a Kotlin object.
     *
     * @param text JSON5 text to parse
     * @return The parsed value (Map, List, String, Number, Boolean, or null)
     * @throws JSON5Exception if the input is invalid JSON5
     */
    fun parse(text: String): Any? {
        return parse(text, null)
    }

    /**
     * Parses a JSON5 string into a Kotlin object, with a reviver function.
     *
     * @param text JSON5 text to parse
     * @param reviver A function that transforms the parsed values, or null for no transformation
     * @return The parsed value (Map, List, String, Number, Boolean, or null)
     * @throws JSON5Exception if the input is invalid JSON5
     */
    fun parse(text: String, reviver: ((key: String, value: Any?) -> Any?)? = null): Any? {
        val lexer = JSON5Lexer(text)
        var token = lexer.nextToken()

        // Handle empty input
        if (token.type == TokenType.EOF) {
            throw JSON5Exception("Empty JSON5 input", token.line, token.column)
        }

        val result = parseValue(token, lexer)

        // Check that there's no extra content after the JSON5 value
        token = lexer.nextToken()
        if (token.type != TokenType.EOF) {
            throw JSON5Exception("Unexpected additional content after JSON5 value", token.line, token.column)
        }

        // Apply the reviver function if provided
        return if (reviver != null) {
            internalize(mapOf("" to result), "", reviver)
        } else {
            result
        }
    }

    private fun internalize(holder: Map<String, Any?>, name: String, reviver: (key: String, value: Any?) -> Any?): Any? {
        val value = holder[name]

        // Process objects and arrays recursively
        if (value != null) {
            when (value) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = value as Map<String, Any?>
                    val mutableMap = map.toMutableMap()

                    for (key in map.keys) {
                        val replacement = internalize(map, key, reviver)
                        if (replacement == null && replacement !== map[key]) {
                            mutableMap.remove(key)
                        } else if (replacement !== map[key]) {
                            mutableMap[key] = replacement
                        }
                    }

                    return reviver(name, mutableMap)
                }
                is List<*> -> {
                    val list = value
                    val mutableList = list.toMutableList()

                    for (i in list.indices) {
                        val key = i.toString()
                        val tempHolder = mapOf(key to list[i])
                        val replacement = internalize(tempHolder, key, reviver)

                        if (replacement == null && list[i] != null) {
                            mutableList[i] = null
                        } else if (replacement !== list[i]) {
                            mutableList[i] = replacement
                        }
                    }

                    return reviver(name, mutableList)
                }
                else -> return reviver(name, value)
            }
        }

        return reviver(name, value)
    }

    private fun parseValue(token: Token, lexer: JSON5Lexer): Any? {
        return when (token.type) {
            TokenType.NULL -> null
            TokenType.BOOLEAN -> (token as Token.BooleanToken).boolValue
            TokenType.STRING -> (token as Token.StringToken).stringValue
            TokenType.NUMERIC -> (token as Token.NumericToken).numberValue
            TokenType.PUNCTUATOR -> {
                when ((token as Token.PunctuatorToken).punctuator) {
                    "{" -> parseObject(lexer)
                    "[" -> parseArray(lexer)
                    else -> throw JSON5Exception("Unexpected punctuator: ${token.punctuator}", token.line, token.column)
                }
            }
            TokenType.IDENTIFIER -> throw JSON5Exception("Unexpected identifier: ${(token as Token.IdentifierToken).identifierValue}", token.line, token.column)
            TokenType.EOF -> throw JSON5Exception("Unexpected end of input", token.line, token.column)
        }
    }

    private fun parseObject(lexer: JSON5Lexer): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        var token = lexer.nextToken()

        // Handle empty object
        if (token.type == TokenType.PUNCTUATOR && (token as Token.PunctuatorToken).punctuator == "}") {
            return result
        }

        while (true) {
            // Parse the property name
            val key = when {
                token.type == TokenType.STRING -> (token as Token.StringToken).stringValue
                token.type == TokenType.IDENTIFIER -> (token as Token.IdentifierToken).identifierValue
                token.type == TokenType.PUNCTUATOR && (token as Token.PunctuatorToken).punctuator == "}" ->
                    break // This is for handling empty objects or trailing commas
                else -> throw JSON5Exception("Expected property name or '}'", token.line, token.column)
            }

            // Expect a colon
            token = lexer.nextToken()
            if (token.type != TokenType.PUNCTUATOR || (token as Token.PunctuatorToken).punctuator != ":") {
                throw JSON5Exception("Expected ':' after property name", token.line, token.column)
            }

            // Parse the property value
            token = lexer.nextToken()
            val value = parseValue(token, lexer)

            // Add the property to the object
            result[key] = value

            // Expect a comma or closing brace
            token = lexer.nextToken()
            if (token.type == TokenType.PUNCTUATOR) {
                token as Token.PunctuatorToken
                if (token.punctuator == "}") {
                    break
                } else if (token.punctuator != ",") {
                    throw JSON5Exception("Expected ',' or '}'", token.line, token.column)
                }
            } else {
                throw JSON5Exception("Expected ',' or '}'", token.line, token.column)
            }

            // After a comma, parse the next property or handle trailing comma
            token = lexer.nextToken()
            if (token.type == TokenType.PUNCTUATOR && (token as Token.PunctuatorToken).punctuator == "}") {
                break // Allow trailing comma
            }
        }

        return result
    }

    private fun parseArray(lexer: JSON5Lexer): List<Any?> {
        val result = mutableListOf<Any?>()
        var token = lexer.nextToken()

        // Handle empty array
        if (token.type == TokenType.PUNCTUATOR && (token as Token.PunctuatorToken).punctuator == "]") {
            return result
        }

        while (true) {
            // Parse the element value
            val value = parseValue(token, lexer)

            // Add the element to the array
            result.add(value)

            // Expect a comma or closing bracket
            token = lexer.nextToken()
            if (token.type == TokenType.PUNCTUATOR) {
                token as Token.PunctuatorToken
                if (token.punctuator == "]") {
                    break
                } else if (token.punctuator != ",") {
                    throw JSON5Exception("Expected ',' or ']'", token.line, token.column)
                }
            } else {
                throw JSON5Exception("Expected ',' or ']'", token.line, token.column)
            }

            // After a comma, parse the next element or handle trailing comma
            token = lexer.nextToken()
            if (token.type == TokenType.PUNCTUATOR && (token as Token.PunctuatorToken).punctuator == "]") {
                break // Allow trailing comma
            }
        }

        return result
    }
}
