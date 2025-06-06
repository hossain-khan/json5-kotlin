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
            'I' -> readInfinity() // Handles "Infinity"
            'N' -> readNaN()     // Handles "NaN"
            // Handle numbers, including those starting with a sign
            '+', '-' -> {
                // Peek ahead for Infinity or NaN
                val peekedChar = peek()
                if (peekedChar == 'I') {
                    readSignedInfinity()
                } else if (peekedChar == 'N') {
                    readSignedNaN()
                } else {
                    readNumber()
                }
            }
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> readNumber()
            else -> {
                if (isIdentifierStart(currentChar)) {
                    readIdentifier()
                } else {
                    throw JSON5Exception("Unexpected character: $currentChar", line, column)
                }
            }
        }
    }

    private fun isIdentifierStart(c: Char?): Boolean {
        if (c == null) return false
        if (c == '$' || c == '_') return true // $ and _ are explicit
        return when (c.category) {
            CharCategory.UPPERCASE_LETTER,          // Lu
            CharCategory.LOWERCASE_LETTER,          // Ll
            CharCategory.TITLECASE_LETTER,          // Lt
            CharCategory.MODIFIER_LETTER,           // Lm
            CharCategory.OTHER_LETTER,              // Lo
            CharCategory.LETTER_NUMBER              // Nl
            -> true
            else -> false
        }
    }

    private fun isIdentifierPart(c: Char?): Boolean {
        if (c == null) return false
        // Check against IdentifierStart first (includes $, _)
        if (isIdentifierStart(c)) return true
        return when (c.category) {
            CharCategory.NON_SPACING_MARK,          // Mn
            CharCategory.COMBINING_SPACING_MARK,    // Mc
            CharCategory.DECIMAL_DIGIT_NUMBER,      // Nd
            CharCategory.CONNECTOR_PUNCTUATION     // Pc (includes _ but already covered by isIdentifierStart)
            -> true
            // Explicitly check for ZWNJ and ZWJ if not covered by categories above
            // (though they are Cf - Format characters, might need specific handling if not part of a category)
            else -> c == '\u200C' || c == '\u200D' // ZWNJ (U+200C) or ZWJ (U+200D)
        }
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
        while (currentChar != null && (currentChar!!.isWhitespace() || currentChar == '\uFEFF')) {
            advance()
        }
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
                    val escapedChar = readEscapeSequence()
                    if (escapedChar != null) {
                        buffer.append(escapedChar)
                    }
                }
                // Handle unescaped line terminators
                '\n', '\r' -> throw JSON5Exception(
                    "Unterminated string literal; unescaped LF or CR found at line $line, column $column. Use line continuations (\\\\n or \\\\r) or escape them (\\n, \\r).",
                    line,
                    column // Use current column for more accuracy
                )
                '\u2028' -> { // Line Separator
                    System.err.println("Warning: JSON5: Unescaped U+2028 (Line Separator) in string at line $line, column $column. Consider escaping it as \\u2028.")
                    buffer.append(currentChar)
                    advance()
                }
                '\u2029' -> { // Paragraph Separator
                    System.err.println("Warning: JSON5: Unescaped U+2029 (Paragraph Separator) in string at line $line, column $column. Consider escaping it as \\u2029.")
                    buffer.append(currentChar)
                    advance()
                }
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

    private fun readEscapeSequence(): Char? { // Return type changed to Char?
        return when (val c = currentChar) {
            // Line Terminators for Line Continuations
            '\n' -> { // LF
                advance()
                null // Indicates line continuation
            }
            '\r' -> { // CR
                advance()
                if (currentChar == '\n') { // CR+LF
                    advance()
                }
                null // Indicates line continuation
            }
            '\u2028' -> { // Line Separator
                advance()
                null // Indicates line continuation
            }
            '\u2029' -> { // Paragraph Separator
                advance()
                null // Indicates line continuation
            }

            // Standard Escape Sequences
            'b' -> { advance(); '\b' }
            'f' -> { advance(); '\u000C' } // Form Feed
            'n' -> { advance(); '\n' } // Line Feed (actual character, not continuation)
            'r' -> { advance(); '\r' } // Carriage Return (actual character, not continuation)
            't' -> { advance(); '\t' }
            'v' -> { advance(); '\u000B' } // Vertical Tab
            '0' -> { advance(); '\u0000' } // Null character (only if not followed by a digit)
            '\\' -> { advance(); '\\' }
            '\'' -> { advance(); '\'' }
            '"' -> { advance(); '"' }
            // Hexadecimal and Unicode escapes are handled below
            'x' -> {
                advance()
                readHexEscape(2)
            }
            'u' -> {
                advance()
                readHexEscape(4)
            }
            else -> {
                // Check if it's one of the line terminators that might be valid if not for line continuation
                // This part is tricky. If we are here, it means it's not `\n`, `\r`, `\u2028`, `\u2029`
                // (as those are handled above for line continuations if they were the char after `\`)
                // or any other valid escape.
                // So, if `c` (currentChar) is a line terminator here, it's an invalid escape.
                // However, the JSON5 spec says "any other character" is the character itself.
                // This implies that `\` followed by a character not in the escape set is just that character.
                // Example: `\q` would be `q`.
                advance()
                c // Return the character itself
            }
        }
    }

    private fun readHexEscape(digits: Int): Char {
        val hexString = StringBuilder()
        repeat(digits) {
            if (currentChar == null || !currentChar!!.isHexDigit()) {
                throw JSON5Exception("Invalid hex escape sequence", line, column)
            }
            hexString.append(currentChar)
            advance()
        }
        return hexString.toString().toInt(16).toChar()
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
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
        // This is called when 'I' is encountered without a preceding sign.
        if (source.substring(pos, minOf(pos + 8, source.length)) == "Infinity") {
            repeat(8) { advance() }
            return Token.NumericToken(Double.POSITIVE_INFINITY, startLine, startColumn)
        }

        throw JSON5Exception("Unexpected identifier starting with I", startLine, startColumn)
    }

    private fun readNaN(): Token {
        val startColumn = column
        val startLine = line

        // Verify that the characters spell "NaN"
        // This is called when 'N' is encountered without a preceding sign.
        if (source.substring(pos, minOf(pos + 3, source.length)) == "NaN") {
            repeat(3) { advance() }
            return Token.NumericToken(Double.NaN, startLine, startColumn)
        }

        throw JSON5Exception("Unexpected identifier starting with N", startLine, startColumn)
    }

    private fun readSignedInfinity(): Token {
        val startColumn = column
        val startLine = line
        val sign = currentChar
        advance() // Consume sign

        if (source.substring(pos, minOf(pos + 8, source.length)) == "Infinity") {
            repeat(8) { advance() }
            return if (sign == '-') {
                Token.NumericToken(Double.NEGATIVE_INFINITY, startLine, startColumn)
            } else {
                Token.NumericToken(Double.POSITIVE_INFINITY, startLine, startColumn)
            }
        }
        // This case should ideally not be reached if peek() in nextToken() is correct
        throw JSON5Exception("Expected Infinity after sign", startLine, startColumn)
    }

    private fun readSignedNaN(): Token {
        val startColumn = column
        val startLine = line
        val sign = currentChar
        advance() // Consume sign

        if (source.substring(pos, minOf(pos + 3, source.length)) == "NaN") {
            repeat(3) { advance() }
            // Sign before NaN is ignored as per JSON5 spec and Double.NaN representation
            return Token.NumericToken(Double.NaN, startLine, startColumn)
        }
        // This case should ideally not be reached if peek() in nextToken() is correct
        throw JSON5Exception("Expected NaN after sign", startLine, startColumn)
    }

    private fun readNumber(): Token.NumericToken {
        val startColumn = column
        val startLine = line
        val buffer = StringBuilder()

        // Sign is handled by nextToken for Infinity/NaN, or prepended here for other numbers
        if (currentChar == '+' || currentChar == '-') {
            buffer.append(currentChar)
            advance()
        }

        // Handle hexadecimal notation
        // Check if buffer already contains sign for hex number
        val hexPrefixPos = if (buffer.isNotEmpty() && (buffer[0] == '+' || buffer[0] == '-')) 1 else 0
        if (source.substring(pos).startsWith("0x", ignoreCase = true) ||
            (buffer.length > hexPrefixPos && buffer.substring(hexPrefixPos) == "0" && (peek() == 'x' || peek() == 'X'))) {
            if (buffer.isEmpty() || (buffer.length == 1 && (buffer[0] == '+' || buffer[0] == '-'))) {
                 buffer.append('0')
                 advance() // Skip '0'
            } else if (buffer.isNotEmpty() && buffer.last() != '0') {
                 // This case means something like "-1" then "0x", which is not a hex.
                 // Let it proceed to decimal parsing.
            }

            if (currentChar == 'x' || currentChar == 'X') {
                 buffer.append(currentChar)
                 advance() // Skip 'x' or 'X'
            }


            // Read hex digits
            var hasDigits = false
            while (currentChar != null && currentChar!!.isHexDigit()) {
                buffer.append(currentChar)
                hasDigits = true
                advance()
            }

            if (!hasDigits) {
                throw JSON5Exception("Invalid hexadecimal number: missing digits after 0x", line, column)
            }
            // Make sure to handle signed hex if needed, though JSON5 spec implies hex is unsigned.
            // For now, assuming toDouble handles "0x...", "-0x..." might be an issue if not careful.
            // Standard libraries usually parse "0x..." as positive. Sign should be outside.
            // Example: "-0x10" -> -16.0. `buffer.toString().toDouble()` might not work for "-0x10".
            // Let's refine this.
            val numericPart = if (buffer.startsWith("-0x") || buffer.startsWith("+0x")) {
                buffer.substring(0,1) + buffer.substring(3) // -Value or +Value
            } else if (buffer.startsWith("0x")) {
                buffer.substring(2)
            } else { // Should be "0x..."
                buffer.toString() // Fallback, though likely an error if not "0x..."
            }

            val doubleValue = numericPart.toLong(16).toDouble()
            val finalValue = if (buffer.startsWith("-")) -doubleValue else doubleValue

            return Token.NumericToken(finalValue, startLine, startColumn)
        }

        // Handle decimal notation
        val originalBufferState = buffer.toString() // Save state in case it's just a '.'

        // Integer part (optional if there's a decimal point)
        var hasIntegerPart = false
        if (currentChar?.isDigit() == true) {
            hasIntegerPart = true
            while (currentChar != null && currentChar!!.isDigit()) {
                buffer.append(currentChar)
                advance()
            }
        } else if (currentChar == '.' && (buffer.isEmpty() || buffer.toString() == "+" || buffer.toString() == "-")) {
            // Handles cases like ".5", "+.5", "-.5"
            // No digits before '.', so append '0' to ensure it's a valid double string like "0.5"
            if (buffer.isEmpty()) buffer.append('0')
            else if (buffer.toString() == "+") buffer.replace(0,1,"0")
            else if (buffer.toString() == "-") buffer.replace(0,1,"-0")
            // else if already has digits, this won't be hit
        }


        // Decimal point and fraction part
        var hasFractionPart = false
        if (currentChar == '.') {
            // Avoid adding multiple decimal points if one was prepended for ".5" like cases
            if (buffer.indexOf('.') == -1) {
                buffer.append('.')
            }
            advance()

            var hasDigitsAfterDecimal = false
            while (currentChar != null && currentChar!!.isDigit()) {
                buffer.append(currentChar)
                hasDigitsAfterDecimal = true
                advance()
            }
            // A number cannot end with just a decimal point e.g. "1." or simply "."
            if (!hasDigitsAfterDecimal && !hasIntegerPart && buffer.endsWith("0.")) { // for cases like ".xyz" -> "0.xyz"
                 // if it was just "." or "+." or "-."
                 if (originalBufferState == "." || originalBufferState == "+." || originalBufferState == "-." ){
                    throw JSON5Exception("Invalid number: lone decimal point", line, column)
                 }
            } else if (!hasDigitsAfterDecimal && !hasIntegerPart && buffer.toString().endsWith(".")) {
                 throw JSON5Exception("Invalid number: lone decimal point", line, column)
            } else if (!hasDigitsAfterDecimal && hasIntegerPart) {
                // "1." is valid in JSON5, it means 1.0
            } else if (!hasDigitsAfterDecimal && !hasIntegerPart) { // e.g. "."
                 throw JSON5Exception("Invalid number: expected digits after decimal point", line, column)
            }
            hasFractionPart = true // even if no digits follow, like "1."
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
                throw JSON5Exception("Invalid exponent in number: missing digits", line, column)
            }

            hasExponentPart = true
        }

        val finalString = buffer.toString()
        // Must have at least one part (integer, fraction, or starts with a decimal point)
        if (finalString.isEmpty() || finalString == "+" || finalString == "-") {
             throw JSON5Exception("Invalid number: empty number", line, column)
        }
        // handles cases like "." , "+." or "-." that were not caught earlier
        if ((finalString == "." || finalString == "+." || finalString == "-.") && !hasFractionPart && !hasIntegerPart && !hasExponentPart) {
            throw JSON5Exception("Invalid number: lone decimal point", line, column)
        }


        val value = try {
            finalString.toDouble()
        } catch (e: NumberFormatException) {
            throw JSON5Exception("Invalid number format: $finalString", line, column, e)
        }
        return Token.NumericToken(value, startLine, startColumn)
    }
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

            val value = buffer.toString().toDouble()
            return Token.NumericToken(value, startLine, startColumn)
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
