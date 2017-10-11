package com.dokany.kotlin

import com.dokany.kotlin.constants.FileSystemFeature
import com.dokany.kotlin.constants.MountOption
import com.dokany.kotlin.constants.NtStatus
import com.dokany.kotlin.structure.ByHandleFileInfo
import com.dokany.kotlin.structure.DeviceOptions
import com.dokany.kotlin.structure.DokanyFileInfo
import com.sun.jna.Callback
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference

/**
 *
 * Dokany API callbacks interface. This is an internal class and should not used directly by code outside com.dokany.java.
 *
 * A struct of callbacks that describe all Dokany API operation that will be called when Windows accesses the file system.
 *
 * If an error occurs, return [NtStatus].
 *
 * All these callbacks can be set to `null` or return [NtStatus.NOT_IMPLEMENTED] if you don't want to support one of them. Be aware that returning such a @JvmField internal varue to
 * important callbacks such as [DokanyOperations.ZwCreateFile] or [DokanyOperations.ReadFile] would make the file system not working or unstable.
 *
 * This is the same struct as <i>_DOKAN_OPERATIONS</i> (dokan.h) in the C++ version of Dokany.</remarks>
 *
 */
open class DokanyOperations: Structure() {
    @JvmField var ZwCreateFile: IZwCreateFile? = null

    @JvmField internal var Cleanup: ICleanup? = null

    @JvmField internal var CloseFile: ICloseFile? = null

    @JvmField internal var ReadFile: IReadFile? = null

    @JvmField internal var WriteFile: IWriteFile? = null

    @JvmField internal var FlushFileBuffers: IFlushFileBuffers? = null

    @JvmField internal var GetFileInformation: IGetFileInformation? = null

    @JvmField internal var FindFiles: IFindFiles? = null

    @JvmField internal var FindFilesWithPattern: IFindFilesWithPattern? = null

    @JvmField internal var SetFileAttributes: ISetFileAttributes? = null

    @JvmField internal var SetFileTime: ISetFileTime? = null

    @JvmField internal var DeleteFile: IDeleteFile? = null

    @JvmField internal var DeleteDirectory: IDeleteDirectory? = null

    @JvmField internal var MoveFile: IMoveFile? = null

    @JvmField internal var SetEndOfFile: ISetEndOfFile? = null

    @JvmField internal var SetAllocationSize: ISetAllocationSize? = null

    @JvmField internal var LockFile: ILockFile? = null

    @JvmField internal var UnlockFile: IUnlockFile? = null

    @JvmField internal var GetDiskFreeSpace: IGetDiskFreeSpace? = null

    @JvmField internal var GetVolumeInformation: IGetVolumeInformation? = null

    @JvmField internal var Mounted: IMounted? = null

    @JvmField internal var Unmounted: IUnmounted? = null

    @JvmField internal var GetFileSecurity: IGetFileSecurity? = null

    @JvmField internal var SetFileSecurity: ISetFileSecurity? = null

    @JvmField internal var FindStreams: IFindStreams? = null

    override fun getFieldOrder() = listOf(
            "ZwCreateFile",
            "Cleanup",
            "CloseFile",
            "ReadFile",
            "WriteFile",
            "FlushFileBuffers",
            "GetFileInformation",
            "FindFiles",
            "FindFilesWithPattern",
            "SetFileAttributes",
            "SetFileTime",
            "DeleteFile",
            "DeleteDirectory",
            "MoveFile",
            "SetEndOfFile",
            "SetAllocationSize",
            "LockFile",
            "UnlockFile",
            "GetDiskFreeSpace",
            "GetVolumeInformation",
            "Mounted",
            "Unmounted",
            "GetFileSecurity",
            "SetFileSecurity",
            "FindStreams"
    )

