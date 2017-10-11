package com.dokany.kotlin

import com.dokany.kotlin.constants.MountError
import com.dokany.kotlin.structure.DeviceOptions
import com.dokany.kotlin.structure.FreeSpace
import com.dokany.kotlin.structure.VolumeInformation
import com.dokany.kotlin.structure.enumFromInt
import com.sun.jna.WString

class DokanyDriver(
        private val deviceOptions: DeviceOptions,
        private val fileSystem: DokanyFileSystem,
        private val volumeInfo: VolumeInformation,
        private val freeSpace: FreeSpace
) {
    private val log = logger()

    init {
        log.info("Dokany version: {}", NativeMethods.DokanVersion())
        log.info("Dokany driver version: {}", NativeMethods.DokanDriverVersion())
    }

    /**
     * Calls [NativeMethods.DokanMain]. Has [java.lang.Runtime.addShutdownHook] which calls [.shutdown]
     */
    fun start() {
        try {
            val mountStatus = NativeMethods.DokanMain(deviceOptions, DokanyOperationsProxy(fileSystem, volumeInfo, freeSpace))

            if (mountStatus < 0) {
                throw IllegalStateException(enumFromInt<MountError>(mountStatus).description)
            }

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    shutdown()
                }
            })
        } catch (t: Throwable) {
            log.warn("Error mounting", t)
            throw t
        }

    }

    /**
     * Calls [.stop].
     */
    fun shutdown() {
        deviceOptions.mountPoint?.let(this::stop)
    }

    /**
     * Calls [NativeMethods.DokanUnmount] and [NativeMethods.DokanRemoveMountPoint]
     *
     * @param mountPoint
     */
    fun stop(mountPoint: String) {
        log.info("Unmount and shutdown: {}", mountPoint)
        NativeMethods.DokanUnmount(mountPoint[0])
        NativeMethods.DokanRemoveMountPoint(WString(mountPoint))
    }
}