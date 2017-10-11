package com.dokany.kotlin.structure

import com.dokany.kotlin.constants.MountOption
import com.dokany.kotlin.toWString
import com.sun.jna.Structure

/**
 * Dokany mount options used to describe Dokany device behavior. This is the same structure as PDOKAN_OPTIONS (dokan.h) in the C++ version of Dokany.
 *
 * @param version Version of the Dokany features requested (version "123" is equal to Dokany version 1.2.3). Defaults to 100.
 * @param mountPoint Mount point. Can be M:\\ (drive letter) or C:\\mount\\dokany (path in NTFS).
 * @param threadCount Number of threads to be used internally by Dokany library. More threads will handle more events at the same time.
 * @param mountOptions Features enabled for the mount.
 * @param uncName UNC name used for network volume.
 * @param timeout Max timeout in milliseconds of each request before Dokany gives up.
 * @param allocationUnitSize Allocation Unit Size of the volume.
 * @param sectorSize Sector size of the volume.
 */
class DeviceOptions(
        internal val version: Short = 105,
        internal val mountPoint: String? = null,
        internal val threadCount: Short = 0,
        internal val mountOptions: EnumIntegerSet<MountOption> = enumIntegerSetOf(),
        internal val uncName: String? = null,
        internal val timeout: Long = 0,
        internal val allocationUnitSize: Long = 0,
        internal val sectorSize: Long = 0
) : Structure(), Structure.ByReference {
    @JvmField internal var Version = version
    @JvmField internal var MountPoint = mountPoint?.toWString()
    @JvmField internal var ThreadCount = threadCount
    @JvmField internal var Options = mountOptions.toInt()
    @JvmField internal var UNCName = uncName?.toWString()
    @JvmField internal var Timeout = timeout
    @JvmField internal var AllocationUnitSize = allocationUnitSize
    @JvmField internal var SectorSize = sectorSize

    @JvmField internal var GlobalContext: Long = 0

    override fun toString() = "DeviceOptions(version=$version, mountPoint=$mountPoint, threadCount=$threadCount, mountOptions=${mountOptions.joinToString { it.name }}, uncName=$uncName, timeout=$timeout, allocationUnitSize=$allocationUnitSize, sectorSize=$sectorSize)"

    override fun getFieldOrder() = listOf(
            "Version",
            "ThreadCount",
            "Options",
            "GlobalContext",
            "MountPoint",
            "UNCName",
            "Timeout",
            "AllocationUnitSize",
            "SectorSize"
    )
}