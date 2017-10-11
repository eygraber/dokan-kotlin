package com.dokany.kotlin.structure

import com.dokany.kotlin.*
import com.dokany.kotlin.constants.FileAttribute
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinBase
import java.util.*

/**
 * Contains information that the [DokanyOperations.GetFileInformation] function retrieves.
 *
 * The identifier that is stored in the nFileIndexHigh and nFileIndexLow members is called the file ID. Support for file IDs is file system-specific. File IDs are not guaranteed to
 * be unique over time, because file systems are free to reuse them. In some cases, the file ID for a file can change over time.
 *
 * In the FAT file system, the file ID is generated from the first cluster of the containing directory and the byte offset within the directory of the entry for the file. Some
 * defragmentation products change this byte offset. (Windows in-box defragmentation does not.) Thus, a FAT file ID can change over time.Renaming a file in the FAT file system can
 * also change the file ID, but only if the new file name is longer than the old one.
 *
 * In the NTFS file system, a file keeps the same file ID until it is deleted. You can replace one file with another file without changing the file ID by using the ReplaceFile
 * function. However, the file ID of the replacement file, not the replaced file, is retained as the file ID of the resulting file.
 *
 * Not all file systems can record creation and last access time, and not all file systems record them in the same manner. For example, on a Windows FAT file system, create time
 * has a resolution of 10 milliseconds, write time has a resolution of 2 seconds, and access time has a resolution of 1 day (the access date). On the NTFS file system, access time
 * has a resolution of 1 hour.
 *
 * @param filePath the path of the file.
 * @param fileIndex see above
 * @param fileAttributes The file attributes of a file. For possible values and their descriptions, see File Attribute Constants. The FILE_ATTRIBUTE_SPARSE_FILE attribute on the file is set if any
 * of the streams of the file have ever been sparse.
 * @param volumeSerialNumber the volume serial number
 * @param creationTime Specifies when a file or directory was created. If the underlying file system does not support creation time, this member is zero.
 * @param lastAccessTime For a file, specifies when the file was last read from, written to, or for executable files, run. For a directory,
 * specifies when the directory is created. If the underlying file system does not support last access time, this member is zero. On the FAT file system, the specified date for
 * both files and directories is correct, but the time of day is always set to midnight.
 * @param lastWriteTime For a file, specifies when the file was last written to, truncated, or overwritten, for example, when WriteFile or SetEndOfFile are used.
 * The date and time are not updated when file attributes or security descriptors are changed. For a directory, specifies when the directory is created. If the
 * underlying file system does not support last write time, this member is zero.
 * @param fileSize the size of the file.
 */
class ByHandleFileInfo(
        private var filePath: String? = null,
        private var fileIndex: Long = 0,
        private val fileAttributes: EnumIntegerSet<FileAttribute> = enumIntegerSetOf(),
        private val volumeSerialNumber: Int = 0,
        private val creationTime: Long = 0,
        private val lastAccessTime: Long = 0,
        private val lastWriteTime: Long = 0,
        private var fileSize: Long = 0
) : Structure(), Structure.ByReference {
    private val now = getCurrentFiletime()
    private val largeFileSize = fileSize.toLargeInt()
    private val largeIndex = fileIndex.toLargeInt()

    @JvmField internal var dwFileAttributes = fileAttributes.toInt()
    @JvmField internal var ftCreationTime = if(creationTime == 0L) now else creationTime.msToFiletime()
    @JvmField internal var ftLastAccessTime = if(lastAccessTime == 0L) now else lastAccessTime.msToFiletime()
    @JvmField internal var ftLastWriteTime = if(lastWriteTime == 0L) now else lastWriteTime.msToFiletime()
    @JvmField internal var dwVolumeSerialNumber = volumeSerialNumber
    @JvmField internal var nFileSizeHigh = if(fileSize != 0L) largeFileSize?.high?.toInt() ?: fileSize.toInt() else 0
    @JvmField internal var nFileSizeLow = if(fileSize != 0L) largeFileSize?.low?.toInt() ?: fileSize.toInt() else 0
    @JvmField internal var nFileIndexHigh = if(fileIndex != 0L) largeIndex?.high?.toInt() ?: fileIndex.toInt() else 0
    @JvmField internal var nFileIndexLow = if(fileIndex != 0L) largeIndex?.low?.toInt() ?: fileIndex.toInt() else 0
    @JvmField internal var dwNumberOfLinks: Int = 1

    fun toWin32FindData(): WinBase.WIN32_FIND_DATA {
        val cFileName = filePath?.trimToSize(DokanyOperationsProxy.MAX_PATH)?.toCharArray() ?: CharArray(1)
        val cAlternateFileName = CharArray(1)

        return WinBase.WIN32_FIND_DATA(
                dwFileAttributes,
                ftCreationTime, ftLastAccessTime, ftLastWriteTime,
                nFileSizeHigh, nFileSizeLow,
                0, 0,
                cFileName, cAlternateFileName
        )
    }

    override fun getFieldOrder() = listOf(
            "dwFileAttributes",
            "ftCreationTime", "ftLastAccessTime", "ftLastWriteTime",
            "dwVolumeSerialNumber",
            "nFileSizeHigh", "nFileSizeLow",
            "dwNumberOfLinks",
            "nFileIndexHigh", "nFileIndexLow"
    )

    fun copyTo(infoToReceive: ByHandleFileInfo) {
        if (Objects.isNull(infoToReceive)) {
            throw IllegalStateException("infoToReceive cannot be null")
        }

        infoToReceive.filePath = filePath

        infoToReceive.setSize(fileSize, nFileSizeHigh, nFileSizeLow)

        infoToReceive.setIndex(fileIndex, nFileIndexHigh, nFileIndexLow)

        infoToReceive.dwFileAttributes = dwFileAttributes

        infoToReceive.setTimes(ftCreationTime, ftLastAccessTime, ftLastWriteTime)

        infoToReceive.dwNumberOfLinks = dwNumberOfLinks
        infoToReceive.dwVolumeSerialNumber = dwVolumeSerialNumber
    }

    private fun setSize(size: Long, sizeHigh: Int, sizeLow: Int) {
        fileSize = size

        val largeInt = size.toLargeInt(sizeHigh, sizeLow)

        nFileSizeHigh = if (size != 0L && sizeHigh == 0) largeInt?.high?.toInt() ?: size.toInt() else size.toInt()
        nFileSizeLow = if (size != 0L && sizeLow == 0) largeInt?.low?.toInt() ?: size.toInt() else size.toInt()

    }

    private fun setIndex(index: Long, indexHigh: Int, indexLow: Int) {
        fileIndex = index

        val largeInt = index.toLargeInt(indexHigh, indexLow)

        nFileIndexHigh = if (index != 0L && indexHigh == 0) largeInt?.high?.toInt() ?: index.toInt() else index.toInt()
        nFileIndexLow = if (index != 0L && indexLow == 0) largeInt?.low?.toInt() ?: index.toInt() else index.toInt()
    }

    private fun setTimes(creationTime: WinBase.FILETIME, lastAccessTime: WinBase.FILETIME, lastWriteTime: WinBase.FILETIME) {
        ftCreationTime = creationTime
        ftLastAccessTime = lastAccessTime
        ftLastWriteTime = lastWriteTime
    }
}