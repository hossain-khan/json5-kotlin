package io.github.json5.kotlin

/**
 * Exception thrown when there's an error parsing JSON5 input.
 */
class JSON5Exception(
    message: String,
    val lineNumber: Int,
    val columnNumber: Int
) : RuntimeException("JSON5: $message at line $lineNumber, column $columnNumber") {

    companion object {
        /**
         * Format a character for error messages, with special handling for control characters.
         */
        fun formatChar(c: Char): String {
            return when {
                c.code < 0x20 || c == '\u007F' -> "\\x" + c.code.toString(16).padStart(2, '0')
                else -> c.toString()
            }
        }

        /**
         * Create an exception for an invalid character in the input.
         */
        fun invalidChar(c: Char, line: Int, col: Int): JSON5Exception {
            val charStr = formatChar(c)
            return JSON5Exception("invalid character '$charStr'", line, col)
        }

        /**
         * Create an exception for invalid end of input.
         */
        fun invalidEndOfInput(line: Int, col: Int): JSON5Exception {
            return JSON5Exception("invalid end of input", line, col)
        }

        /**
         * Create an exception for invalid identifier character.
         */
        fun invalidIdentifierChar(line: Int, col: Int): JSON5Exception {
            return JSON5Exception("invalid identifier character", line, col)
        }
    }
}
