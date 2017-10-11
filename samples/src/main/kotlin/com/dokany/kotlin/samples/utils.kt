package com.dokany.kotlin.samples

import com.dokany.kotlin.utils.FilenameUtils
import com.dokany.kotlin.utils.UNIX_SEPARATOR
import com.sun.jna.platform.win32.WinBase
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.attribute.FileTime
import java.util.stream.Stream

@Suppress("UNCHECKED_CAST")
internal fun <T> Stream<T?>.filterNotNull(): Stream<T> = filter { it != null } as Stream<T>

internal fun <T : Any> T.logger() = LoggerFactory.getLogger(javaClass)

internal fun String.normalizedPath() = FilenameUtils.normalizedPath(this)

internal fun String.trimTailSeparator() =
        if(endsWith(UNIX_SEPARATOR)) {
            substring(0, lastIndex)
        }
        else {
            this
        }

internal fun File.canDelete() = renameTo(this)

internal fun WinBase.FILETIME.toFileTime() = FileTime.from(toDate().toInstant())