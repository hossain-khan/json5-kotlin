package io.github.json5.kotlin

import kotlin.math.pow

/**
 * Lexer for JSON5 syntax
 * Breaks JSON5 text into tokens for the parser
 */
class JSON5Lexer(private val source: String) {
    private var pos: Int = 0
    private var line: Int = 1
    private var column: Int = 1 // Starting column at 1 to match JavaScript implementation
    private var currentChar: Char? = null

    init {
        // Initialize the first character
        currentChar = if (source.isNotEmpty()) source[0] else null
    }

    /**
     * Returns the next token from the input
     */
    fun nextToken(): Token {
        skipWhitespace()
        skipComments()

        if (currentChar == null) {
            return Token.EOFToken(line, column)
        }

        return when (currentChar) {
            '{', '}', '[', ']', ':', ',' -> {
                val token = Token.PunctuatorToken(currentChar.toString(), line, column)
                advance()
                token
            }
            '"', '\'' -> readString()
            'n' -> readNull()
            't' -> readTrue()
            'f' -> readFalse()
            'I' -> readInfinity()
            'N' -> readNaN()
            '+', '-' -> {
                if (peek() == 'I') {
                    // Handle -Infinity
                    val sign = currentChar
                    val startColumn = column
                    advance()
                    if (source.substring(pos, minOf(pos + 8, source.length)) == "Infinity") {
                        repeat(8) { advance() }
                        return Token.NumericToken(if (sign == '-') Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY, line, startColumn)
                    } else if (source.substring(pos, minOf(pos + 3, source.length)) == "NaN") {
                        // Handle -NaN (technically the same as NaN)
                        repeat(3) { advance() }
                        return Token.NumericToken(Double.NaN, line, startColumn)
                    }
                    // Not Infinity/NaN, revert and continue with normal number parsing
                    pos -= 1
                    if (currentChar == '\n') {
                        line -= 1
                        // Need to find last column position - but in this case we're just handling Infinity/NaN
                        column = 1
                    } else {
                        column -= 1
                    }
                    currentChar = sign
                }
                readNumber()
            }
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> readNumber()
            '\\' -> {
                // Handle Unicode escape sequences in identifiers
                val startColumn = column
                advance() // Skip the backslash

                if (currentChar != 'u') {
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                }

                advance() // Skip 'u'
                val hexDigits = StringBuilder()
                repeat(4) {
                    if (currentChar == null || !currentChar!!.isHexDigit()) {
                        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                    }
                    hexDigits.append(currentChar)
                    advance()
                }

                val char = hexDigits.toString().toInt(16).toChar()
                if (!isIdentifierStart(char)) {
                    throw JSON5Exception.invalidIdentifierChar(line, startColumn)
                }

                val buffer = StringBuilder().append(char)

                // Continue reading the rest of the identifier
                while (true) {
                    if (currentChar == '\\') {
                        val continueColumn = column
                        advance() // Skip backslash
                        if (currentChar != 'u') {
                            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                        }
                        advance() // Skip 'u'

                        val identHexDigits = StringBuilder()
                        repeat(4) {
                            if (currentChar == null || !currentChar!!.isHexDigit()) {
                                throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                            }
                            identHexDigits.append(currentChar)
                            advance()
                        }

                        val continueChar = identHexDigits.toString().toInt(16).toChar()
                        if (!isIdentifierPart(continueChar)) {
                            throw JSON5Exception.invalidIdentifierChar(line, continueColumn)
                        }

                        buffer.append(continueChar)
                    } else if (currentChar != null && isIdentifierPart(currentChar)) {
                        buffer.append(currentChar)
                        advance()
                    } else {
                        break
                    }
                }

                return Token.IdentifierToken(buffer.toString(), line, startColumn)
            }
            '/' -> {
                // Handle incomplete comments
                val startColumn = column
                val lookAhead = peek()
                if (lookAhead == null) {
                    advance()
                    throw JSON5Exception.invalidChar('/', line, startColumn)
                }
                if (lookAhead != '/' && lookAhead != '*') {
                    advance()
                    throw JSON5Exception.invalidChar('/', line, startColumn)
                }
                throw JSON5Exception.invalidChar(lookAhead, line, column + 1)
            }
            else -> {
                if (isIdentifierStart(currentChar)) {
                    readIdentifier()
                } else {
                    val c = currentChar ?: ' '
                    val startColumn = column
                    advance()
                    throw JSON5Exception.invalidChar(c, line, startColumn)
                }
            }
        }
    }

