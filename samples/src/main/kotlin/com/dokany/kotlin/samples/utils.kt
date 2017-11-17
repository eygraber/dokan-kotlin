package com.dokany.kotlin.samples

import com.dokany.kotlin.utils.FilenameUtils
import com.dokany.kotlin.utils.UNIX_SEPARATOR
import com.sun.jna.platform.win32.WinBase
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
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

internal fun File.recursiveSize(): Long {
    var size = 0L

    return try {
        Files.walkFileTree(toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE.apply {
                size += attrs.size()
            }

            override fun visitFileFailed(file: Path, exc: IOException?) = FileVisitResult.CONTINUE
            override fun postVisitDirectory(dir: Path, exc: IOException?) = FileVisitResult.CONTINUE
        })

        size
    } catch (e: IOException) {
        0L
    }
}