    /**
     * CreateFile is called each time a request is made on a file system object.
     *
     * If the file is a directory, this method is also called. In this case, the method should return [NtStatus.SUCCESS] when that directory can be opened and
     * [DokanyFileInfo.isDirectory] has to be set to *true*. [DokanyFileInfo.context] can be used to store
     * data FileStream that can be retrieved in all other request related to the context.
     *
     * [See here for more information about the parameters of this callback.](https://msdn.microsoft.com/en-us/library/windows/hardware/ff566424(v=vs.85).aspx)
     */
    interface IZwCreateFile : Callback {
        /**
         * @param rawPath Path requested by the Kernel on the File System.
         * @param securityContext ??
         * @param rawDesiredAccess ?? Permissions for file or directory.
         * @param rawFileAttributes Provides attributes for files and directories.
         *[See here](https://msdn.microsoft.com/en-us/library/system.io.fileattributes(v=vs.110).aspx)
         * @param rawShareAccess Type of share access to other threads. Device and intermediate drivers usually set ShareAccess to zero, which gives the caller exclusive access to
         * the open file.
         * @param rawCreateDisposition
         * @param rawCreateOptions Represents advanced options for creating a File object.
         * [See here](https://msdn.microsoft.com/en-us/library/system.io.fileoptions(v=vs.110).aspx)
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                securityContext: WinBase.SECURITY_ATTRIBUTES,
                rawDesiredAccess: Int,
                rawFileAttributes: Int,
                rawShareAccess: Int,
                rawCreateDisposition: Int,
                rawCreateOptions: Int,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     * Receipt of this request indicates that the last handle for a file object that is associated with the target device object has been closed (but, due to outstanding I/O
     * requests, might not have been released).
     *
     * Cleanup is requested before [DokanyOperations.CloseFile] is called.
     *
     */
    interface ICleanup : Callback {
        /**
         *
         * @param rawPath
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         */
        fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo)
    }

