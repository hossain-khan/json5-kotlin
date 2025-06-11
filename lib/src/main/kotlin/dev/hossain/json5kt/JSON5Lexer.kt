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
                        val plusSign = currentChar // Should be '+'
                        val lineAtSign = line
                        val columnAtSign = column
                        val posAtSign = pos
                        advance() // Consume '+'

                        val keyword = "Infinity"
                        val keywordLength = 8
                        if (pos + keywordLength <= source.length && source.startsWith(keyword, pos)) {
                            val charAfterKeywordPos = pos + keywordLength
                            val isEndOfSource = charAfterKeywordPos == source.length
                            val isFollowedByIdentifierPart = !isEndOfSource && isIdentifierPart(source[charAfterKeywordPos])

                            if (!isFollowedByIdentifierPart) {
                                repeat(keywordLength) { advance() } // Consume "Infinity"
                                return Token.NumericToken(Double.POSITIVE_INFINITY, lineAtSign, columnAtSign)
                            }
                        }
                        // If not a valid +Infinity, revert to state before '+' and let readNumber handle it (or fail)
                        this.pos = posAtSign
                        this.line = lineAtSign
                        this.column = columnAtSign
                        this.currentChar = plusSign
                    }
                    return readNumber()
                } else if (currentChar == '-') {
                    // Handle -Infinity and -NaN
                    val minusSign = currentChar // Should be '-'
                    val lineAtSign = line
                    val columnAtSign = column
                    val posAtSign = pos

                    val nextCharPeek = peek()

                    if (nextCharPeek == 'I') {
                        advance() // Consume '-'
                        val keyword = "Infinity"
                        val keywordLength = 8
                        if (pos + keywordLength <= source.length && source.startsWith(keyword, pos)) {
                            val charAfterKeywordPos = pos + keywordLength
                            val isEndOfSource = charAfterKeywordPos == source.length
                            val isFollowedByIdentifierPart = !isEndOfSource && isIdentifierPart(source[charAfterKeywordPos])

                            if (!isFollowedByIdentifierPart) {
                                repeat(keywordLength) { advance() } // Consume "Infinity"
                                return Token.NumericToken(Double.NEGATIVE_INFINITY, lineAtSign, columnAtSign)
                            }
                        }
                        // If not a valid -Infinity, revert to state before '-'
                        this.pos = posAtSign
                        this.line = lineAtSign
                        this.column = columnAtSign
                        this.currentChar = minusSign
                        // Fall through to readNumber
                    } else if (nextCharPeek == 'N') {
                        advance() // Consume '-'
                        val keyword = "NaN"
                        val keywordLength = 3
                        if (pos + keywordLength <= source.length && source.startsWith(keyword, pos)) {
                            val charAfterKeywordPos = pos + keywordLength
                            val isEndOfSource = charAfterKeywordPos == source.length
                            val isFollowedByIdentifierPart = !isEndOfSource && isIdentifierPart(source[charAfterKeywordPos])

                            if (!isFollowedByIdentifierPart) {
                                repeat(keywordLength) { advance() } // Consume "NaN"
                                return Token.NumericToken(Double.NaN, lineAtSign, columnAtSign)
                            }
                        }
                        // If not a valid -NaN, revert to state before '-'
                        this.pos = posAtSign
                        this.line = lineAtSign
                        this.column = columnAtSign
                        this.currentChar = minusSign
                        // Fall through to readNumber
                    }
                    // If not -Infinity or -NaN, it's potentially a regular negative number or an error
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
        val startLine = line
        val startColumn = column
        val keyword = "null"
        val keywordLength = 4

        if (pos + keywordLength <= source.length && source.startsWith(keyword, pos)) {
            val charAfterKeywordPos = pos + keywordLength
            val isEndOfSource = charAfterKeywordPos == source.length
            val isFollowedByIdentifierPart = !isEndOfSource && isIdentifierPart(source[charAfterKeywordPos])

            if (!isFollowedByIdentifierPart) {
                repeat(keywordLength) { advance() }
                return Token.NullToken(startLine, startColumn)
            } else {
                // Keyword is followed by an identifier part, making it invalid.
                // Report the character that violates the rule.
                // Temporarily advance to get correct line/column for the offending char.
                var tempLine = line
                var tempColumn = column
                var tempPos = pos
                repeat(keywordLength) {
                    // Simulate advancing past the keyword
                    tempPos++
                    if (tempPos < source.length) {
                        if (source[tempPos - 1] == '\n') { // check char before advancing for newline
                            tempLine++
                            tempColumn = 1
                        } else {
                            tempColumn++
                        }
                    }
                }
                throw JSON5Exception.invalidChar(source[charAfterKeywordPos], tempLine, tempColumn)
            }
        }

        // If we reach here, it's not "null" as a standalone keyword.
        // currentChar should still be at the position where the match failed or started.
        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
    }

    private fun readTrue(): Token {
        val startLine = line
        val startColumn = column
        val keyword = "true"
        val keywordLength = 4

        if (pos + keywordLength <= source.length && source.startsWith(keyword, pos)) {
            val charAfterKeywordPos = pos + keywordLength
            val isEndOfSource = charAfterKeywordPos == source.length
            val isFollowedByIdentifierPart = !isEndOfSource && isIdentifierPart(source[charAfterKeywordPos])

            if (!isFollowedByIdentifierPart) {
                repeat(keywordLength) { advance() }
                return Token.BooleanToken(true, startLine, startColumn)
            } else {
                var tempLine = line
                var tempColumn = column
                var tempPos = pos
                repeat(keywordLength) {
                    tempPos++
                    if (tempPos < source.length) {
                        if (source[tempPos - 1] == '\n') {
                            tempLine++
                            tempColumn = 1
                        } else {
                            tempColumn++
                        }
                    }
                }
                throw JSON5Exception.invalidChar(source[charAfterKeywordPos], tempLine, tempColumn)
            }
        }
        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
    }

    private fun readFalse(): Token {
        val startLine = line
        val startColumn = column
        val keyword = "false"
        val keywordLength = 5

        if (pos + keywordLength <= source.length && source.startsWith(keyword, pos)) {
            val charAfterKeywordPos = pos + keywordLength
            val isEndOfSource = charAfterKeywordPos == source.length
            val isFollowedByIdentifierPart = !isEndOfSource && isIdentifierPart(source[charAfterKeywordPos])

            if (!isFollowedByIdentifierPart) {
                repeat(keywordLength) { advance() }
                return Token.BooleanToken(false, startLine, startColumn)
            } else {
                var tempLine = line
                var tempColumn = column
                var tempPos = pos
                repeat(keywordLength) {
                    tempPos++
                    if (tempPos < source.length) {
                        if (source[tempPos - 1] == '\n') {
                            tempLine++
                            tempColumn = 1
                        } else {
                            tempColumn++
                        }
                    }
                }
                throw JSON5Exception.invalidChar(source[charAfterKeywordPos], tempLine, tempColumn)
            }
        }
        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
    }

    private fun readInfinity(): Token {
        val startLine = line
        val startColumn = column
        val keyword = "Infinity"
        val keywordLength = 8

        if (pos + keywordLength <= source.length && source.startsWith(keyword, pos)) {
            val charAfterKeywordPos = pos + keywordLength
            val isEndOfSource = charAfterKeywordPos == source.length
            val isFollowedByIdentifierPart = !isEndOfSource && isIdentifierPart(source[charAfterKeywordPos])

            if (!isFollowedByIdentifierPart) {
                repeat(keywordLength) { advance() }
                return Token.NumericToken(Double.POSITIVE_INFINITY, startLine, startColumn)
            } else {
                var tempLine = line
                var tempColumn = column
                var tempPos = pos
                repeat(keywordLength) {
                    tempPos++
                    if (tempPos < source.length) {
                        if (source[tempPos - 1] == '\n') {
                            tempLine++
                            tempColumn = 1
                        } else {
                            tempColumn++
                        }
                    }
                }
                throw JSON5Exception.invalidChar(source[charAfterKeywordPos], tempLine, tempColumn)
            }
        }
        // This part of the original code was complex, attempting to match char-by-char.
        // The new logic is more direct. If it's not "Infinity" cleanly, throw based on current char.
        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
    }

    private fun readNaN(): Token {
        val startLine = line
        val startColumn = column
        val keyword = "NaN"
        val keywordLength = 3

        if (pos + keywordLength <= source.length && source.startsWith(keyword, pos)) {
            val charAfterKeywordPos = pos + keywordLength
            val isEndOfSource = charAfterKeywordPos == source.length
            val isFollowedByIdentifierPart = !isEndOfSource && isIdentifierPart(source[charAfterKeywordPos])

            if (!isFollowedByIdentifierPart) {
                repeat(keywordLength) { advance() }
                return Token.NumericToken(Double.NaN, startLine, startColumn)
            } else {
                var tempLine = line
                var tempColumn = column
                var tempPos = pos
                repeat(keywordLength) {
                    tempPos++
                    if (tempPos < source.length) {
                        if (source[tempPos - 1] == '\n') {
                            tempLine++
                            tempColumn = 1
                        } else {
                            tempColumn++
                        }
                    }
                }
                throw JSON5Exception.invalidChar(source[charAfterKeywordPos], tempLine, tempColumn)
            }
        }
        throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
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
                val fullHexStr = buffer.toString()
                val hexValueStr = if (isNegative) fullHexStr.substring(3) else fullHexStr.substring(2)

                if (hexValueStr.isEmpty()) {
                    throw JSON5Exception.invalidChar(currentChar ?: ' ', line, column)
                }

                val value = parseHexToDouble(hexValueStr)
                return Token.NumericToken(if (isNegative) -value else value, startLine, startColumn)
            } catch (e: NumberFormatException) {
                throw JSON5Exception("Invalid hexadecimal number: ${e.message}", line, column)
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
        // Use java.math.BigInteger to parse the hex string and then convert to Double.
        // This handles arbitrarily large hexadecimal integers and converts them to Double,
        // mimicking JavaScript's behavior for large number precision.
        // NumberFormatException will be thrown by BigInteger if hexStr is not a valid hex number.
        return java.math.BigInteger(hexStr, 16).toDouble()
    }

    private fun readIdentifier(): Token {
        val startColumn = column
        val buffer = StringBuilder()

        // Handle the case where the first character is already processed
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
                // Special handling for malformed literals - check if this might be a truncated literal
                val ident = buffer.toString()
                if ((ident == "t" || ident == "tr" || ident == "tru") && currentChar != null) {
                    // This looks like a malformed "true" literal
                    throw JSON5Exception.invalidChar(currentChar!!, line, column)
                } else if ((ident == "f" || ident == "fa" || ident == "fal" || ident == "fals") && currentChar != null) {
                    // This looks like a malformed "false" literal
                    throw JSON5Exception.invalidChar(currentChar!!, line, column)
                } else if ((ident == "n" || ident == "nu" || ident == "nul") && currentChar != null) {
                    // This looks like a malformed "null" literal
                    throw JSON5Exception.invalidChar(currentChar!!, line, column)
                }
                break
            }
        }

        val ident = buffer.toString()
        return when (ident) {
            "true" -> Token.BooleanToken(true, line, startColumn)
            "false" -> Token.BooleanToken(false, line, startColumn)
            "null" -> Token.NullToken(line, startColumn)
            "Infinity" -> Token.NumericToken(Double.POSITIVE_INFINITY, line, startColumn)
            "-Infinity" -> Token.NumericToken(Double.NEGATIVE_INFINITY, line, startColumn)
            "NaN" -> Token.NumericToken(Double.NaN, line, startColumn)
            else -> Token.IdentifierToken(ident, line, startColumn)
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
