package com.dokany.kotlin.samples.mirrorfs

import com.dokany.kotlin.DokanyDriver
import com.dokany.kotlin.constants.FileSystemFeature
import com.dokany.kotlin.constants.MountOption
import com.dokany.kotlin.samples.logger
import com.dokany.kotlin.structure.DeviceOptions
import com.dokany.kotlin.structure.FreeSpace
import com.dokany.kotlin.structure.VolumeInformation
import com.dokany.kotlin.structure.enumIntegerSetOf
import java.util.*

fun main(args: Array<String>) {
    MountMirrorFS().start()
}

class MountMirrorFS {
    private val log = logger()

    internal fun start() {
        log.info("Starting Dokany MirrorFS")

        val mountPoint = "K:\\"
        val threadCount: Short = 1

        val mountOptions = enumIntegerSetOf(
                MountOption.DEBUG_MODE,
                MountOption.STD_ERR_OUTPUT,
                MountOption.MOUNT_MANAGER
        )

        val uncName = ""
        val timeout = 10000L
        val allocationUnitSize = 4096L
        val sectorSize = 4096L

        val deviceOptions = DeviceOptions(
                100,
                mountPoint,
                threadCount,
                mountOptions,
                uncName,
                timeout,
                allocationUnitSize,
                sectorSize
        )

        val fsFeatures = enumIntegerSetOf(
                FileSystemFeature.CASE_PRESERVED_NAMES,
                FileSystemFeature.CASE_SENSITIVE_SEARCH,
                FileSystemFeature.PERSISTENT_ACLS,
                FileSystemFeature.SUPPORTS_REMOTE_STORAGE,
                FileSystemFeature.UNICODE_ON_DISK
        )

        val volumeInfo = VolumeInformation(
                VolumeInformation.DEFAULT_MAX_COMPONENT_LENGTH,
                "Mirror",
                -0x6789abce,
                "Dokany MirrorFS",
                fsFeatures
        )

        val freeSpace = FreeSpace(200_000, 200)

        val mirrorFS = MirrorFS(
                deviceOptions,
                "C:\\Users\\eli\\Desktop\\mirror_this",
                volumeInfo,
                freeSpace,
                Date()
        )

        val dokanyDriver = DokanyDriver(deviceOptions, mirrorFS, volumeInfo, freeSpace)

        dokanyDriver.start()
    }
}