    private fun isIdentifierStart(c: Char?): Boolean {
        if (c == null) return false
        return c == '$' || c == '_' || c.isLetter()
    }

    private fun isIdentifierPart(c: Char?): Boolean {
        if (c == null) return false
        return c == '$' || c == '_' || c.isLetterOrDigit() || c == '\u200C' || c == '\u200D'
    }

    private fun advance() {
        pos++
        if (pos < source.length) {
            currentChar = source[pos]
            if (currentChar == '\n') {
                line++
                column = 1 // Reset to 1 when we encounter a newline
            } else {
                column++
            }
        } else {
            currentChar = null
        }
    }

    private fun peek(): Char? {
        val peekPos = pos + 1
        return if (peekPos < source.length) source[peekPos] else null
    }

    private fun skipWhitespace() {
        while (currentChar != null && isWhitespace(currentChar!!)) {
            advance()
        }
    }

    private fun isWhitespace(c: Char): Boolean {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' ||
               c == '\u00A0' || c == '\u2028' || c == '\u2029' ||
               c == '\u000B' || c == '\u000C' || c == '\uFEFF' ||
               // Include other Unicode space separators
               c.category == CharCategory.SPACE_SEPARATOR
    }

    private fun skipComments() {
        if (currentChar == '/' && peek() == '/') {
            // Skip single-line comment
            advance() // Skip first '/'
            advance() // Skip second '/'

            // Read until end of line or end of input
            while (currentChar != null && currentChar != '\n') {
                advance()
            }
            skipWhitespace()
        } else if (currentChar == '/' && peek() == '*') {
            // Skip multi-line comment
            advance() // Skip '/'
            advance() // Skip '*'

            while (currentChar != null) {
                if (currentChar == '*' && peek() == '/') {
                    advance() // Skip '*'
                    advance() // Skip '/'
                    break
                }
                advance()
            }
            if (currentChar == null) {
                throw JSON5Exception.invalidEndOfInput(line, column)
            }
            skipWhitespace()
        }
    }

    private fun readString(): Token.StringToken {
        val startColumn = column
        val startLine = line
        val quoteChar = currentChar
        advance() // Skip the quote character

        val buffer = StringBuilder()
        var done = false

        while (!done && currentChar != null) {
            when (currentChar) {
                quoteChar -> {
                    done = true
                    advance() // Skip the closing quote
                }
                '\\' -> {
                    advance() // Skip the backslash
                    buffer.append(readEscapeSequence())
                }
                '\n', '\r' -> throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                else -> {
                    buffer.append(currentChar)
                    advance()
                }
            }
        }

        if (!done) {
            throw JSON5Exception.invalidEndOfInput(startLine, startColumn + 1)
        }

        return Token.StringToken(buffer.toString(), startLine, startColumn)
    }

