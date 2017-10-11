package com.dokany.kotlin.samples.mirrorfs

import com.dokany.kotlin.DokanyFileSystem
import com.dokany.kotlin.Win32FindStreamData
import com.dokany.kotlin.constants.FileAttribute
import com.dokany.kotlin.constants.FileSystemFeature
import com.dokany.kotlin.samples.*
import com.dokany.kotlin.structure.*
import com.dokany.kotlin.utils.UNIX_SEPARATOR
import com.dokany.kotlin.utils.wildcardMatch
import com.sun.jna.platform.win32.*
import java.io.*
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
            dokanyFileInfo: DokanyFileInfo,
            pattern: String?
    ): Set<WinBase.WIN32_FIND_DATA> {
        val normalizedPath = pathToSearch.normalizedPath() ?: return emptySet()
        val safePattern = pattern?.normalizedPath() ?: "*"

        log.trace("findFilesWithPattern for {} with pattern {}", normalizedPath, safePattern)
        log.trace("dokanyFileInfo in findFilesWithPattern: {}", dokanyFileInfo)

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

        val attributesAsInt = Kernel32Util.getFileAttributes(fullPath)

        val attributes = enumSetFromInt<FileAttribute>(attributesAsInt)

        val basicAttributes = Files.getFileAttributeView(Paths.get(fullPath), BasicFileAttributeView::class.java).readAttributes()

        val fileIndex = basicAttributes.fileKey() as? Long ?: 0

        log.trace("rootPath: {}", root)

        val pathWithoutRoot = if(pathPartToRemove == null) {
            normalizedPath
        }
        else {
            normalizedPath.replace(pathPartToRemove, "")
        }.trimTailSeparator()

        log.trace("pathWithoutRoot: {}", pathWithoutRoot)

        return ByHandleFileInfo(
                pathWithoutRoot,
                fileIndex,
                attributes,
                volumeInfo.serialNumber,
                basicAttributes.creationTime().toMillis(),
                basicAttributes.lastAccessTime().toMillis(),
                basicAttributes.lastModifiedTime().toMillis(),
                basicAttributes.size()
        )
    }

    override fun move(oldPath: String, newPath: String, replaceIfExisting: Boolean) {}

    override fun deleteFile(path: String, dokanyFileInfo: DokanyFileInfo) {
        val file = Paths.get(path).toFile()

        if (file.isDirectory) {
            throw AccessDeniedException("Path is a directory: " + file)
        }

        if (!file.exists()) {
            throw FileNotFoundException("Path does not exist: " + file)
        }

        if(file.canDelete()) {
            dokanyFileInfo.DeleteOnClose = 1
        }
    }

    override fun deleteDirectory(path: String, dokanyFileInfo: DokanyFileInfo) {
        val directory = Paths.get(path).toFile()

        if (directory.isFile) {
            throw AccessDeniedException("Path is a file: " + directory)
        }

        if(directory.canDelete()) {
            dokanyFileInfo.DeleteOnClose = 1
        }
    }

    override fun read(path: String, offset: Int, readLength: Int): FileData {
        val normalizedPath = path.normalizedPath() ?: return FileData(byteArrayOf(), 0)
        val fullPath = getFullPath(normalizedPath)

        log.trace("read: {}", fullPath)

        val data = ByteArray(readLength)

        val numRead = FileInputStream(fullPath).use { fis ->
            fis.skip(offset.toLong())
            fis.read(data, 0, readLength)
        }

        return FileData(data, numRead)
    }

    override fun write(path: String, offset: Int, data: ByteArray, writeLength: Int): Int {
        val normalizedPath = path.normalizedPath() ?: return 0
        val fullPath = getFullPath(normalizedPath)

        FileOutputStream(fullPath).use { fos ->
            fos.write(data, offset, writeLength)
        }

        return writeLength - offset
    }

    override fun createEmptyFile(path: String, options: Long, attributes: EnumIntegerSet<FileAttribute>) {}

    override fun createEmptyDirectory(path: String, options: Long, attributes: EnumIntegerSet<FileAttribute>) {}

    override fun flushFileBuffers(path: String) {}

    override fun cleanup(path: String, dokanyFileInfo: DokanyFileInfo) {
        if(dokanyFileInfo.deleteOnClose) {
            Files.delete(Paths.get(path))
        }

        log.trace("dokanyFileInfo in cleanup for path {} {} ", path, dokanyFileInfo)
    }

    override fun close(path: String, dokanyFileInfo: DokanyFileInfo) {
        dokanyFileInfo.Context = 0

        log.trace("dokanyFileInfo in close for path {} {} ", path, dokanyFileInfo)
    }

    override fun getSecurity(path: String, kind: Int, out: ByteArray) = 0
    override fun setSecurity(path: String, kind: Int, data: ByteArray) {}

    override fun truncate(path: String) = 0L

    override fun setAllocationSize(path: String, length: Int) {}

    override fun setEndOfFile(path: String, offset: Int) {}

    override fun setAttributes(path: String, attributes: EnumIntegerSet<FileAttribute>) {
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