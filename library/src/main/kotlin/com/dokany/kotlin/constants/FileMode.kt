package com.dokany.kotlin.constants

import com.dokany.kotlin.structure.EnumInt
import com.sun.jna.platform.win32.WinNT

enum class FileMode(override val mask: Int) : EnumInt {
    APPEND(6),
    CREATE(WinNT.CREATE_ALWAYS),
    CREATE_NEW(WinNT.CREATE_NEW),
    OPEN(WinNT.OPEN_EXISTING),
    OPEN_OR_CREATE(WinNT.OPEN_ALWAYS),
    TRUNCATE(WinNT.TRUNCATE_EXISTING)
}