    private fun readEscapeSequence(): Char {
        val escapeCol = column

        when (currentChar) {
            'b' -> {
                advance()
                return '\b'
            }
            'f' -> {
                advance()
                return '\u000C'
            }
            'n' -> {
                advance()
                return '\n'
            }
            'r' -> {
                advance()
                return '\r'
            }
            't' -> {
                advance()
                return '\t'
            }
            'v' -> {
                advance()
                return '\u000B'
            }
            '0' -> {
                advance()
                return '\u0000'
            }
            '\\' -> {
                advance()
                return '\\'
            }
            '\'' -> {
                advance()
                return '\''
            }
            '"' -> {
                advance()
                return '"'
            }
            'a' -> {
                advance()
                return '\u0007' // Bell character
            }
            '\n' -> {
                advance()
                return '\u0000' // Line continuation returns nothing visible
            }
            '\r' -> {
                advance()
                if (currentChar == '\n') advance()
                return '\u0000' // Line continuation returns nothing visible
            }
            '\u2028', '\u2029' -> {
                advance()
                return '\u0000' // Line continuation with line/paragraph separator
            }
            'x' -> {
                advance()
                try {
                    return readHexEscape(2)
                } catch (e: Exception) {
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                }
            }
            'u' -> {
                advance()
                try {
                    return readHexEscape(4)
                } catch (e: Exception) {
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                }
            }
            null -> throw JSON5Exception.invalidEndOfInput(line, escapeCol)
            in '1'..'9' -> throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
            else -> {
                // Just return the character after the backslash (e.g. for \', \", etc.)
                val c = currentChar
                advance()
                return c ?: throw JSON5Exception.invalidEndOfInput(line, escapeCol)
            }
        }
    }

    private fun readNull(): Token {
        val startColumn = column
        val startLine = line

        // Verify that the characters spell "null"
        if (source.substring(pos, minOf(pos + 4, source.length)) == "null") {
            repeat(4) { advance() }
            return Token.NullToken(startLine, startColumn)
        }

        throw JSON5Exception("Unexpected identifier", startLine, startColumn)
    }

    private fun readTrue(): Token {
        val startColumn = column
        val startLine = line

        // Verify that the characters spell "true"
        if (source.substring(pos, minOf(pos + 4, source.length)) == "true") {
            repeat(4) { advance() }
            return Token.BooleanToken(true, startLine, startColumn)
        } else {
            val c = source.getOrNull(pos + 3)
            if (c != null && pos + 3 < source.length) {
                throw JSON5Exception.invalidChar(c, line, column + 3)
            }
        }

        throw JSON5Exception("Unexpected identifier", startLine, startColumn)
    }

    private fun readFalse(): Token {
        val startColumn = column
        val startLine = line

        // Verify that the characters spell "false"
        if (source.substring(pos, minOf(pos + 5, source.length)) == "false") {
            repeat(5) { advance() }
            return Token.BooleanToken(false, startLine, startColumn)
        }

        throw JSON5Exception("Unexpected identifier", startLine, startColumn)
    }

    private fun readInfinity(): Token {
        val startColumn = column
        val startLine = line

        // Verify that the characters spell "Infinity"
        if (source.substring(pos, minOf(pos + 8, source.length)) == "Infinity") {
            repeat(8) { advance() }
            return Token.NumericToken(Double.POSITIVE_INFINITY, startLine, startColumn)
        }

        throw JSON5Exception("Unexpected identifier", startLine, startColumn)
    }

    private fun readNaN(): Token {
        val startColumn = column
        val startLine = line

        // Verify that the characters spell "NaN"
        if (source.substring(pos, minOf(pos + 3, source.length)) == "NaN") {
            repeat(3) { advance() }
            return Token.NumericToken(Double.NaN, startLine, startColumn)
        }

        throw JSON5Exception("Unexpected identifier", startLine, startColumn)
    }

