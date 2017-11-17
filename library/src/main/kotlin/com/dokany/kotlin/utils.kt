package com.dokany.kotlin

import com.dokany.kotlin.constants.ErrorCode
import com.dokany.kotlin.constants.NtStatus
import com.dokany.kotlin.utils.FilenameUtils
import com.dokany.kotlin.utils.UNIX_SEPARATOR
import com.sun.jna.WString
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinNT
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.util.*

internal fun String.toWString() = WString(this)

internal fun WString.normalizedPath() = FilenameUtils.normalizedPath(toString())
internal fun String.normalizedPath() = FilenameUtils.normalizedPath(this)

internal fun String.trimToSize(len: Int) =
        substring(0, minOf(length, len)).let {
            if(it.startsWith(UNIX_SEPARATOR)) {
                it.substring(1, it.length)
            }
            else {
                it
            }
        }

private const val ZERO_BYTE: Byte = 0
internal fun Boolean.toByte() = if(this) 1 else ZERO_BYTE
internal fun Byte.toBoolean() = this != ZERO_BYTE

internal fun Long.toLargeInt(high: Int = 0, low: Int = 0) =
        if (this != 0L && (high == 0 || low == 0)) {
            WinNT.LARGE_INTEGER(this)
        }
        else {
            null
        }

internal fun getCurrentFiletime() = WinBase.FILETIME(Date())
internal fun Long.msToFiletime() = WinBase.FILETIME(Date(this))

internal fun <T : Any> T.logger() = LoggerFactory.getLogger(javaClass)

internal fun Throwable.toErrorCode(defaultVal: Int = NtStatus.UNSUCCESSFUL.mask) = when {
    this is FileNotFoundException -> ErrorCode.ERROR_FILE_NOT_FOUND.mask
    this is FileAlreadyExistsException -> ErrorCode.ERROR_ALREADY_EXISTS.mask
    else -> defaultVal
}

internal data class HighLow(
        val high: Int,
        val low: Int
)

internal fun Long.toHighLow() = HighLow(
        (this shr 32 and 0xffffffffL).toInt(),
        (this and 0xffffffffL).toInt()
)
