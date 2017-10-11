package com.dokany.kotlin.constants

import com.dokany.kotlin.structure.EnumInteger

enum class MountOption(
        override val mask: Int,
        val description: String
) : EnumInteger {
    DEBUG_MODE(1, "Enable ouput debug message"),

    STD_ERR_OUTPUT(2, "Enable ouput debug message to stderr"),

    ALT_STREAM(4, "Use alternate stream"),

    WRITE_PROTECTION(8, "Enable mount drive as write-protected"),

    NETWORK_DRIVE(16, "Use network drive - Dokan network provider need to be installed"),

    REMOVABLE_DRIVE(32, "Use removable drive"),

    MOUNT_MANAGER(64, "Use mount manager"),

    CURRENT_SESSION(128, "Mount the drive on current session only"),

    FILELOCK_USER_MODE(256, "Enable Lockfile/Unlockfile operations. Otherwise Dokan will take care of it");

    val isReadOnly by lazy { this == WRITE_PROTECTION }
}