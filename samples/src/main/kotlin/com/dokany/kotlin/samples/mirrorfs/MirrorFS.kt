package com.dokany.kotlin.samples.mirrorfs

import com.dokany.kotlin.DokanyFileSystem
import com.dokany.kotlin.NativeMethods
import com.dokany.kotlin.Win32FindStreamData
import com.dokany.kotlin.constants.*
import com.dokany.kotlin.samples.*
import com.dokany.kotlin.structure.*
import com.dokany.kotlin.utils.UNIX_SEPARATOR
import com.dokany.kotlin.utils.wildcardMatch
import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.util.*
import kotlin.streams.toList

class MirrorFS(
        deviceOptions: DeviceOptions,
        root: String,
        volumeInfo: VolumeInformation,
        freeSpace: FreeSpace,
        rootCreationDate: Date
) : DokanyFileSystem(deviceOptions, root, volumeInfo, freeSpace, rootCreationDate) {
    private val log = logger()

    private val isCaseSensitive: Boolean by lazy {
        volumeInfo.fileSystemFeatures.contains(FileSystemFeature.CASE_PRESERVED_NAMES)
    }

    init {
        val rootFile = File(root)
        if(!rootFile.exists()) {
            throw FileNotFoundException("Cannot find root directory $root")
        }
        if(!rootFile.isDirectory) {
            throw IllegalArgumentException("Specified root is not a directory ($root)")
        }
    }

    override fun createFile(
            path: String,
            securityContext: WinBase.SECURITY_ATTRIBUTES,
            desiredAccess: EnumIntSet<FileAccess>,
            fileAttributes: EnumIntSet<FileAttribute>,
            shareAccess: EnumIntSet<FileShare>,
            createDisposition: FileMode,
            createOptions: EnumIntSet<FileOptions>,
            fileInfo: FileInfo
    ): NtStatus {
        val fullPath = getFullPath(path)

        val file = File(fullPath)

        var addShareReadFlag = false

        fun getShareAccess() = if(addShareReadFlag) enumSetFromInt<FileShare>((shareAccess.toInt() and FileShare.READ.mask)) else shareAccess

        val k32 = Kernel32.INSTANCE

        val fileAttribs = k32.GetFileAttributes(file.absolutePath)
        val isFileAttribsValid = fileAttribs != WinNT.INVALID_FILE_ATTRIBUTES
        val isDirectoryAttribute = fileAttribs and WinNT.FILE_ATTRIBUTE_DIRECTORY == WinNT.FILE_ATTRIBUTE_DIRECTORY
        val isHiddenAttribute = fileAttribs and WinNT.FILE_ATTRIBUTE_HIDDEN == WinNT.FILE_ATTRIBUTE_HIDDEN
        val isSystemAttribute = fileAttribs and WinNT.FILE_ATTRIBUTE_SYSTEM == WinNT.FILE_ATTRIBUTE_SYSTEM
        val isReadOnlyAttribute = fileAttribs and WinNT.FILE_ATTRIBUTE_READONLY == WinNT.FILE_ATTRIBUTE_READONLY

        if(isFileAttribsValid && isDirectoryAttribute) {
            if(!createOptions.contains(FileOptions.NON_DIRECTORY_FILE)) {
                fileInfo.isDirectory = true
                addShareReadFlag = true
            }
            else {
                return NtStatus.IS_A_DIRECTORY
            }
        }

        var status = NtStatus.SUCCESS

        if(fileInfo.isDirectory) {
            if(createDisposition == FileMode.CREATE_NEW || createDisposition == FileMode.OPEN) {
                if(!k32.CreateDirectory(fullPath, securityContext)) {
                    val error = k32.GetLastError()
                    if(error != Kernel32.ERROR_ALREADY_EXISTS || createDisposition == FileMode.CREATE_NEW) {
                        status = enumFromInt(NativeMethods.DokanNtStatusFromWin32(error))
                    }

                }
            }

            if(status == NtStatus.SUCCESS) {
                if(isFileAttribsValid && !isDirectoryAttribute && createOptions.contains(FileOptions.DIRECTORY_FILE)) {
                    return NtStatus.NOT_A_DIRECTORY
                }

                val handle = k32.CreateFile(
                        fullPath,
                        desiredAccess.toInt(),
                        getShareAccess().toInt(),
                        securityContext,
                        FileMode.OPEN.mask,
                        fileAttributes.toInt() and createOptions.toInt() and FileOptions.BACKUP_SEMANTICS.mask,
                        null
                )

                if(handle == Kernel32.INVALID_HANDLE_VALUE) {
                    val error = k32.GetLastError()
                    status = enumFromInt(NativeMethods.DokanNtStatusFromWin32(error))
                }
                else {
                    fileInfo.context = Pointer.nativeValue(handle.pointer)

                    if(createDisposition == FileMode.OPEN && isFileAttribsValid) {
                        return NtStatus.OBJECT_NAME_COLLISION
                    }
                }
            }
        }
        else {
            if(isFileAttribsValid &&
                    ((!fileAttributes.contains(FileAttribute.HIDDEN)) && isHiddenAttribute) ||
                    ((!fileAttributes.contains(FileAttribute.SYSTEM)) && isSystemAttribute) &&
                            (createDisposition == FileMode.TRUNCATE || createDisposition == FileMode.CREATE)) {
                return NtStatus.ACCESS_DENIED
            }

            if(isFileAttribsValid &&
                    isReadOnlyAttribute ||
                    fileAttributes.contains(FileAttribute.READONLY) &&
                    createOptions.contains(FileOptions.DELETE_ON_CLOSE) ) {
                return NtStatus.CANNOT_DELETE
            }

            val addGenericWrite = createDisposition == FileMode.TRUNCATE
            val accessToUse = if(addGenericWrite) enumSetFromInt(desiredAccess.toInt() and FileAccess.GENERIC_WRITE.mask) else desiredAccess

            val handle = k32.CreateFile(
                    fullPath,
                    accessToUse.toInt(),
                    shareAccess.toInt(),
                    securityContext,
                    createDisposition.mask,
                    fileAttributes.toInt() and createOptions.toInt(),
                    null
            )

            if(handle == Kernel32.INVALID_HANDLE_VALUE) {
                val error = k32.GetLastError()

                status = enumFromInt(NativeMethods.DokanNtStatusFromWin32(error))
            }
            else {
                if(isFileAttribsValid && createDisposition == FileMode.TRUNCATE) {
                    val allAttribs = (fileAttributes.toInt() and createOptions.toInt() or fileAttribs).toLong()
                    k32.SetFileAttributes(fullPath, WinDef.DWORD(allAttribs))
                }

                fileInfo.context = Pointer.nativeValue(handle.pointer)

                if(createDisposition == FileMode.OPEN || createDisposition == FileMode.CREATE) {
                    val error = k32.GetLastError()
                    if(error == WinNT.ERROR_ALREADY_EXISTS) {
                        status = NtStatus.OBJECT_NAME_COLLISION
                    }
                }
            }
        }

        return status
    }

    override fun mounted() {
        log.trace("Mounted")
    }

    override fun unmounted() {
        log.trace("UnMounted")
    }

    override fun doesPathExist(path: String) = try {
        Paths.get(path).toFile().exists()
    } catch(e: Throwable) {
        log.error("There was an error checking if the path exists", e)
        false
    }.also { result ->
        log.trace("doesPathExist {} - {}", path, result)
    }

    override fun findFilesWithPattern(
            pathToSearch: String,
            fileInfo: FileInfo,
            pattern: String?
    ): Set<WinBase.WIN32_FIND_DATA> {
        val normalizedPath = pathToSearch.normalizedPath() ?: return emptySet()
        val safePattern = pattern?.normalizedPath() ?: "*"

        log.trace("findFilesWithPattern for {} with pattern {}", normalizedPath, safePattern)
        log.trace("fileInfo in findFilesWithPattern: {}", fileInfo)

        val startingPath = getFullPath(normalizedPath)

        val patternToMatch = startingPath + safePattern

        return Files.list(Paths.get(startingPath))
                .map { it.toString().normalizedPath() }
                .filterNotNull()
                .filter { wildcardMatch(it, patternToMatch, isCaseSensitive) }
                .peek { log.trace("Found match: {}", it) }
                .toList()
                .let { paths ->
                    HashSet<WinBase.WIN32_FIND_DATA>(paths.size).apply {
                        paths.forEach {
                            try {
                                val info = getInfo(it, patternToMatch.replace("*", ""))
                                add(info.toWin32FindData())
                            }
                            catch(e: Exception) {
                                when(e) {
                                    is IOException, is Win32Exception -> {
                                        log.warn("Could not retrieve file info for {}", normalizedPath, e)
                                    }
                                    else -> throw e
                                }
                            }
                        }
                    }
                }
    }

    private fun getFullPath(normalizedPath: String) =
            if(normalizedPath.startsWith(UNIX_SEPARATOR)) {
                if(normalizedPath.length == 1) {
                    root
                }
                else {
                    root + normalizedPath
                }
            }
            else {
                normalizedPath
            }.replace("$UNIX_SEPARATOR$UNIX_SEPARATOR", UNIX_SEPARATOR.toString())

    override fun getInfo(path: String) = path.normalizedPath()?.let {
        getInfo(it, null)
    }

    private fun getInfo(normalizedPath: String, pathPartToRemove: String?): ByHandleFileInfo {
        log.trace("getInfo for {} with pathPartToRemove {}", normalizedPath, pathPartToRemove)

        val fullPath = getFullPath(normalizedPath)

        log.trace("rootPath: {}", root)

        val pathWithoutRoot = if(pathPartToRemove == null) {
            normalizedPath
        }
        else {
            normalizedPath.replace(pathPartToRemove, "")
        }.trimTailSeparator()

        log.trace("pathWithoutRoot: {}", pathWithoutRoot)

        return if(normalizedPath.length == 1) {
            ByHandleFileInfo(filePath = pathWithoutRoot, fileAttributes = Kernel32.INSTANCE.GetFileAttributes(fullPath))
        }
        else {
            val p = Memory(WinBase.WIN32_FIND_DATA.sizeOf().toLong())
            val findHandle = Kernel32.INSTANCE.FindFirstFile(fullPath, p)
            if(findHandle == WinNT.INVALID_HANDLE_VALUE) {
                throw Win32Exception(Kernel32.INSTANCE.GetLastError())
            }

            val find = WinBase.WIN32_FIND_DATA(p)

            ByHandleFileInfo(
                    pathWithoutRoot,
                    find.dwFileAttributes,
                    volumeInfo.serialNumber,
                    find.ftCreationTime,
                    find.ftLastAccessTime,
                    find.ftLastWriteTime,
                    find.nFileSizeHigh,
                    find.nFileSizeLow,
                    0,0
            ).apply {
                Kernel32.INSTANCE.FindClose(findHandle)
            }
        }
    }

    override fun move(oldPath: String, newPath: String, replaceIfExisting: Boolean) {}

    override fun deleteFile(path: String, fileInfo: FileInfo) {
        val file = Paths.get(path).toFile()

        if (file.isDirectory) {
            throw AccessDeniedException("Path is a directory: " + file)
        }

        if (!file.exists()) {
            throw FileNotFoundException("Path does not exist: " + file)
        }

        if(file.canDelete()) {
            // fileInfo.deleteOnClose = true
        }
    }

    override fun deleteDirectory(path: String, fileInfo: FileInfo) {
        val directory = Paths.get(path).toFile()

        if (directory.isFile) {
            throw AccessDeniedException("Path is a file: " + directory)
        }

        if(directory.canDelete()) {
            // fileInfo.deleteOnClose = true
        }
    }

    override fun read(path: String, offset: Int, readLength: Int): FileData {
        val normalizedPath = path.normalizedPath() ?: return FileData(byteArrayOf(), 0)
        val fullPath = getFullPath(normalizedPath)

        log.trace("read: {}", fullPath)

        val data = ByteArray(readLength)

        val numRead = RandomAccessFile(fullPath, "rw").use { file ->
            file.seek(offset.toLong())
            file.read(data, 0, readLength)
        }

        return FileData(data, numRead)
    }

    override fun write(path: String, offset: Int, data: ByteArray, writeLength: Int): Int {
        val normalizedPath = path.normalizedPath() ?: return 0
        val fullPath = getFullPath(normalizedPath)

        RandomAccessFile(fullPath, "rw").use { file ->
            file.seek(offset.toLong())
            file.write(data, 0, writeLength)
        }

        return writeLength
    }

    override fun createEmptyFile(path: String, options: Long, attributes: EnumIntSet<FileAttribute>) {}

    override fun createEmptyDirectory(path: String, options: Long, attributes: EnumIntSet<FileAttribute>) {}

    override fun flushFileBuffers(path: String) {}

    override fun cleanup(path: String, fileInfo: FileInfo) {
        if(fileInfo.deleteOnClose) {
            Files.delete(Paths.get(path))
        }

        log.trace("fileInfo in cleanup for path {} {} ", path, fileInfo)
    }

    override fun close(path: String, fileInfo: FileInfo) {
        fileInfo.context = 0

        log.trace("fileInfo in close for path {} {} ", path, fileInfo)
    }

    override fun getSecurity(path: String, kind: Int, out: ByteArray) = 0
    override fun setSecurity(path: String, kind: Int, data: ByteArray) {}

    override fun truncate(path: String) = 0L

    override fun setAllocationSize(path: String, length: Int) {}

    override fun setEndOfFile(path: String, offset: Int) {}

    override fun setAttributes(path: String, attributes: EnumIntSet<FileAttribute>) {
        Kernel32.INSTANCE.SetFileAttributes(path, WinDef.DWORD(attributes.toInt().toLong()))
    }

    override fun unlock(path: String, offset: Int, length: Int) {
        throw UnsupportedOperationException("unlock: Not yet implemented")
    }

    override fun lock(path: String, offset: Int, length: Int) {
        throw UnsupportedOperationException("lock: Not yet implemented")
    }

    override fun findStreams(pathToSearch: String): Set<Win32FindStreamData> {
        throw UnsupportedOperationException("findStreams: Not yet implemented")
    }

    override fun setTime(path: String, creation: WinBase.FILETIME, lastAccess: WinBase.FILETIME, lastModification: WinBase.FILETIME) {
        val attributes = Files.getFileAttributeView(Paths.get(path), BasicFileAttributeView::class.java)

        attributes.setTimes(creation.toFileTime(), lastAccess.toFileTime(), lastModification.toFileTime())
    }
}