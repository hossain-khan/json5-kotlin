package io.github.json5.kotlin

/**
 * Lexer for JSON5 syntax
 * Breaks JSON5 text into tokens for the parser
 */
class JSON5Lexer(private val source: String) {
    private var pos: Int = 0
    private var line: Int = 1
    private var column: Int = 0
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
                    advance()
                    if (source.substring(pos, minOf(pos + 8, source.length)) == "Infinity") {
                        repeat(8) { advance() }
                        return Token.NumericToken(if (sign == '-') Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY, line, column)
                    } else if (source.substring(pos, minOf(pos + 3, source.length)) == "NaN") {
                        // Handle -NaN (technically the same as NaN)
                        repeat(3) { advance() }
                        return Token.NumericToken(Double.NaN, line, column)
                    }
                    // Not Infinity/NaN, revert and continue with normal number parsing
                    pos -= 1
                    currentChar = sign
                }
                readNumber()
            }
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> readNumber()
            '\\' -> readEscapedIdentifier()
            else -> {
                if (isIdentifierStart(currentChar)) {
                    readIdentifier()
                } else {
                    throw JSON5Exception("Unexpected character: $currentChar", line, column)
                }
            }
        }
    }

    private fun readEscapedIdentifier(): Token {
        val startColumn = column
        advance() // Skip the backslash

        // Handle Unicode escapes in identifiers
        if (currentChar != 'u') {
            throw JSON5Exception("Expected 'u' after backslash in identifier", line, column)
        }

        advance() // Skip the 'u'
        val c = readHexEscape(4)

        // Check if valid identifier start
        if (!isIdentifierStart(c)) {
            throw JSON5Exception("Invalid identifier character", line, column)
        }

        val buffer = StringBuilder().append(c)

        // Continue reading the rest of the identifier
        while (true) {
            if (currentChar == '\\') {
                advance() // Skip backslash
                if (currentChar != 'u') {
                    throw JSON5Exception("Expected 'u' after backslash in identifier", line, column)
                }
                advance() // Skip 'u'
                buffer.append(readHexEscape(4))
            } else if (currentChar != null && isIdentifierPart(currentChar)) {
                buffer.append(currentChar)
                advance()
            } else {
                break
            }
        }

        return Token.IdentifierToken(buffer.toString(), line, startColumn)
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
                column = 0
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
            skipWhitespace()
        }
    }

    private fun readString(): Token.StringToken {
        val startColumn = column
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
                '\n', '\r' -> throw JSON5Exception("Unterminated string", line, startColumn)
                else -> {
                    buffer.append(currentChar)
                    advance()
                }
            }
        }

        if (!done) {
            throw JSON5Exception("Unterminated string", line, startColumn)
        }

        return Token.StringToken(buffer.toString(), line, startColumn)
    }

    private fun readEscapeSequence(): Char {
        return when (currentChar) {
            'b' -> { advance(); '\b' }
            'f' -> { advance(); '\u000C' }
            'n' -> { advance(); '\n' }
            'r' -> { advance(); '\r' }
            't' -> { advance(); '\t' }
            'v' -> { advance(); '\u000B' }
            '0' -> { advance(); '\u0000' }
            '\\' -> { advance(); '\\' }
            '\'' -> { advance(); '\'' }
            '"' -> { advance(); '"' }
            'a' -> { advance(); '\u0007' } // Bell character
            '\n' -> {
                advance()
                return ' ' // Line continuation returns nothing visible
            }
            '\r' -> {
                advance()
                if (currentChar == '\n') advance()
                return ' ' // Line continuation returns nothing visible
            }
            '\u2028', '\u2029' -> {
                advance()
                return ' ' // Line continuation with line/paragraph separator
            }
            'x' -> {
                advance()
                readHexEscape(2)
            }
            'u' -> {
                advance()
                readHexEscape(4)
            }
            else -> {
                val c = currentChar
                advance()
                c ?: throw JSON5Exception("Invalid escape sequence", line, column)
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
            advance() // Skip '+'
        } else if (currentChar == '-') {
            isNegative = true
            buffer.append('-')
            advance() // Skip '-'
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
                throw JSON5Exception("Invalid hexadecimal number", line, column)
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
                throw JSON5Exception("Invalid hexadecimal number: ${buffer}", line, column)
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
            advance()

            if (currentChar == '+' || currentChar == '-') {
                buffer.append(currentChar)
                advance()
            }

            var hasExponentDigits = false
            while (currentChar != null && currentChar!!.isDigit()) {
                buffer.append(currentChar)
                hasExponentDigits = true
                advance()
            }

            if (!hasExponentDigits) {
                throw JSON5Exception("Invalid exponent in number", line, column)
            }

            hasExponentPart = true
        }

        // Must have at least one part (integer, fraction, or starts with a decimal point)
        if (!(hasIntegerPart || hasFractionPart) || (hasFractionPart && !hasIntegerPart && buffer.length == 1)) {
            throw JSON5Exception("Invalid number", line, column)
        }

        val value = buffer.toString().toDouble()
        return Token.NumericToken(value, startLine, startColumn)
    }

    private fun parseHexToDouble(hexStr: String): Double {
        var result = 0.0
        for (c in hexStr) {
            result = result * 16 + c.digitToInt(16)
        }
        return result
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
}
