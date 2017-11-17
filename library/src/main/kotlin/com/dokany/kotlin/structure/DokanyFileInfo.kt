package com.dokany.kotlin.structure

import com.dokany.kotlin.toBoolean
import com.dokany.kotlin.toByte
import com.sun.jna.Structure

data class FileInfo(
        var context: Long,
        val pid: Int,
        var isDirectory: Boolean,
        val deleteOnClose: Boolean,
        val pagingIo: Boolean,
        val synchronousIo: Boolean,
        val noCache: Boolean,
        val writeToEndOfFile: Boolean
)

/**
 * From dokan.h
 *
 * [see here](https://github.com/dokan-dev/dokany/blob/master/dokan/dokan.h)
 */
class DokanyFileInfo : Structure(), Structure.ByReference {
    /**
     * Reserved. Used internally by Dokany library. Never modify.
     */
    @JvmField internal var DokanContext: Long = 0

    @JvmField internal var Context: Long = 0
    @JvmField internal var DokanOptions: DeviceOptions? = null
    @JvmField internal var ProcessId: Int = 0
    @JvmField internal var IsDirectory: Byte = false.toByte()
    @JvmField internal var DeleteOnClose: Byte = false.toByte()
    @JvmField internal var PagingIo: Byte = false.toByte()
    @JvmField internal var SynchronousIo: Byte = false.toByte()
    @JvmField internal var Nocache: Byte = false.toByte()
    @JvmField internal var WriteToEndOfFile: Byte = false.toByte()

    fun toFileInfo() = FileInfo(
            Context,
            ProcessId,
            IsDirectory.toBoolean(),
            DeleteOnClose.toBoolean(),
            PagingIo.toBoolean(),
            SynchronousIo.toBoolean(),
            Nocache.toBoolean(),
            WriteToEndOfFile.toBoolean()
    )

    override fun toString() = "DokanyFileInfo(context=$Context, dokanOptions=$DokanOptions, processId=$ProcessId, isDirectory=$IsDirectory, deleteOnClose=$DeleteOnClose, pagingIo=$PagingIo, synchronousIo=$SynchronousIo, noCache=$Nocache, writeToEndOfFile=$WriteToEndOfFile)"

    override fun getFieldOrder() = listOf(
            "Context",
            "DokanContext",
            "DokanOptions",
            "ProcessId",
            "IsDirectory",
            "DeleteOnClose",
            "PagingIo",
            "SynchronousIo",
            "Nocache",
            "WriteToEndOfFile"
    )
}