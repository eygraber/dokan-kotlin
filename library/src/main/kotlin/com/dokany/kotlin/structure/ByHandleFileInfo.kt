package com.dokany.kotlin.structure

import com.dokany.kotlin.DokanyOperations
import com.dokany.kotlin.DokanyOperationsProxy
import com.dokany.kotlin.trimToSize
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
        fileAttributes: Int = 0,
        private val volumeSerialNumber: Int = 0,
        creationTime: WinBase.FILETIME = WinBase.FILETIME(),
        lastAccessTime: WinBase.FILETIME = WinBase.FILETIME(),
        lastWriteTime: WinBase.FILETIME = WinBase.FILETIME(),
        highSize: Int = 0,
        lowSize: Int = 0,
        highIndex: Int = 0,
        lowIndex: Int = 0
) : Structure(), Structure.ByReference {
    @JvmField internal var dwFileAttributes = fileAttributes
    @JvmField internal var ftCreationTime = creationTime
    @JvmField internal var ftLastAccessTime = lastAccessTime
    @JvmField internal var ftLastWriteTime = lastWriteTime
    @JvmField internal var dwVolumeSerialNumber = volumeSerialNumber
    @JvmField internal var nFileSizeHigh = highSize
    @JvmField internal var nFileSizeLow = lowSize
    @JvmField internal var nFileIndexHigh = highIndex
    @JvmField internal var nFileIndexLow = lowIndex
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

        infoToReceive.nFileSizeHigh = nFileSizeHigh
        infoToReceive.nFileSizeLow = nFileSizeLow

        infoToReceive.nFileIndexHigh = nFileIndexHigh
        infoToReceive.nFileIndexLow = nFileIndexLow

        infoToReceive.dwFileAttributes = dwFileAttributes

        infoToReceive.setTimes(ftCreationTime, ftLastAccessTime, ftLastWriteTime)

        infoToReceive.dwNumberOfLinks = dwNumberOfLinks
        infoToReceive.dwVolumeSerialNumber = dwVolumeSerialNumber
    }

    private fun setTimes(creationTime: WinBase.FILETIME, lastAccessTime: WinBase.FILETIME, lastWriteTime: WinBase.FILETIME) {
        ftCreationTime = creationTime
        ftLastAccessTime = lastAccessTime
        ftLastWriteTime = lastWriteTime
    }
}