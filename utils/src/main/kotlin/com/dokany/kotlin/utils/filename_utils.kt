package com.dokany.kotlin.utils

import java.io.File
import java.io.Serializable
import java.util.*

// borrowed with love from https://github.com/apache/commons-io/blob/43720d02405e0b96939b331c1be7812fe5fec877/src/main/java/org/apache/commons/io/FilenameUtils.java

object FilenameUtils {
    fun normalizedPath(path: String): String? {
        val normalizedPath = path.normalizePath() ?: return null
        val normalized = File(normalizedPath)

        return if (normalized.isDirectory) {
            val lastSeparatorIndex = normalizedPath.lastIndexOf(UNIX_SEPARATOR)
            if(lastSeparatorIndex == -1 || lastSeparatorIndex != normalizedPath.lastIndex) {
                normalizedPath + UNIX_SEPARATOR
            }
            else {
                normalizedPath
            }
        }
        else {
            normalizedPath
        }
    }
}

private fun isSystemWindows() = SYSTEM_SEPARATOR == WINDOWS_SEPARATOR

const private val NOT_FOUND = -1

/**
 * The extension separator character.
 */
const private val EXTENSION_SEPARATOR = '.'

/**
 * The extension separator String.
 */
val EXTENSION_SEPARATOR_STR = Character.toString(EXTENSION_SEPARATOR)

/**
 * The Unix separator character.
 */
const val UNIX_SEPARATOR = '/'

/**
 * The Windows separator character.
 */
const val WINDOWS_SEPARATOR = '\\'

/**
 * The system separator character.
 */
val SYSTEM_SEPARATOR = File.separatorChar

/**
 * The separator character that is the opposite of the system separator.
 */
private val OTHER_SEPARATOR: Char =
        if (isSystemWindows()) {
            UNIX_SEPARATOR
        } else {
            WINDOWS_SEPARATOR
        }

fun wildcardMatch(filename: String?, wildcardMatcher: String?, caseSensitivity: Boolean = true): Boolean {
    if (filename == null && wildcardMatcher == null) {
        return true
    }
    if (filename == null || wildcardMatcher == null) {
        return false
    }

    val sensitivity = if(caseSensitivity) IOCase.SENSITIVE else IOCase.INSENSITIVE

    val wcs = splitOnTokens(wildcardMatcher)
    var anyChars = false
    var textIdx = 0
    var wcsIdx = 0
    val backtrack = Stack<IntArray>()

    // loop around a backtrack stack, to handle complex * matching
    do {
        if (backtrack.size > 0) {
            val array = backtrack.pop()
            wcsIdx = array[0]
            textIdx = array[1]
            anyChars = true
        }

        // loop whilst tokens and text left to process
        while (wcsIdx < wcs.size) {

            if (wcs[wcsIdx] == "?") {
                // ? so move to next text char
                textIdx++
                if (textIdx > filename.length) {
                    break
                }
                anyChars = false

            } else if (wcs[wcsIdx] == "*") {
                // set any chars status
                anyChars = true
                if (wcsIdx == wcs.size - 1) {
                    textIdx = filename.length
                }

            } else {
                // matching text token
                if (anyChars) {
                    // any chars then try to locate text token
                    textIdx = sensitivity.checkIndexOf(filename, textIdx, wcs[wcsIdx])
                    if (textIdx == NOT_FOUND) {
                        // token not found
                        break
                    }
                    val repeat = sensitivity.checkIndexOf(filename, textIdx + 1, wcs[wcsIdx])
                    if (repeat >= 0) {
                        backtrack.push(intArrayOf(wcsIdx, repeat))
                    }
                } else {
                    // matching from current position
                    if (!sensitivity.checkRegionMatches(filename, textIdx, wcs[wcsIdx])) {
                        // couldnt match token
                        break
                    }
                }

                // matched text token, move text index to end of matched token
                textIdx += wcs[wcsIdx].length
                anyChars = false
            }

            wcsIdx++
        }

        // full match
        if (wcsIdx == wcs.size && textIdx == filename.length) {
            return true
        }

    } while (backtrack.size > 0)

    return false
}

/**
 * Internal method to perform the normalization.
 *
 * @param filename  the filename
 * @param separator The separator character to use
 * @param keepSeparator  true to keep the final separator
 * @return the normalized filename. Null bytes inside string will be removed.
 */