    private fun readNumber(): Token.NumericToken {
        val startColumn = column
        val startLine = line
        val buffer = StringBuilder()
        var isNegative = false

        // Handle sign
        if (currentChar == '+') {
            buffer.append('+')
            advance() // Skip '+'
        } else if (currentChar == '-') {
            isNegative = true
            buffer.append('-')
            advance() // Skip '-'
        }

        // Handle number following sign
        if (currentChar == null || (!currentChar!!.isDigit() && currentChar != '.')) {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }

        // Handle hexadecimal notation
        if (currentChar == '0' && (peek() == 'x' || peek() == 'X')) {
            buffer.append('0')
            advance() // Skip '0'
            buffer.append(currentChar)
            advance() // Skip 'x' or 'X'

            // Read hex digits
            var hasDigits = false
            while (currentChar != null && currentChar!!.isHexDigit()) {
                buffer.append(currentChar)
                hasDigits = true
                advance()
            }

            if (!hasDigits) {
                throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
            }

            try {
                // Parse the hex number manually instead of using toDouble()
                val hexStr = buffer.toString()
                val value = if (isNegative) {
                    -parseHexToDouble(hexStr.substring(3)) // skip "-0x"
                } else {
                    parseHexToDouble(hexStr.substring(2)) // skip "0x"
                }
                return Token.NumericToken(value, startLine, startColumn)
            } catch (e: NumberFormatException) {
                throw JSON5Exception("Invalid hexadecimal number", line, column)
            }
        }

        // Handle decimal notation

        // Integer part (optional if there's a decimal point)
        var hasIntegerPart = false
        if (currentChar?.isDigit() == true) {
            hasIntegerPart = true
            while (currentChar != null && currentChar!!.isDigit()) {
                buffer.append(currentChar)
                advance()
            }
        }

        // Decimal point and fraction part
        var hasFractionPart = false
        if (currentChar == '.') {
            buffer.append('.')
            advance()

            hasFractionPart = true
            while (currentChar != null && currentChar!!.isDigit()) {
                buffer.append(currentChar)
                advance()
            }
        }

        // Exponent part
        var hasExponentPart = false
        if (currentChar == 'e' || currentChar == 'E') {
            buffer.append(currentChar)

            // Save position for error reporting
            val eColumn = column
            advance()

            if (currentChar == '+' || currentChar == '-') {
                buffer.append(currentChar)

                // Save position for error reporting
                val signColumn = column
                advance()

                // Check for invalid character after exponent sign
                if (currentChar == null || !currentChar!!.isDigit()) {
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                }
            } else if (currentChar == null || !currentChar!!.isDigit()) {
                throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
            }

            var hasExponentDigits = false
            while (currentChar != null && currentChar!!.isDigit()) {
                buffer.append(currentChar)
                hasExponentDigits = true
                advance()
            }

            if (!hasExponentDigits) {
                throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
            }

            hasExponentPart = true
        }

        // Must have at least one part (integer, fraction, or starts with a decimal point)
        if (!(hasIntegerPart || hasFractionPart) || (hasFractionPart && !hasIntegerPart && buffer.length == 1)) {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }

        val value = buffer.toString().toDouble()
        return Token.NumericToken(value, startLine, startColumn)
    }

    private fun parseHexToDouble(hexStr: String): Double {
        // For very large hex numbers, we need to use a manual approach that mimics JavaScript
        // behavior to ensure consistent results across platforms

        // If the hex string is too long, we need to process it carefully to avoid precision issues
        if (hexStr.length > 12) {
            // Parse the hex string in chunks to avoid overflow
            val upperHex = hexStr.substring(0, hexStr.length - 8)
            val lowerHex = hexStr.substring(hexStr.length - 8)

            // Convert each chunk to a double and combine them
            val upperValue = upperHex.toULong(16).toDouble()
            val lowerValue = lowerHex.toULong(16).toDouble()

            // Combine the chunks (upper * 2^32 + lower)
            return upperValue * 2.0.pow(32) + lowerValue
        }

        // For shorter hex strings, direct conversion works fine
        return hexStr.toULong(16).toDouble()
    }

    private fun readIdentifier(): Token {
        val startColumn = column
        val buffer = StringBuilder()

        while (currentChar != null && isIdentifierPart(currentChar)) {
            buffer.append(currentChar)
            advance()
        }

        return Token.IdentifierToken(buffer.toString(), line, startColumn)
    }

    private fun readHexEscape(digits: Int): Char {
        val hexString = StringBuilder()
        repeat(digits) {
            if (currentChar == null) {
                throw JSON5Exception.invalidEndOfInput(line, column)
            }
            if (!currentChar!!.isHexDigit()) {
                throw JSON5Exception.invalidChar(currentChar!!, line, column)
            }
            hexString.append(currentChar)
            advance()
        }
        return hexString.toString().toInt(16).toChar()
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