    /**
     * CloseFile is called at the end of the life of the context. Receipt of this request indicates that the last handle of the file object that is associated with the target
     * device object has been closed and released. All outstanding I/O requests have been completed or canceled.
     *
     * CloseFile is requested after [DokanyOperations.Cleanup] is called. Anything remaining in [DokanyFileInfo.context] has to be cleared
     * before return.
     *
     */
    interface ICloseFile : Callback {
        /**
         *
         * @param rawPath
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         */
        fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo)
    }

    /**
     * ReadFile callback on the file previously opened in [DokanyOperations.ZwCreateFile]. It can be called by different thread at the same time, therefore the read has to be
     * thread safe.
     *
     */
    interface IReadFile : Callback {
        /**
         *
         * @param rawPath
         * @param rawBuffer
         * @param rawBufferLength
         * @param rawReadLength
         * @param rawOffset
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawBuffer: Pointer,
                rawBufferLength: Int,
                rawReadLength: IntByReference,
                rawOffset: Long,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     * WriteFile callback on the file previously opened in [DokanyOperations.ZwCreateFile] It can be called by different thread at the same time, therefore the write/context has to
     * be thread safe.
     *
     */
    interface IWriteFile : Callback {
        /**
         *
         * @param rawPath
         * @param rawBuffer
         * @param rawNumberOfBytesToWrite
         * @param rawNumberOfBytesWritten
         * @param rawOffset
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawBuffer: Pointer,
                rawNumberOfBytesToWrite: Int,
                rawNumberOfBytesWritten: IntByReference,
                rawOffset: Long,
                dokanyFileInfo: DokanyFileInfo): Long

    }

    /**
     * Clears buffers for this context and causes any buffered data to be written to the file.
     *
     */
    interface IFlushFileBuffers : Callback {
        /**
         *
         * @param rawPath
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Get specific informations on a file.
     *
     */
    interface IGetFileInformation : Callback {
        /**
         *
         * @param fileName
         * @param handleFileInfo
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                fileName: WString,
                handleFileInfo: ByHandleFileInfo,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * List all files in the path requested.
     */
    interface IFindFiles : Callback {
        /**
         *
         * @param rawPath
         * @param rawFillFindData
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawFillFindData: IFillWin32FindData,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Same as [DokanyOperations.FindFiles] but with a search pattern to filter the result.
     *
     */
    interface IFindFilesWithPattern : Callback {
        /**
         *
         * @param fileName
         * @param searchPattern
         * @param rawFillFindData
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                fileName: WString,
                searchPattern: WString,
                rawFillFindData: IFillWin32FindData,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Set file attributes on a specific file.
     */
    interface ISetFileAttributes : Callback {
        /**
         *
         * @param rawPath
         * @param rawAttributes
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawAttributes: Int,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Set file times on a specific file.
     */
    interface ISetFileTime : Callback {
        /**
         *
         * @param rawPath path to file or directory
         * @param rawCreationTime time of creation
         * @param rawLastAccessTime time of last access
         * @param rawLastWriteTime time of last modification
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawCreationTime: WinBase.FILETIME,
                rawLastAccessTime: WinBase.FILETIME,
                rawLastWriteTime: WinBase.FILETIME,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Check if it is possible to delete a file.
     *
     * You should NOT delete the file in this method, but instead you must only check whether you can delete the file or not, and return [NtStatus.SUCCESS] (when you can
     * delete it) or appropriate error codes such as [NtStatus.ACCESS_DENIED], [NtStatus.OBJECT_PATH_NOT_FOUND], [NtStatus.OBJECT_NAME_NOT_FOUND].
     *
     * [DokanyOperations.DeleteFile] will also be called with [DokanyFileInfo.deleteOnClose] set to *false* to notify the driver when the file is no longer
     * requested to be deleted.
     *
     * When you return [NtStatus.SUCCESS], you get a [DokanyOperations.Cleanup]> call afterwards with [DokanyFileInfo.deleteOnClose] set to *true* and only
     * then you have to actually delete the file being closed.
     *
     * @see [DokanyOperations.DeleteDirectory]
     */
    interface IDeleteFile : Callback {
        /**
         *
         * @param rawPath
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Check if it is possible to delete a directory.
     *
     * @see [DokanyOperations.DeleteFile] for more specifics.
     */
    interface IDeleteDirectory : Callback {
        /**
         *
         * @param rawPath
         * @param dokanyFileInfo [DokanyFileInfo] with information about the directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Move a file or directory to a new location.
     */
    interface IMoveFile : Callback {
        /**
         *
         * @param rawPath
         * @param rawNewFileName
         * @param rawReplaceIfExisting
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawNewFileName: WString,
                rawReplaceIfExisting: Boolean,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * SetEndOfFile is used to truncate or extend a file (physical file size).
     */
    interface ISetEndOfFile : Callback {
        /**
         *
         * @param rawPath
         * @param rawByteOffset
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawByteOffset: Long,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * SetAllocationSize is used to truncate or extend a file.
     */
    interface ISetAllocationSize : Callback {
        /**
         *
         * @param rawPath
         * @param rawLength
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawLength: Long,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Lock file at a specific offset and data length. This is only used if [MountOption.FILELOCK_USER_MODE] is enabled.
     */
    interface ILockFile : Callback {
        /**
         *
         * @param rawPath
         * @param rawByteOffset
         * @param rawLength
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawByteOffset: Long,
                rawLength: Long,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Unlock file at a specific offset and data length. This is only used if [MountOption.FILELOCK_USER_MODE] is enabled.
     */
    interface IUnlockFile : Callback {
        /**
         *
         * @param rawPath
         * @param rawByteOffset
         * @param rawLength
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawByteOffset: Long,
                rawLength: Long,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Retrieves information about the amount of space that is available on a disk volume, which is the total amount of space, the total amount of free space, and the total amount
     * of free space available to the user that is associated with the calling thread.
     *
     * Neither this method nor [DokanyOperations.GetVolumeInformation] save the [DokanyFileInfo.context]. Before these methods are called,
     * [DokanyOperations.ZwCreateFile] may not be called. (ditto [DokanyOperations.CloseFile] and [DokanyOperations.Cleanup]).
     *
     */
    interface IGetDiskFreeSpace : Callback {
        /**
         *
         * @param freeBytesAvailable
         * @param totalNumberOfBytes
         * @param totalNumberOfFreeBytes
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                freeBytesAvailable: LongByReference,
                totalNumberOfBytes: LongByReference,
                totalNumberOfFreeBytes: LongByReference,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Retrieves information about the file system and volume associated with the specified root directory.
     *
     * Neither this method nor [DokanyOperations.GetVolumeInformation] save the [DokanyFileInfo.context]. Before these methods are called,
     * [DokanyOperations.ZwCreateFile] may not be called. (ditto [DokanyOperations.CloseFile] and [DokanyOperations.Cleanup]).
     *
     * @see [FileSystemFeature.READ_ONLY_VOLUME] is automatically added to the `features` if WriteProtection was specified when
     * the volume was mounted.
     *
     * If [NtStatus.NOT_IMPLEMENTED] is returned, the Dokany kernel driver use following settings by default:
     *
     *
     *  * rawVolumeSerialNumber = 0x19831116
     *  * rawMaximumComponentLength = 256
     *  * rawFileSystemFlags = CaseSensitiveSearch, CasePreservedNames, SupportsRemoteStorage, UnicodeOnDisk
     *  * rawFileSystemNameBuffer = NTFS
     *
     */
    interface IGetVolumeInformation : Callback {
        /**
         *
         * @param rawVolumeNameBuffer
         * @param rawVolumeNameSize
         * @param rawVolumeSerialNumber
         * @param rawMaximumComponentLength
         * @param rawFileSystemFlags
         * @param rawFileSystemNameBuffer
         * @param rawFileSystemNameSize
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawVolumeNameBuffer: Pointer,
                rawVolumeNameSize: Int,
                rawVolumeSerialNumber: IntByReference,
                rawMaximumComponentLength: IntByReference,
                /* FileSystemFeatures */ rawFileSystemFlags: IntByReference,
                rawFileSystemNameBuffer: Pointer,
                rawFileSystemNameSize: Int,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Is called when Dokany succeeded mounting the volume.
     */
    interface IMounted : Callback {
        fun mounted(
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Is called when Dokany succeeded unmounting the volume.
     */
    interface IUnmounted : Callback {
        fun unmounted(
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Get specified information about the security of a file or directory.
     *
     * Supported since version 0.6.0. You must specify the version in [DeviceOptions.version].
     */
    interface IGetFileSecurity : Callback {
        /**
         *
         * @param rawPath
         * @param rawSecurityInformation
         * @param rawSecurityDescriptor
         * @param rawSecurityDescriptorLength
         * @param rawSecurityDescriptorLengthNeeded
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                /* SecurityInformation */ rawSecurityInformation: Int,
                rawSecurityDescriptor: Pointer,
                rawSecurityDescriptorLength: Int,
                rawSecurityDescriptorLengthNeeded: IntByReference,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     * Sets the security of a file or directory object.
     *
     * Supported since version 0.6.0. You must specify the version in [DeviceOptions.version].
     */
    interface ISetFileSecurity : Callback {
        /**
         *
         * @param rawPath
         * @param rawSecurityInformation
         * @param rawSecurityDescriptor
         * @param rawSecurityDescriptorLength
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawSecurityInformation: Int,
                // @TODO: This is a pointer??
                rawSecurityDescriptor: Pointer,
                rawSecurityDescriptorLength: Int,
                dokanyFileInfo: DokanyFileInfo): Long
    }


    interface IFillWin32FindData : Callback {
        /**
         *
         * @param rawFillFindData
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         */
        fun fillWin32FindData(
                rawFillFindData: WinBase.WIN32_FIND_DATA,
                dokanyFileInfo: DokanyFileInfo)
    }

    /**
     *
     * Retrieve all NTFS Streams informations on the file. This is only called if [MountOption.ALT_STREAM] is enabled.
     */
    interface IFindStreams : Callback {
        /**
         *
         * @param rawPath
         * @param rawFillFindData
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         * @return [NtStatus]
         */
        fun callback(
                rawPath: WString,
                rawFillFindData: IFillWin32FindStreamData,
                dokanyFileInfo: DokanyFileInfo): Long
    }

    /**
     *
     *
     */

    interface IFillWin32FindStreamData : Callback {
        /**
         *
         * @param rawFillFindData
         * @param dokanyFileInfo [DokanyFileInfo] with information about the file or directory.
         */
        fun callback(
                rawFillFindData: Win32FindStreamData,
                dokanyFileInfo: DokanyFileInfo)
    }

    interface Win32FindStreamDataInterface {
        fun length(value: Long)
        fun cFileName(): CharArray
    }
}