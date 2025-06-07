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

    private fun nextChar(): Char? {
        if (pos >= source.length) {
            return null
        }

        val c = source[pos]
        return c
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
                    }
                    // Not Infinity, revert and continue with normal number parsing
                    pos -= 1
                    if (currentChar == '\n') {
                        line -= 1
                        column = 1
                    } else {
                        column -= 1
                    }
                    currentChar = sign
                } else if (peek() == 'N') {
                    // Handle -NaN
                    val sign = currentChar
                    val startColumn = column
                    advance()
                    if (source.substring(pos, minOf(pos + 3, source.length)) == "NaN") {
                        repeat(3) { advance() }
                        return Token.NumericToken(Double.NaN, line, startColumn) // NaN is NaN regardless of sign
                    }
                    // Not NaN, revert and continue with normal number parsing
                    pos -= 1
                    if (currentChar == '\n') {
                        line -= 1
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

                // Now process the escaped character
                if (currentChar == 'u') {
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
                    while (currentChar != null) {
                        if (currentChar == '\\') {
                            val continueColumn = column
                            advance() // Skip backslash

                            if (currentChar == 'u') {
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
                            } else {
                                throw JSON5Exception.invalidChar(currentChar ?: ' ', line, continueColumn)
                            }
                        } else if (isIdentifierPart(currentChar)) {
                            buffer.append(currentChar)
                            advance()
                        } else {
                            break
                        }
                    }

                    return Token.IdentifierToken(buffer.toString(), line, startColumn)
                } else {
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                }
            }
            '$', '_' -> {
                // Handle property names starting with $ or _
                readIdentifier()
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

        return when (currentChar) {
            'b' -> {
                advance()
                '\b'
            }
            'f' -> {
                advance()
                '\u000C'
            }
            'n' -> {
                advance()
                '\n'
            }
            'r' -> {
                advance()
                '\r'
            }
            't' -> {
                advance()
                '\t'
            }
            'v' -> {
                advance()
                '\u000B'
            }
            '0' -> {
                advance()
                // Check if the next character is a digit (which would be invalid)
                if (currentChar != null && currentChar!!.isDigit()) {
                    throw JSON5Exception.invalidChar(currentChar!!, line, column)
                }
                '\u0000'
            }
            '\\' -> {
                advance()
                '\\'
            }
            '\'' -> {
                advance()
                '\''
            }
            '"' -> {
                advance()
                '"'
            }
            'a' -> {
                advance()
                '\u0007' // Bell character
            }
            ' ' -> {
                advance()
                ' ' // Space
            }
            '\n' -> {
                advance()
                '\u0000' // Line continuation - returns nothing
            }
            '\r' -> {
                advance()
                if (currentChar == '\n') advance()
                '\u0000' // Line continuation - returns nothing
            }
            '\u2028', '\u2029' -> {
                advance()
                '\u0000' // Line continuation - returns nothing
            }
            'x' -> {
                advance() // Skip 'x'
                val hexString = StringBuilder()
                repeat(2) {
                    if (currentChar == null || !currentChar!!.isHexDigit()) {
                        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                    }
                    hexString.append(currentChar)
                    advance()
                }
                hexString.toString().toInt(16).toChar()
            }
            'u' -> {
                advance() // Skip 'u'
                val hexString = StringBuilder()
                repeat(4) {
                    if (currentChar == null || !currentChar!!.isHexDigit()) {
                        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                    }
                    hexString.append(currentChar)
                    advance()
                }
                hexString.toString().toInt(16).toChar()
            }
            null -> throw JSON5Exception.invalidEndOfInput(line, escapeCol)
            in '1'..'9' -> throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
            else -> {
                // Just skip the escape and include the character itself (as per JSON5 spec for unknown escapes)
                val c = currentChar ?: throw JSON5Exception.invalidEndOfInput(line, escapeCol)
                advance()
                c
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
        // For hexadecimal numbers, we need to replicate JavaScript's behavior
        try {
            // For small numbers that can be represented as a Long, this approach is precise
            if (hexStr.length <= 15) {
                return hexStr.toLong(16).toDouble()
            }

            // For larger numbers, we need to handle them specially
            // JavaScript converts large hex numbers to double precision which can lose precision
            // We'll calculate this by breaking down into chunks

            var result = 0.0
            var power = 1.0

            // Process 8 digits at a time from right to left
            var remaining = hexStr
            while (remaining.isNotEmpty()) {
                val chunk = remaining.takeLast(8) // Take up to 8 digits
                remaining = remaining.dropLast(chunk.length)

                val chunkValue = chunk.toLongOrNull(16) ?: 0
                result += chunkValue * power
                power *= 16.0.pow(8) // Move to next 8-digit chunk
            }

            return result
        } catch (e: NumberFormatException) {
            // If it's too big for Long, use JavaScript's approach: convert to number and it might lose precision
            // This is the behavior in the reference implementation
            val jsChunks = hexStr.chunked(12) // Process in chunks JavaScript can handle
            var result = 0.0
            for (i in jsChunks.indices) {
                val chunk = jsChunks[i]
                result += chunk.toULong(16).toDouble() * 16.0.pow((jsChunks.size - 1 - i) * 12)
            }
            return result
        }
    }

    private fun readIdentifier(): Token {
        val startColumn = column
        val buffer = StringBuilder()

        // Handle the case where the first character is already processed
        // (e.g., when called from the main switch statement)
        if (currentChar != null && isIdentifierStart(currentChar)) {
            buffer.append(currentChar)
            advance()
        }

        while (currentChar != null) {
            if (currentChar == '\\') {
                val escapeColumn = column
                advance() // Skip the backslash

                if (currentChar != 'u') {
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, escapeColumn)
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
                if (!isIdentifierPart(char)) {
                    throw JSON5Exception.invalidIdentifierChar(line, escapeColumn)
                }

                buffer.append(char)
            } else if (isIdentifierPart(currentChar)) {
                buffer.append(currentChar)
                advance()
            } else {
                break
            }
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
