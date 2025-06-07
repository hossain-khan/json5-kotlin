package io.github.json5.kotlin

/**
 * Helper for handling escape sequences in JSON5
 */
object EscapeHelper {
    /**
     * Processes a string that may contain escape sequences into their actual character representation
     * This is used for property names that contain escaped characters like \uXXXX
     */
    fun processEscapedString(input: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < input.length) {
            if (input[i] == '\\' && i + 1 < input.length) {
                // Handle escape sequence
                when (val nextChar = input[i + 1]) {
                    'u' -> {
                        // Unicode escape sequence \uXXXX
                        if (i + 5 < input.length) {
                            val hexCode = input.substring(i + 2, i + 6)
                            try {
                                val charCode = hexCode.toInt(16)
                                result.append(charCode.toChar())
                                i += 6
                            } catch (e: NumberFormatException) {
                                result.append('\\').append('u')
                                i += 2
                            }
                        } else {
                            result.append('\\').append('u')
                            i += 2
                        }
                    }
                    'x' -> {
                        // Hex escape sequence \xFF
                        if (i + 3 < input.length) {
                            val hexCode = input.substring(i + 2, i + 4)
                            try {
                                val charCode = hexCode.toInt(16)
                                result.append(charCode.toChar())
                                i += 4
                            } catch (e: NumberFormatException) {
                                result.append('\\').append('x')
                                i += 2
                            }
                        } else {
                            result.append('\\').append('x')
                            i += 2
                        }
                    }
                    'b' -> { result.append('\b'); i += 2 }
                    'f' -> { result.append('\u000C'); i += 2 }
                    'n' -> { result.append('\n'); i += 2 }
                    'r' -> { result.append('\r'); i += 2 }
                    't' -> { result.append('\t'); i += 2 }
                    'v' -> { result.append('\u000B'); i += 2 }
                    '0' -> { result.append('\u0000'); i += 2 }
                    'a' -> { result.append('\u0007'); i += 2 } // Bell character
                    '\\' -> { result.append('\\'); i += 2 }
                    '\'' -> { result.append('\''); i += 2 }
                    '"' -> { result.append('"'); i += 2 }
                    '\n' -> { i += 2 } // Line continuation - skip both characters
                    '\r' -> {
                        i += 2
                        // Skip following \n if present (for \r\n line endings)
                        if (i < input.length && input[i] == '\n') {
                            i++
                        }
                    }
                    '\u2028', '\u2029' -> { i += 2 } // Line/paragraph separator continuation
                    else -> {
                        result.append(nextChar)
                        i += 2
                    }
                }
            } else {
                result.append(input[i])
                i++
            }
        }

        return result.toString()
    }
}
