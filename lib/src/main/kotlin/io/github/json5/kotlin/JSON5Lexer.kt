package io.github.json5.kotlin

import java.math.BigInteger
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
            // Removed '\\' case for identifiers here, will be handled by isIdentifierStart -> readIdentifier
            '$', '_', '\\' -> readIdentifier(line, column) // Treat '\\' for \uXXXX as start of identifier
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
                    readIdentifier(line, column)
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
                    readEscapeSequence()?.let { buffer.append(it) }
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

    private fun readEscapeSequence(): Char? {
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
                    // JSON5: \0 not followed by a digit is U+0000.
                    // \0 followed by a digit is an error in JSON5 (unlike C octal escapes)
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
            // Removed 'a' case, will be handled by else
            // Removed ' ' case, will be handled by else
            '\n' -> { // Line Feed
                advance()
                null // Line continuation - returns nothing
            }
            '\r' -> { // Carriage Return
                advance()
                if (currentChar == '\n') advance() // Consume LF if it's CRLF
                null // Line continuation - returns nothing
            }
            '\u2028' -> { // Line Separator
                advance()
                null // Line continuation - returns nothing
            }
            '\u2029' -> { // Paragraph Separator
                advance()
                null // Line continuation - returns nothing
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
            // '1' through '9' are not valid single-character escapes in JSON5
            // and should not be treated as octal escapes. They fall into the 'else'
            // case, resulting in the character itself.
            // e.g. \1 -> "1"
            null -> throw JSON5Exception.invalidEndOfInput(line, escapeCol) // End of input during escape
            else -> {
                // For any other character following a backslash, the character itself is used.
                // This includes characters like 'a', 'c', '/', '1', etc.
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
        val originalBuffer = StringBuilder() // Keep original full number string for error reporting or other uses if needed
        var isNegative = false
        val initialChar = currentChar

        // Handle sign
        if (currentChar == '+') {
            originalBuffer.append('+')
            advance() // Skip '+'
        } else if (currentChar == '-') {
            isNegative = true
            originalBuffer.append('-')
            advance() // Skip '-'
        }

        // Handle number following sign
        // For hex, first digit must be 0. For others, it can be a digit or '.'
        if (currentChar == null || (!currentChar!!.isDigit() && currentChar != '.' && !(initialChar == '0' && (currentChar == 'x' || currentChar == 'X'))) ) {
             // Check if it's a standalone sign (which is invalid) or sign followed by non-digit/non-hex-marker
            if (originalBuffer.isNotEmpty() && (currentChar == null || !currentChar!!.isDigit() && currentChar != '.' && currentChar != 'x' && currentChar != 'X')) {
                 throw JSON5Exception.invalidChar(initialChar ?: ' ', line, startColumn)
            }
            throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
        }

        // Handle hexadecimal notation
        if (currentChar == '0' && (peek() == 'x' || peek() == 'X')) {
            originalBuffer.append(currentChar) // '0'
            advance() // Skip '0'
            originalBuffer.append(currentChar) // 'x' or 'X'
            advance() // Skip 'x' or 'X'

            val hexDigitsBuffer = StringBuilder()
            while (currentChar != null && currentChar!!.isHexDigit()) {
                hexDigitsBuffer.append(currentChar)
                originalBuffer.append(currentChar)
                advance()
            }

            if (hexDigitsBuffer.isEmpty()) {
                // This means we had "0x" but no valid hex digits after it
                throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
            }

            try {
                val numericValue = parseHexToDouble(hexDigitsBuffer.toString())
                val finalValue = if (isNegative) -numericValue else numericValue
                return Token.NumericToken(finalValue, startLine, startColumn)
            } catch (e: NumberFormatException) {
                // This might happen if parseHexToDouble itself throws an error for some reason,
                // though with BigInteger it's less likely for valid hex strings.
                throw JSON5Exception("Invalid hexadecimal number: ${hexDigitsBuffer.toString()}", line, column)
            }
        }

        // Handle decimal notation
        // Append digits already part of originalBuffer (sign)
        val buffer = StringBuilder(originalBuffer)

        // Integer part (optional if there's a decimal point)
        var hasIntegerPart = false
        if (currentChar?.isDigit() == true) {
            hasIntegerPart = true
            while (currentChar != null && currentChar!!.isDigit()) {
                buffer.append(currentChar)
                advance()
            }
        } else if (currentChar != '.') { // If not a digit, and not starting a decimal, it's an error if we are here.
            // This case should ideally be caught earlier, but as a safeguard:
             if (buffer.isEmpty() || (buffer.length == 1 && (buffer[0] == '+' || buffer[0] == '-'))) { // only a sign was present
                throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
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
        if (hexStr.isEmpty()) {
            // This case should ideally be prevented by the caller (readNumber ensures hasDigits).
            throw NumberFormatException("Empty hex string")
        }
        // Use BigInteger to parse the hex string, then convert to Double.
        // This aligns with JavaScript's behavior of converting hex literals to its Number type (double).
        return BigInteger(hexStr, 16).toDouble()
    }

    private fun readIdentifier(startLine: Int, startColumn: Int): Token.IdentifierToken {
        val buffer = StringBuilder()
        var firstChar = true

        while (currentChar != null) {
            val charToProcess: Char
            val charCol = column // Store column before potential advance in readUnicodeEscapeForIdentifier

            if (currentChar == '\\') {
                advance() // Skip the backslash
                if (currentChar == 'u') {
                    advance() // Skip 'u'
                    charToProcess = readUnicodeEscapeForIdentifier(charCol)
                } else {
                    // As per ES5.1, an escape sequence in an identifier must be a UnicodeEscapeSequence.
                    // \c is not valid in an identifier name.
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, charCol)
                }
            } else {
                charToProcess = currentChar!!
                advance()
            }

            if (firstChar) {
                if (!isIdentifierStart(charToProcess)) {
                    throw JSON5Exception.invalidIdentifierChar(line, charCol)
                }
                firstChar = false
            } else {
                if (!isIdentifierPart(charToProcess)) {
                    // If it's not a valid part, it means the identifier ended one char ago.
                    // We need to "unread" the charToProcess by moving pos and column back.
                    // This is tricky because advance() can cross lines.
                    // For simplicity in this refactor, we'll assume identifiers are typically
                    // not immediately followed by invalid characters that would need complex unreading.
                    // A more robust solution might involve peeking or more careful advance/retreat.
                    // However, the current structure of the main loop in nextToken() usually handles
                    // whitespace or punctuators that would terminate the identifier correctly.
                    // The issue arises if an invalid char is directly adjacent, e.g., ident#
                    // Let's throw, assuming the nextToken's main loop will break or handle.
                    // The original code also advanced and then checked isIdentifierPart.
                    throw JSON5Exception.invalidIdentifierChar(line, charCol)
                }
            }
            buffer.append(charToProcess)

            // After processing a char (or escape), check if the next one is still part of the identifier
            if (currentChar == null || (!isIdentifierPart(currentChar) && currentChar != '\\')) {
                break
            }
        }
        if (buffer.isEmpty()) {
            // This can happen if called with '\' but not followed by 'u' and a valid sequence
            // or if the first char itself is invalid and an exception was thrown and caught,
            // or if the input is just "\"
            throw JSON5Exception.invalidIdentifierChar(startLine, startColumn)
        }
        return Token.IdentifierToken(buffer.toString(), startLine, startColumn)
    }

    private fun readUnicodeEscapeForIdentifier(escapeStartColumn: Int): Char {
        val hexDigits = StringBuilder()
        repeat(4) {
            if (currentChar == null) {
                throw JSON5Exception.invalidEndOfInput(line, column)
            }
            if (!currentChar!!.isHexDigit()) {
                throw JSON5Exception.invalidChar(currentChar!!, line, column)
            }
            hexDigits.append(currentChar)
            advance()
        }
        return hexDigits.toString().toInt(16).toChar()
    }

    // Renamed from readHexEscape to avoid confusion with string hex escapes
    private fun readHexEscapeGeneric(digits: Int, escapeLine: Int, escapeCol: Int): Char {
        val hexString = StringBuilder()
        repeat(digits) {
            if (currentChar == null) {
                throw JSON5Exception.invalidEndOfInput(escapeLine, escapeCol + 1 + it) // Approx position
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