internal fun String.normalizePath(separator: Char = UNIX_SEPARATOR, keepSeparator: Boolean = true): String? {
    failIfNullBytePresent(this)

    var size = length
    if (size == 0) {
        return this
    }
    val prefix = getPrefixLength(this)
    if (prefix < 0) {
        return null
    }

    val array = CharArray(size + 2)  // +1 for possible extra slash, +2 for arraycopy
    toCharArray(array, 0, 0, length)

    // fix separators throughout
    val otherSeparator = if (separator == SYSTEM_SEPARATOR) OTHER_SEPARATOR else SYSTEM_SEPARATOR
    array.indices
            .filter { array[it] == otherSeparator }
            .forEach { array[it] = separator }

    // add extra separator on the end to simplify code below
    var lastIsDirectory = true
    if (array[size - 1] != separator) {
        array[size++] = separator
        lastIsDirectory = false
    }

    // adjoining slashes
    run {
        var i = prefix + 1
        while (i < size) {
            if (array[i] == separator && array[i - 1] == separator) {
                System.arraycopy(array, i, array, i - 1, size - i)
                size--
                i--
            }
            i++
        }
    }

    // dot slash
    run {
        var i = prefix + 1
        while (i < size) {
            if (array[i] == separator && array[i - 1] == '.' &&
                    (i == prefix + 1 || array[i - 2] == separator)) {
                if (i == size - 1) {
                    lastIsDirectory = true
                }
                System.arraycopy(array, i + 1, array, i - 1, size - i)
                size -= 2
                i--
            }
            i++
        }
    }

    // double dot slash
    var i = prefix + 2
    outer@ while (i < size) {
        if (array[i] == separator && array[i - 1] == '.' && array[i - 2] == '.' &&
                (i == prefix + 2 || array[i - 3] == separator)) {
            if (i == prefix + 2) {
                return null
            }
            if (i == size - 1) {
                lastIsDirectory = true
            }
            var j: Int = i - 4
            while (j >= prefix) {
                if (array[j] == separator) {
                    // remove b/../ from a/b/../c
                    System.arraycopy(array, i + 1, array, j + 1, size - i)
                    size -= i - j
                    i = j + 1
                    i++
                    continue@outer
                }
                j--
            }
            // remove a/../ from a/../c
            System.arraycopy(array, i + 1, array, prefix, size - i)
            size -= i + 1 - prefix
            i = prefix + 1
        }
        i++
    }

    return when {
    // should never be less than 0
        size <= 0 -> ""

    // should never be less than prefix
        size <= prefix -> String(array, 0, size)

    // keep trailing separator
        lastIsDirectory && keepSeparator -> String(array, 0, size)

    // lose trailing separator
        else -> String(array, 0, size - 1)
    }
}

private fun getPrefixLength(filename: String?): Int {
    if (filename == null) {
        return NOT_FOUND
    }
    val len = filename.length
    if (len == 0) {
        return 0
    }
    var ch0 = filename[0]
    if (ch0 == ':') {
        return NOT_FOUND
    }
    if (len == 1) {
        if (ch0 == '~') {
            return 2  // return a length greater than the input
        }
        return if (ch0.isSeparator()) 1 else 0
    } else {
        if (ch0 == '~') {
            var posUnix = filename.indexOf(UNIX_SEPARATOR, 1)
            var posWin = filename.indexOf(WINDOWS_SEPARATOR, 1)
            if (posUnix == NOT_FOUND && posWin == NOT_FOUND) {
                return len + 1  // return a length greater than the input
            }
            posUnix = if (posUnix == NOT_FOUND) posWin else posUnix
            posWin = if (posWin == NOT_FOUND) posUnix else posWin
            return Math.min(posUnix, posWin) + 1
        }
        val ch1 = filename[1]
        if (ch1 == ':') {
            ch0 = Character.toUpperCase(ch0)
            if (ch0 in 'A'..'Z') {
                return if (len == 2 || !filename[2].isSeparator()) {
                    2
                } else 3
            } else if (ch0 == UNIX_SEPARATOR) {
                return 1
            }
            return NOT_FOUND

        } else if (ch0.isSeparator() && ch1.isSeparator()) {
            var posUnix = filename.indexOf(UNIX_SEPARATOR, 2)
            var posWin = filename.indexOf(WINDOWS_SEPARATOR, 2)
            if (posUnix == NOT_FOUND && posWin == NOT_FOUND || posUnix == 2 || posWin == 2) {
                return NOT_FOUND
            }
            posUnix = if (posUnix == NOT_FOUND) posWin else posUnix
            posWin = if (posWin == NOT_FOUND) posUnix else posWin
            return Math.min(posUnix, posWin) + 1
        } else {
            return if (ch0.isSeparator()) 1 else 0
        }
    }
}

