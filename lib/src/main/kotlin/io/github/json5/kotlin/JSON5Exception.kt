package io.github.json5.kotlin

/**
 * Exception thrown when there's an error parsing JSON5 data.
 */
class JSON5Exception(
    message: String,
    val lineNumber: Int = -1,
    val columnNumber: Int = -1
) : RuntimeException(message) {

    override val message: String
        get() = if (lineNumber > 0) {
            "${super.message} at line $lineNumber, column $columnNumber"
        } else {
            super.message ?: ""
        }
}
