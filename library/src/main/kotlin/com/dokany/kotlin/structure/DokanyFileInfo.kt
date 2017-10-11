package com.dokany.kotlin.structure

import com.dokany.kotlin.NativeMethods
import com.dokany.kotlin.toBoolean
import com.dokany.kotlin.toByte
import com.sun.jna.Structure

/**
 * From dokan.h
 *
 * [see here](https://github.com/dokan-dev/dokany/blob/master/dokan/dokan.h)
 *
 * @param context This can be used to carry information between operation. HANDLE This can be whatever type such as {@link com.sun.jna.platform.win32.WinNT.HANDLE},
 * {@link com.sun.jna.Structure}, {@link com.sun.jna.ptr.IntByReference}, {@link com.sun.jna.Pointer} that will help the implementation understand the request context of the
 * event.
 *
 * @param dokanOptions A pointer to {@link DeviceOptions} which was passed to [NativeMethods.DokanMain].
 * @param processId Process id for the thread that originally requested a given I/O operation.
 * @param isDirectory Requesting a directory file. Must be set in [DokanyOperations.ZwCreateFile] if the file object appears to be a directory.
 * @param deleteOnClose Flag if the file has to be deleted during [DokanyOperations.Cleanup] event.
 * @param pagingIo Read or write is paging IO.
 * @param synchronousIo Read or write is synchronous IO.
 * @param noCache Read or write directly from data source without cache.
 * @param writeToEndOfFile If true, write to the current end of file instead of using the Offset parameter.
 */
class DokanyFileInfo(
        context: Long = 0,
        dokanOptions: DeviceOptions? = null,
        processId: Int = 0,
        isDirectory: Boolean = false,
        deleteOnClose: Boolean = false,
        pagingIo: Boolean = false,
        synchronousIo: Boolean = false,
        noCache: Boolean = false,
        writeToEndOfFile: Boolean = false
) : Structure(), Structure.ByReference {
    @JvmField var Context = context
    @JvmField internal var DokanOptions = dokanOptions
    @JvmField internal var ProcessId = processId
    @JvmField internal var IsDirectory = isDirectory.toByte()
    @JvmField var DeleteOnClose = deleteOnClose.toByte()
    @JvmField internal var PagingIo = pagingIo.toByte()
    @JvmField internal var SynchronousIo = synchronousIo.toByte()
    @JvmField internal var Nocache = noCache.toByte()
    @JvmField internal var WriteToEndOfFile = writeToEndOfFile.toByte()

    val deleteOnClose get() = DeleteOnClose.toBoolean()
    val isDirectory get() = IsDirectory.toBoolean()

    override fun toString() = "DokanyFileInfo(context=$Context, dokanOptions=$DokanOptions, processId=$ProcessId, isDirectory=$IsDirectory, deleteOnClose=$DeleteOnClose, pagingIo=$PagingIo, synchronousIo=$SynchronousIo, noCache=$Nocache, writeToEndOfFile=$WriteToEndOfFile)"

    /**
     * Reserved. Used internally by Dokany library. Never modify.
     */
    @JvmField internal var DokanContext: Long = 0

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