/**
 * Check the input for null bytes, a sign of unsanitized data being passed to to file level functions.
 *
 * This may be used for poison byte attacks.
 * @param path the path to check
 */
private fun failIfNullBytePresent(path: String) {
    val containsNullByte = path.any { it.toInt() == 0 }
    if(containsNullByte) {
        throw IllegalArgumentException("Null byte present in file/path name. There are no " + "known legitimate use cases for such data, but several injection attacks may use it")
    }
}

/**
 * Splits a string into a number of tokens.
 * The text is split by '?' and '*'.
 * Where multiple '*' occur consecutively they are collapsed into a single '*'.
 *
 * @param text  the text to split
 * @return the array of tokens, never null
 */
private fun splitOnTokens(text: String): Array<String> {
    if (text.indexOf('?') == NOT_FOUND && text.indexOf('*') == NOT_FOUND) {
        return arrayOf(text)
    }

    val array = text.toCharArray()
    val list = ArrayList<String>()
    val buffer = StringBuilder()
    var prevChar: Char = 0.toChar()
    for (ch in array) {
        if (ch == '?' || ch == '*') {
            if (buffer.isNotEmpty()) {
                list.add(buffer.toString())
                buffer.setLength(0)
            }
            if (ch == '?') {
                list.add("?")
            } else if (prevChar != '*') { // ch == '*' here; check if previous char was '*'
                list.add("*")
            }
        } else {
            buffer.append(ch)
        }
        prevChar = ch
    }
    if (buffer.isNotEmpty()) {
        list.add(buffer.toString())
    }

    return list.toTypedArray()
}

private fun Char.isSeparator() = this == UNIX_SEPARATOR || this == WINDOWS_SEPARATOR

/**
 * Enumeration of IO case sensitivity.
 *
 *
 * Different filing systems have different rules for case-sensitivity.
 * Windows is case-insensitive, Unix is case-sensitive.
 *
 *
 * This class captures that difference, providing an enumeration to
 * control how filename comparisons should be performed. It also provides
 * methods that use the enumeration to perform comparisons.
 *
 *
 * Wherever possible, you should use the `check` methods in this
 * class to compare filenames.
 */
private enum class IOCase
//-----------------------------------------------------------------------
/**
 * Private constructor.
 *
 * @param constantName  the name
 * @param sensitive  the sensitivity
 */
