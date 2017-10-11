package com.dokany.kotlin.structure

import com.dokany.kotlin.constants.FileSystemFeature

data class VolumeInformation(
        val maxComponentLength: Int = DEFAULT_MAX_COMPONENT_LENGTH,
        val name: String = DEFAULT_VOLUME_NAME,
        val serialNumber: Int = DEFAULT_SERIAL_NUMBER,
        val fileSystemName: String = DEFAULT_FS_NAME,
        val fileSystemFeatures: EnumIntegerSet<FileSystemFeature> = DEFAULT_FS_FEATURES
) {
    companion object {
        const val DEFAULT_MAX_COMPONENT_LENGTH = 256
        const val DEFAULT_SERIAL_NUMBER = 0x12345678
        const val DEFAULT_VOLUME_NAME = "VOLUME1"
        const val DEFAULT_FS_NAME = "DOKANY"

        val DEFAULT_FS_FEATURES = enumIntegerSetOf(FileSystemFeature.CASE_PRESERVED_NAMES)
    }
}