package com.dokany.kotlin

import com.dokany.kotlin.constants.FileAttribute
import com.dokany.kotlin.constants.MountOption
import com.dokany.kotlin.structure.*
import com.sun.jna.platform.win32.WinBase
import java.util.*

abstract class DokanyFileSystem(
        deviceOptions: DeviceOptions,
        root: String,
        protected val volumeInfo: VolumeInformation,
        protected val freeSpace: FreeSpace,
        protected val rootCreationDate: Date
) {
    protected val allocationUnitSize = deviceOptions.allocationUnitSize
    protected val sectorSize = deviceOptions.sectorSize
    protected val timeout = deviceOptions.timeout

    protected val isDebug = deviceOptions.mountOptions.contains(MountOption.DEBUG_MODE)
    protected val isDebugStdErr = deviceOptions.mountOptions.contains(MountOption.STD_ERR_OUTPUT)

    protected val root: String = root.normalizedPath() ?: throw IllegalArgumentException("Bad root")

    abstract fun mounted()
    abstract fun unmounted()
    abstract fun doesPathExist(path: String): Boolean
    abstract fun findFilesWithPattern(pathToSearch: String, dokanyFileInfo: DokanyFileInfo, pattern: String?): Set<WinBase.WIN32_FIND_DATA>
    abstract fun findStreams(pathToSearch: String): Set<Win32FindStreamData>

    /**
     * Only used if dokan option UserModeLock is enabled
     */
    abstract fun unlock(path: String, offset: Int, length: Int)

    /**
     * Only used if dokan option UserModeLock is enabled
     */
    abstract fun lock(path: String, offset: Int, length: Int)

    abstract fun move(oldPath: String, newPath: String, replaceIfExisting: Boolean)
    abstract fun deleteFile(path: String, dokanyFileInfo: DokanyFileInfo)
    abstract fun deleteDirectory(path: String, dokanyFileInfo: DokanyFileInfo)
    abstract fun read(path: String, offset: Int, readLength: Int): FileData
    abstract fun write(path: String, offset: Int, data: ByteArray, writeLength: Int): Int
    abstract fun createEmptyFile(path: String, options: Long, attributes: EnumIntegerSet<FileAttribute>)
    abstract fun createEmptyDirectory(path: String, options: Long, attributes: EnumIntegerSet<FileAttribute>)
    abstract fun flushFileBuffers(path: String)
    abstract fun cleanup(path: String, dokanyFileInfo: DokanyFileInfo)
    abstract fun close(path: String, dokanyFileInfo: DokanyFileInfo)
    abstract fun getSecurity(path: String, kind: Int, out: ByteArray): Int
    abstract fun setSecurity(path: String, kind: Int, data: ByteArray)
    abstract fun truncate(path: String): Long
    abstract fun setAllocationSize(path: String, length: Int)
    abstract fun setEndOfFile(path: String, offset: Int)
    abstract fun setAttributes(path: String, attributes: EnumIntegerSet<FileAttribute>)
    abstract fun getInfo(path: String): ByHandleFileInfo?
    abstract fun setTime(path: String, creation: WinBase.FILETIME, lastAccess: WinBase.FILETIME, lastModification: WinBase.FILETIME)
}