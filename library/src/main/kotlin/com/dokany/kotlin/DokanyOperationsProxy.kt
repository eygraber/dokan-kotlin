package com.dokany.kotlin

import com.dokany.kotlin.constants.ErrorCode
import com.dokany.kotlin.constants.FileAttribute
import com.dokany.kotlin.constants.NtStatus
import com.dokany.kotlin.structure.*
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinError.ERROR_READ_FAULT
import com.sun.jna.platform.win32.WinError.ERROR_WRITE_FAULT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import org.slf4j.Logger

/**
 * Implementation of [DokanyOperations] which connects to [DokanyFileSystem].
 */
class DokanyOperationsProxy(
        fileSystem: DokanyFileSystem,
        volumeInfo: VolumeInformation,
        freeSpace: FreeSpace,
        log: Logger = logger()
) : DokanyOperations() {
    init {
        ZwCreateFile = _ZwCreateFile()
        Cleanup = _Cleanup(fileSystem, log)
        CloseFile = _CloseFile(log)
        ReadFile = _ReadFile(fileSystem, log)
        WriteFile = _WriteFile(fileSystem, log)
        FlushFileBuffers = _FlushFileBuffers(fileSystem, log)
        GetFileInformation = _GetFileInformation(fileSystem, log)
        FindFiles = _FindFiles(fileSystem, log)
        FindFilesWithPattern = _FindFilesWithPattern(fileSystem, log)
        SetFileAttributes = _SetFileAttributes(fileSystem, log)
        SetFileTime = _SetFileTime(fileSystem, log)
        DeleteFile = _DeleteFile(fileSystem, log)
        DeleteDirectory = _DeleteDirectory(fileSystem, log)
        MoveFile = _MoveFile(fileSystem, log)
        SetEndOfFile = _SetEndOfFile(fileSystem, log)
        SetAllocationSize = _SetAllocationSize(fileSystem, log)
        LockFile = _LockFile(fileSystem, log)
        UnlockFile = _UnlockFile(fileSystem, log)
        GetDiskFreeSpace = _GetDiskFreeSpace(freeSpace, log)
        GetVolumeInformation = _GetVolumeInformation(volumeInfo, log)
        Mounted = _Mounted(fileSystem, log)
        Unmounted = _Unmounted(fileSystem, log)
        GetFileSecurity = _GetFileSecurity(fileSystem, log)
        SetFileSecurity = _SetFileSecurity(fileSystem, log)
        FindStreams = _FindStreams(fileSystem, log)
    }
    companion object {
        const val MAX_PATH = 260

        private fun isSkipFile(path: WString) = path.normalizedPath()?.let(this::isSkipFile) ?: false

        private fun isSkipFile(normalizedPath: String): Boolean {
            var toReturn = false

            val pathLowerCase = normalizedPath.toLowerCase()

            if (pathLowerCase.endsWith("desktop.ini")
                    || pathLowerCase.endsWith("autorun.inf")
                    || pathLowerCase.endsWith("folder.jpg")
                    || pathLowerCase.endsWith("folder.gif")) {
                // TODO: re-enable logging
                toReturn = true
            }
            return toReturn
        }
    }

    /*-
	 *                          |                    When the file...
	This argument:           |             Exists            Does not exist
	-------------------------+------------------------------------------------------
	CREATE_ALWAYS            |            Truncates             Creates
	CREATE_NEW         +-----------+        Fails               Creates
	OPEN_ALWAYS     ===| does this |===>    Opens               Creates
	OPEN_EXISTING      +-----------+        Opens                Fails
	TRUNCATE_EXISTING        |            Truncates              Fails
	 */
    private class _ZwCreateFile : IZwCreateFile {
        override fun callback(
                rawPath: WString,
                securityContext: WinBase.SECURITY_ATTRIBUTES,
                rawDesiredAccess: Int,
                rawFileAttributes: Int,
                rawShareAccess: Int,
                rawCreateDisposition: Int,
                rawCreateOptions: Int,
                dokanyFileInfo: DokanyFileInfo
        ): Long {
            // Normalize path
            val normalizedPath = rawPath.normalizedPath()

            Kernel32.INSTANCE.CreateFile(normalizedPath, rawDesiredAccess, rawShareAccess, securityContext, rawCreateDisposition, rawFileAttributes, null)

            return ErrorCode.SUCCESS.mask.toLong()
        }
    }

    private class _Cleanup(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : ICleanup {
        override fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo
        ) {
            if (isSkipFile(rawPath)) {
                return
            }

            try {
                rawPath.normalizedPath()?.let { normalizedPath ->
                    fileSystem.cleanup(normalizedPath, dokanyFileInfo)

                    log.trace("Cleaned up: {}", normalizedPath)
                } ?: log.warn("Couldn't cleanup file because {} couldn't be normalized", rawPath)
            } catch (t: Throwable) {
                log.warn("Error in cleaning up file: {}", rawPath, t)
            }

        }
    }

    private class _CloseFile(
            private val log: Logger
    ) : ICloseFile {
        override fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo
        ) {
            if (isSkipFile(rawPath)) {
                return
            }

            try {
                // TODO: Can close always be done here not matter the FS?
                // dokanyFileInfo.Context = 0;
                val normalizedPath = rawPath.normalizedPath()
                // fileSystem.close(normalizedPath, dokanyFileInfo);

                log.trace("Closed file: {}", normalizedPath)
            } catch (e: Throwable) {
                log.warn("Error in closing file: {}", rawPath, e)
            }

        }
    }

    private class _FindFiles(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IFindFiles {
        override fun callback(
                rawPath: WString,
                rawFillFindData: IFillWin32FindData,
                dokanyFileInfo: DokanyFileInfo
        ): Long {
            return _FindFilesWithPattern(fileSystem, log).callback(rawPath, WString(""), rawFillFindData, dokanyFileInfo)
        }
    }

    private class _FindFilesWithPattern(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IFindFilesWithPattern {
        override fun callback(
                fileName: WString,
                searchPattern: WString,
                rawFillFindData: IFillWin32FindData,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val pathToSearch = fileName.normalizedPath()
            log.trace("FindFilesWithPattern {}", pathToSearch)

            if(pathToSearch == null) {
                log.warn("Couldn't find files with pattern because {} couldn't be normalized", fileName)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                val filesFound = fileSystem.findFilesWithPattern(pathToSearch, dokanyFileInfo, searchPattern.toString())
                log.debug("Found {} paths", filesFound.size)
                try {
                    filesFound.forEach { file ->
                        log.trace("file in find: {}", file.fileName)
                        rawFillFindData.fillWin32FindData(file, dokanyFileInfo)
                    }
                } catch (e: Error) {
                    log.warn("Error filling Win32FindData", e)
                }

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _ReadFile(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IReadFile {
        override fun callback(
                rawPath: WString,
                rawBuffer: Pointer,
                rawBufferLength: Int,
                rawReadLength: IntByReference,
                rawOffset: Long,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.debug("ReadFile: {} with readLength ", normalizedPath, rawBufferLength)

            if (dokanyFileInfo.isDirectory) {
                log.trace("isDir:will throw file not found error")
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            if(normalizedPath == null) {
                log.warn("Couldn't read file because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                val data = fileSystem.read(normalizedPath, rawOffset.toInt(), rawBufferLength)
                val numRead = data.length
                log.debug("numRead: {}", numRead)

                if (numRead > 0) {
                    rawBuffer.write(0, data.bytes, 0, numRead)
                    log.debug("wrote data length: {}", data.bytes.size)
                }

                rawReadLength.value = numRead

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode(ERROR_READ_FAULT.toLong())
            }
        }
    }

    private class _WriteFile(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IWriteFile {
        override fun callback(
                rawPath: WString,
                rawBuffer: Pointer,
                rawNumberOfBytesToWrite: Int,
                rawNumberOfBytesWritten: IntByReference,
                rawOffset: Long,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.debug("WriteFile: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't write file because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                val data = ByteArray(rawNumberOfBytesToWrite)
                rawBuffer.read(0L, data, 0, rawNumberOfBytesToWrite)
                val written = fileSystem.write(normalizedPath, rawOffset.toInt(), data, rawNumberOfBytesToWrite)
                rawNumberOfBytesWritten.value = written
                log.debug("Wrote this number of bytes: {}", written)
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode(ERROR_WRITE_FAULT.toLong())
            }

        }
    }

    private class _FlushFileBuffers(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IFlushFileBuffers {
        override fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("FlushFileBuffers: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't flush file buffer because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.flushFileBuffers(normalizedPath)
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode(ERROR_WRITE_FAULT.toLong())
            }

        }
    }

    private class _GetFileInformation(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IGetFileInformation {
        override fun callback(
                fileName: WString,
                handleFileInfo: ByHandleFileInfo,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedFileName = fileName.normalizedPath()
            log.debug("GetFileInformation: {}", normalizedFileName)
            log.trace("dokanyFileInfo in getinfo: {}", dokanyFileInfo)

            if (isSkipFile(fileName)) {
                return NtStatus.FILE_INVALID.mask.toLong()
            }

            if(normalizedFileName == null) {
                log.warn("Couldn't get file info because {} couldn't be normalized", fileName)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                val retrievedInfo = fileSystem.getInfo(normalizedFileName)
                if(retrievedInfo == null) {
                    log.warn("Error reading info; couldn't get info")
                    ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
                }
                else {
                    retrievedInfo.copyTo(handleFileInfo)
                    ErrorCode.SUCCESS.mask.toLong()
                }
            } catch (t: Throwable) {
                log.warn("Error reading info: {}", t.message)
                t.toErrorCode(ERROR_WRITE_FAULT.toLong())
            }

        }
    }

    private class _SetFileAttributes(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : ISetFileAttributes {
        override fun callback(
                rawPath: WString,
                rawAttributes: Int,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            val attribs: EnumIntegerSet<FileAttribute> = enumSetFromInt(rawAttributes)
            log.trace("SetFileAttributes as {} for {}", attribs, normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't set file attributes because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.setAttributes(normalizedPath, attribs)
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode(ERROR_WRITE_FAULT.toLong())
            }

        }
    }

    private class _SetFileTime(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : ISetFileTime {
        override fun callback(
                rawPath: WString,
                rawCreationTime: WinBase.FILETIME,
                rawLastAccessTime: WinBase.FILETIME,
                rawLastWriteTime: WinBase.FILETIME,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("SetFileTime for {}; creationTime = {}; lastAccessTime = {}; lastWriteTime = {}", normalizedPath, rawCreationTime, rawLastAccessTime, rawLastWriteTime)

            if(normalizedPath == null) {
                log.warn("Couldn't set file time because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.setTime(normalizedPath, rawCreationTime, rawLastAccessTime, rawLastWriteTime)
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode(ERROR_WRITE_FAULT.toLong())
            }

        }
    }

    private class _DeleteFile(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IDeleteFile {
        override fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("DeleteFile: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't delete file because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.deleteFile(normalizedPath, dokanyFileInfo)

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _DeleteDirectory(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IDeleteDirectory {
        override fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("DeleteDirectory: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't delete directory because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.deleteDirectory(normalizedPath, dokanyFileInfo)

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _MoveFile(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IMoveFile {
        override fun callback(
                rawPath: WString,
                rawNewFileName: WString,
                rawReplaceIfExisting: Boolean,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val oldNormalizedPath = rawPath.normalizedPath()
            val newNormalizedPath = rawNewFileName.normalizedPath()
            log.debug("trace: {} to {}; replace existing? ", oldNormalizedPath, newNormalizedPath, rawReplaceIfExisting)

            if(oldNormalizedPath == null) {
                log.warn("Couldn't move file because from path {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            if(newNormalizedPath == null) {
                log.warn("Couldn't move file because to path {} couldn't be normalized", rawNewFileName)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.move(oldNormalizedPath, newNormalizedPath, rawReplaceIfExisting)

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _SetEndOfFile(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : ISetEndOfFile {
        override fun callback(
                rawPath: WString,
                rawByteOffset: Long,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("SetEndOfFile: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't set end of file because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.setEndOfFile(normalizedPath, rawByteOffset.toInt())
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _SetAllocationSize(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : ISetAllocationSize {
        override fun callback(
                rawPath: WString,
                rawLength: Long,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("SetAllocationSize: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't set allocation size because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.setAllocationSize(normalizedPath, rawLength.toInt())
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _LockFile(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : ILockFile {
        override fun callback(
                rawPath: WString,
                rawByteOffset: Long,
                rawLength: Long,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("LockFile: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't lock file because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.lock(normalizedPath, rawByteOffset.toInt(), rawLength.toInt())

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _UnlockFile(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IUnlockFile {
        override fun callback(
                rawPath: WString,
                rawByteOffset: Long,
                rawLength: Long,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("UnlockFile: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't unlock file because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                fileSystem.unlock(normalizedPath, rawByteOffset.toInt(), rawLength.toInt())

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _GetDiskFreeSpace(
            private val freeSpace: FreeSpace,
            private val log: Logger
    ) : IGetDiskFreeSpace {
        override fun callback(
                freeBytesAvailable: LongByReference,
                totalNumberOfBytes: LongByReference,
                totalNumberOfFreeBytes: LongByReference,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            log.trace("GetDiskFreeSpace")
            log.trace("rawFreeBytesAvailable: {}", freeBytesAvailable.value)
            log.debug("rawTotalNumberOfBytes", totalNumberOfBytes)
            log.debug("rawTotalNumberOfFreeBytes", totalNumberOfFreeBytes)

            // rawTotalNumberOfBytes.setValue(new LONGLONG(freeSpace.getTotalBytes()));

            // These two are the same unless per-user quotas are enabled
            // rawTotalNumberOfFreeBytes.setValue(new LONGLONG(freeSpace.getFreeBytes()));

            // If per-user quotas are being used, this value may be less than the total number of free bytes on a disk
            freeBytesAvailable.value = freeSpace.freeBytes
            log.trace("new rawFreeBytesAvailable: {}", freeBytesAvailable.value)

            return 0
        }
    }

    private class _GetVolumeInformation(
            private val volumeInfo: VolumeInformation,
            private val log: Logger
    ) : IGetVolumeInformation {
        override fun callback(
                rawVolumeNameBuffer: Pointer,
                rawVolumeNameSize: Int,
                rawVolumeSerialNumber: IntByReference,
                rawMaximumComponentLength: IntByReference,
                rawFileSystemFlags: IntByReference,
                rawFileSystemNameBuffer: Pointer,
                rawFileSystemNameSize: Int,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            log.trace("GetVolumeInformation")

            return try {
                rawVolumeNameBuffer.setWideString(0L, volumeInfo.name.trimToSize(rawVolumeNameSize))

                rawVolumeSerialNumber.value = volumeInfo.serialNumber

                rawMaximumComponentLength.value = volumeInfo.maxComponentLength

                rawFileSystemFlags.value = volumeInfo.fileSystemFeatures.toInt()

                rawFileSystemNameBuffer.setWideString(0L, volumeInfo.fileSystemName.trimToSize(rawFileSystemNameSize))

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _Mounted(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IMounted {
        override fun mounted(dokanyFileInfo: DokanyFileInfo): Long {
            return try {
                fileSystem.mounted()
                log.info("Dokany File System mounted")
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _Unmounted(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IUnmounted {
        override fun unmounted(dokanyFileInfo: DokanyFileInfo): Long {
            return try {
                fileSystem.unmounted()
                log.info("Dokany File System unmounted")
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _GetFileSecurity(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IGetFileSecurity {
        override fun callback(
                rawPath: WString,
                rawSecurityInformation: Int,
                rawSecurityDescriptor: Pointer,
                rawSecurityDescriptorLength: Int,
                rawSecurityDescriptorLengthNeeded: IntByReference,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("SetFileSecurity: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't get file security because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                val out = ByteArray(rawSecurityDescriptorLength)
                val expectedLength = fileSystem.getSecurity(normalizedPath, rawSecurityInformation, out)
                rawSecurityDescriptor.write(0L, out, 0, rawSecurityDescriptorLength)
                rawSecurityDescriptorLengthNeeded.value = expectedLength

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _SetFileSecurity(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : ISetFileSecurity {
        override fun callback(
                rawPath: WString,
                rawSecurityInformation: Int,
                rawSecurityDescriptor: Pointer,
                rawSecurityDescriptorLength: Int,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("SetFileSecurity: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't set file security because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                val data = ByteArray(rawSecurityDescriptorLength)
                rawSecurityDescriptor.read(0L, data, 0, rawSecurityDescriptorLength)
                fileSystem.setSecurity(normalizedPath, rawSecurityInformation, data)

                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }

    private class _FindStreams(
            private val fileSystem: DokanyFileSystem,
            private val log: Logger
    ) : IFindStreams {
        override fun callback(
                rawPath: WString,
                rawFillFindData: IFillWin32FindStreamData,
                dokanyFileInfo: DokanyFileInfo
        ): Long {

            val normalizedPath = rawPath.normalizedPath()
            log.trace("FindStreams: {}", normalizedPath)

            if(normalizedPath == null) {
                log.warn("Couldn't find streams because {} couldn't be normalized", rawPath)
                return ErrorCode.ERROR_FILE_NOT_FOUND.mask.toLong()
            }

            return try {
                val streams = fileSystem.findStreams(normalizedPath)
                log.debug("Found {} streams", streams.size)
                streams.forEach { file -> rawFillFindData.callback(file, dokanyFileInfo) }
                ErrorCode.SUCCESS.mask.toLong()
            } catch (t: Throwable) {
                t.toErrorCode()
            }

        }
    }
}