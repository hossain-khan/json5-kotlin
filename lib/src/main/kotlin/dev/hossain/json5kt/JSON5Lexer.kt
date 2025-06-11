package dev.hossain.json5kt

/**
 * Lexer for JSON5 syntax
 * Breaks JSON5 text into tokens for the parser
 */
class JSON5Lexer(
    private val source: String,
) {
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
        // Fix: skip whitespace and comments in a loop before every token
        while (true) {
            val before = pos
            skipWhitespace()
            skipComments()
            if (pos == before) break
        }

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
            // Fix: Only treat 'n', 't', 'f', 'I', 'N' as keywords if they match the full identifier
            else -> {
                if (isIdentifierStart(currentChar)) {
                    return readIdentifier()
                } else if (currentChar == 'n') {
                    return readNull()
                } else if (currentChar == 't') {
                    return readTrue()
                } else if (currentChar == 'f') {
                    return readFalse()
                } else if (currentChar == 'I') {
                    return readInfinity()
                } else if (currentChar == 'N') {
                    return readNaN()
                } else if (currentChar == '+') {
                    if (peek() == 'I') {
                        // Handle +Infinity
                        val sign = currentChar
                        val startColumn = column
                        advance()
                        if (source.substring(pos, minOf(pos + 8, source.length)) == "Infinity") {
                            repeat(8) { advance() }
                            return Token.NumericToken(Double.POSITIVE_INFINITY, line, startColumn)
                        }
                        pos -= 1
                        if (currentChar == '\n') {
                            line -= 1
                            column = 1
                        } else {
                            column -= 1
                        }
                        currentChar = sign
                    }
                    return readNumber()
                } else if (currentChar == '-') {
                    if (peek() == 'I') {
                        // Handle -Infinity
                        val sign = currentChar
                        val startColumn = column
                        advance()
                        if (source.substring(pos, minOf(pos + 8, source.length)) == "Infinity") {
                            repeat(8) { advance() }
                            return Token.NumericToken(Double.NEGATIVE_INFINITY, line, startColumn)
                        }
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
                        pos -= 1
                        if (currentChar == '\n') {
                            line -= 1
                            column = 1
                        } else {
                            column -= 1
                        }
                        currentChar = sign
                    }
                    return readNumber()
                } else if (currentChar in '0'..'9' || currentChar == '.') {
                    return readNumber()
                } else if (currentChar == '\\') {
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
                } else if (currentChar == '$' || currentChar == '_') {
                    return readIdentifier()
                } else if (currentChar == '/') {
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

    private fun isWhitespace(c: Char): Boolean =
        c == ' ' ||
            c == '\t' ||
            c == '\n' ||
            c == '\r' ||
            c == '\u00A0' ||
            c == '\u2028' ||
            c == '\u2029' ||
            c == '\u000B' ||
            c == '\u000C' ||
            c == '\uFEFF' ||
            // Include other Unicode space separators
            c.category == CharCategory.SPACE_SEPARATOR

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

    /**
     * Optimized string reading with pre-sized buffer and efficient escape handling.
     * Performance improvements:
     * - Pre-sized StringBuilder to reduce allocations
     * - Fast path for strings without escapes (but maintains position tracking accuracy)
     * - Optimized escape sequence processing
     */
    private fun readString(): Token.StringToken {
        val startColumn = column
        val startLine = line
        val quoteChar = currentChar
        advance() // Skip the quote character

        // Estimate initial capacity based on typical string lengths (reduces allocations)
        val buffer = StringBuilder(32)
        var done = false

        while (!done && currentChar != null) {
            when (currentChar) {
                quoteChar -> {
                    done = true
                    advance() // Skip the closing quote
                }
                '\\' -> {
                    advance() // Skip the backslash
                    val escapeChar = readEscapeSequenceOrNull()
                    if (escapeChar != null) {
                        // Only append if it's not a line continuation
                        buffer.append(escapeChar)
                    }
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

    /**
     * Read an escape sequence, returning null for line continuations
     */
    private fun readEscapeSequenceOrNull(): Char? {
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
                null // Line continuation - skip the newline
            }
            '\r' -> {
                advance()
                if (currentChar == '\n') advance()
                null // Line continuation - skip the CR/LF
            }
            '\u2028', '\u2029' -> {
                advance()
                null // Line continuation - skip line/paragraph separator
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

        // Check each character of "null" individually
        if (currentChar != 'n') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'u') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'l') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'l') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        return Token.NullToken(startLine, startColumn)
    }

    private fun readTrue(): Token {
        val startColumn = column
        val startLine = line

        // Check each character of "true" individually
        if (currentChar != 't') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'r') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'u') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'e') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        return Token.BooleanToken(true, startLine, startColumn)
    }

    private fun readFalse(): Token {
        val startColumn = column
        val startLine = line

        // Check each character of "false" individually
        if (currentChar != 'f') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'a') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'l') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 's') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'e') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        return Token.BooleanToken(false, startLine, startColumn)
    }

    private fun readInfinity(): Token {
        val startColumn = column
        val startLine = line

        // Check each character of "Infinity" individually
        if (currentChar != 'I') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'n') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'f') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'i') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'n') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'i') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 't') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'y') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        return Token.NumericToken(Double.POSITIVE_INFINITY, startLine, startColumn)
    }

    private fun readNaN(): Token {
        val startColumn = column
        val startLine = line

        // Check each character of "NaN" individually
        if (currentChar != 'N') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'a') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        if (currentChar != 'N') {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }
        advance()

        return Token.NumericToken(Double.NaN, startLine, startColumn)
    }

    /**
     * Optimized number reading with reduced allocations and faster hex parsing.
     * Performance improvements:
     * - Pre-sized StringBuilder with estimated capacity
     * - Optimized hex number parsing without string manipulations
     * - Fast path for simple integer numbers
     */
    private fun readNumber(): Token.NumericToken {
        val startColumn = column
        val startLine = line
        var isNegative = false

        // Handle sign
        if (currentChar == '+') {
            advance() // Skip '+'
        } else if (currentChar == '-') {
            isNegative = true
            advance() // Skip '-'
        }

        // Handle number following sign
        if (currentChar == null || (!currentChar!!.isDigit() && currentChar != '.')) {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }

        // Handle hexadecimal notation - optimized path
        if (currentChar == '0' && (peek() == 'x' || peek() == 'X')) {
            advance() // Skip '0'
            advance() // Skip 'x' or 'X'

            // Collect hex digits directly without StringBuilder for common small cases
            val hexStart = pos
            var hexDigitCount = 0
            while (currentChar != null && currentChar!!.isHexDigit()) {
                hexDigitCount++
                advance()
            }

            if (hexDigitCount == 0) {
                throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
            }

            try {
                val hexStr = source.substring(hexStart, pos)
                val value = if (isNegative) -parseHexToDouble(hexStr) else parseHexToDouble(hexStr)
                return Token.NumericToken(value, startLine, startColumn)
            } catch (e: NumberFormatException) {
                throw JSON5Exception("Invalid hexadecimal number", line, column)
            }
        }

        // Handle decimal notation - optimized with pre-sizing
        // Estimate capacity based on typical number lengths (reduces allocations)
        val buffer = StringBuilder(16)

        if (isNegative) {
            buffer.append('-')
        }

        // Integer part (optional if there's a decimal point)
        var hasIntegerPart = false
        if (currentChar?.isDigit() == true) {
            hasIntegerPart = true
            // Fast path for simple integers - collect digits efficiently
            do {
                buffer.append(currentChar)
                advance()
            } while (currentChar != null && currentChar!!.isDigit())
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
        if (currentChar == 'e' || currentChar == 'E') {
            buffer.append(currentChar)
            advance()

            if (currentChar == '+' || currentChar == '-') {
                buffer.append(currentChar)
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
        }

        // Must have at least one part (integer, fraction, or starts with a decimal point)
        if (!(hasIntegerPart || hasFractionPart) || (hasFractionPart && !hasIntegerPart && buffer.length <= 1)) {
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }

        val value = buffer.toString().toDouble()
        return Token.NumericToken(value, startLine, startColumn)
    }

    /**
     * Optimized hex parsing with fast path for common cases.
     * Performance improvement: Avoid string operations and power calculations for small hex numbers.
     */
    private fun parseHexToDouble(hexStr: String): Double {
        // Fast path for empty/invalid input
        if (hexStr.isEmpty()) return 0.0

        // Fast path for small hex numbers (most common case)
        // Can represent up to 15 hex digits precisely in a Long
        if (hexStr.length <= 15) {
            return try {
                hexStr.toLong(16).toDouble()
            } catch (e: NumberFormatException) {
                0.0
            }
        }

        // For larger numbers, use optimized chunking approach
        // Reduce allocations by processing in place
        try {
            var result = 0.0
            val len = hexStr.length
            var power = 1.0

            // Process from right to left in 8-digit chunks to minimize allocations
            var end = len
            while (end > 0) {
                val start = maxOf(0, end - 8)
                val chunkValue = hexStr.substring(start, end).toLong(16)
                result += chunkValue * power
                power *= 4294967296.0 // 16^8 as constant (0x100000000)
                end = start
            }

            return result
        } catch (e: NumberFormatException) {
            // Fallback for very large numbers - simplified approach
            return hexStr.toULongOrNull(16)?.toDouble() ?: 0.0
        }
    }

    /**
     * Optimized identifier reading with fast path for simple identifiers.
     * Performance improvements:
     * - Pre-sized StringBuilder for typical identifier lengths
     * - Fast path scanning for simple identifiers without escapes
     * - Reduced string allocations in validation
     */
    private fun readIdentifier(): Token {
        val startColumn = column

        // Pre-size buffer for typical identifier length
        val buffer = StringBuilder(16)

        // Handle the case where the first character is already processed
        if (currentChar != null && isIdentifierStart(currentChar)) {
            buffer.append(currentChar)
            advance()
        }

        // Fast path for simple identifiers without escape sequences
        while (currentChar != null && isIdentifierPart(currentChar) && currentChar != '\\') {
            buffer.append(currentChar)
            advance()
        }

        // Handle escape sequences if present
        while (currentChar != null) {
            if (currentChar == '\\') {
                val escapeColumn = column
                advance() // Skip the backslash

                if (currentChar != 'u') {
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, escapeColumn)
                }

                advance() // Skip 'u'

                // Read 4 hex digits directly without StringBuilder for better performance
                var hexValue = 0
                repeat(4) {
                    if (currentChar == null || !currentChar!!.isHexDigit()) {
                        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                    }
                    hexValue = hexValue * 16 + currentChar!!.digitToInt(16)
                    advance()
                }

                val char = hexValue.toChar()
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

        val ident = buffer.toString()

        // Fast literal matching using when expression (more efficient than multiple if conditions)
        return when (ident) {
            "true" -> Token.BooleanToken(true, line, startColumn)
            "false" -> Token.BooleanToken(false, line, startColumn)
            "null" -> Token.NullToken(line, startColumn)
            "Infinity" -> Token.NumericToken(Double.POSITIVE_INFINITY, line, startColumn)
            "-Infinity" -> Token.NumericToken(Double.NEGATIVE_INFINITY, line, startColumn)
            "NaN" -> Token.NumericToken(Double.NaN, line, startColumn)
            else -> {
                // Check for malformed literals more efficiently
                if (currentChar != null) {
                    when {
                        ident in arrayOf("t", "tr", "tru") -> throw JSON5Exception.invalidChar(currentChar!!, line, column)
                        ident in arrayOf("f", "fa", "fal", "fals") -> throw JSON5Exception.invalidChar(currentChar!!, line, column)
                        ident in arrayOf("n", "nu", "nul") -> throw JSON5Exception.invalidChar(currentChar!!, line, column)
                    }
                }
                Token.IdentifierToken(ident, line, startColumn)
            }
        }
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

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