private constructor(
        /** The enumeration name.  */
        //-----------------------------------------------------------------------
        /**
         * Gets the name of the constant.
         *
         * @return the name of the constant
         */
        val constantName: String,
        /** The sensitivity flag.  */
        @field:Transient
        /**
         * Does the object represent case sensitive comparison.
         *
         * @return true if case sensitive
         */
        val isCaseSensitive: Boolean) : Serializable {

    /**
     * The constant for case sensitive regardless of operating system.
     */
    SENSITIVE("Sensitive", true),

    /**
     * The constant for case insensitive regardless of operating system.
     */
    INSENSITIVE("Insensitive", false),

    /**
     * The constant for case sensitivity determined by the current operating system.
     * Windows is case-insensitive when comparing filenames, Unix is case-sensitive.
     *
     *
     * **Note:** This only caters for Windows and Unix. Other operating
     * systems (e.g. OSX and OpenVMS) are treated as case sensitive if they use the
     * Unix file separator and case-insensitive if they use the Windows file separator
     * (see [java.io.File.separatorChar]).
     *
     *
     * If you derialize this constant of Windows, and deserialize on Unix, or vice
     * versa, then the value of the case-sensitivity flag will change.
     */
    SYSTEM("System", !isSystemWindows());

    /**
     * Replaces the enumeration from the stream with a real one.
     * This ensures that the correct flag is set for SYSTEM.
     *
     * @return the resolved object
     */
    private fun readResolve(): Any {
        return forName(name)
    }

    //-----------------------------------------------------------------------
    /**
     * Compares two strings using the case-sensitivity rule.
     *
     *
     * This method mimics [String.compareTo] but takes case-sensitivity
     * into account.
     *
     * @param str1  the first string to compare, not null
     * @param str2  the second string to compare, not null
     * @return true if equal using the case rules
     * @throws NullPointerException if either string is null
     */
    fun checkCompareTo(str1: String?, str2: String?): Int {
        if (str1 == null || str2 == null) {
            throw NullPointerException("The strings must not be null")
        }
        return if (isCaseSensitive) str1.compareTo(str2) else str1.compareTo(str2, ignoreCase = true)
    }

    /**
     * Compares two strings using the case-sensitivity rule.
     *
     *
     * This method mimics [String.equals] but takes case-sensitivity
     * into account.
     *
     * @param str1  the first string to compare, not null
     * @param str2  the second string to compare, not null
     * @return true if equal using the case rules
     * @throws NullPointerException if either string is null
     */
    fun checkEquals(str1: String?, str2: String?): Boolean {
        if (str1 == null || str2 == null) {
            throw NullPointerException("The strings must not be null")
        }
        return if (isCaseSensitive) str1 == str2 else str1.equals(str2, ignoreCase = true)
    }

    /**
     * Checks if one string starts with another using the case-sensitivity rule.
     *
     *
     * This method mimics [String.startsWith] but takes case-sensitivity
     * into account.
     *
     * @param str  the string to check, not null
     * @param start  the start to compare against, not null
     * @return true if equal using the case rules
     * @throws NullPointerException if either string is null
     */
    fun checkStartsWith(str: String, start: String): Boolean {
        return str.regionMatches(0, start, 0, start.length, ignoreCase = !isCaseSensitive)
    }

    /**
     * Checks if one string ends with another using the case-sensitivity rule.
     *
     *
     * This method mimics [String.endsWith] but takes case-sensitivity
     * into account.
     *
     * @param str  the string to check, not null
     * @param end  the end to compare against, not null
     * @return true if equal using the case rules
     * @throws NullPointerException if either string is null
     */
    fun checkEndsWith(str: String, end: String): Boolean {
        val endLen = end.length
        return str.regionMatches(str.length - endLen, end, 0, endLen, ignoreCase = !isCaseSensitive)
    }

    /**
     * Checks if one string contains another starting at a specific index using the
     * case-sensitivity rule.
     *
     *
     * This method mimics parts of [String.indexOf]
     * but takes case-sensitivity into account.
     *
     * @param str  the string to check, not null
     * @param strStartIndex  the index to start at in str
     * @param search  the start to search for, not null
     * @return the first index of the search String,
     * -1 if no match or `null` string input
     * @throws NullPointerException if either string is null
     * @since 2.0
     */
    fun checkIndexOf(str: String, strStartIndex: Int, search: String): Int {
        val endIndex = str.length - search.length
        if (endIndex >= strStartIndex) {
            for (i in strStartIndex..endIndex) {
                if (checkRegionMatches(str, i, search)) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * Checks if one string contains another at a specific index using the case-sensitivity rule.
     *
     *
     * This method mimics parts of [String.regionMatches]
     * but takes case-sensitivity into account.
     *
     * @param str  the string to check, not null
     * @param strStartIndex  the index to start at in str
     * @param search  the start to search for, not null
     * @return true if equal using the case rules
     * @throws NullPointerException if either string is null
     */
    fun checkRegionMatches(str: String, strStartIndex: Int, search: String): Boolean {
        return str.regionMatches(strStartIndex, search, 0, search.length, ignoreCase = !isCaseSensitive)
    }

    //-----------------------------------------------------------------------
    /**
     * Gets a string describing the sensitivity.
     *
     * @return a string describing the sensitivity
     */
    override fun toString(): String {
        return name
    }

    companion object {

        /** Serialization version.  */
        private const val serialVersionUID = -6343169151696340687L

        //-----------------------------------------------------------------------
        /**
         * Factory method to create an IOCase from a name.
         *
         * @param name  the name to find
         * @return the IOCase object
         * @throws IllegalArgumentException if the name is invalid
         */
        fun forName(name: String): IOCase {
            for (ioCase in IOCase.values()) {
                if (ioCase.name == name) {
                    return ioCase
                }
            }
            throw IllegalArgumentException("Invalid IOCase name: " + name)
        }
    }

}
