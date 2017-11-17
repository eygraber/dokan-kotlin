package com.dokany.kotlin.constants

import com.dokany.kotlin.structure.EnumInt
import com.sun.jna.platform.win32.WinNT

enum class FileShare(override val mask: Int) : EnumInt {
    DELETE(WinNT.FILE_SHARE_DELETE),
    INHERITABLE(0x10),
    NONE(0),
    READ(WinNT.FILE_READ_DATA),
    READ_WRITE(3),
    WRITE(WinNT.FILE_WRITE_DATA)
}