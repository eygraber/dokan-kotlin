package com.dokany.kotlin.constants

import com.dokany.kotlin.structure.EnumInteger
import com.sun.jna.platform.win32.WinNT

enum class CreationDisposition(
        override val mask: Int,
        val description: String
) : EnumInteger {
    /*-
     *                          |                    When the file...
    This argument:           |             Exists            Does not exist
    -------------------------+------------------------------------------------------
    CREATE_ALWAYS            |            Truncates             Creates
    CREATE_NEW         +-----------+        Fails               Creates
    OPEN_ALWAYS     ===| does this |===>    Opens               Creates
    OPEN_EXISTING      +-----------+        Opens                Fails
    TRUNCATE_EXISTING        |            Truncates              Fails
     */

    CREATE_NEW(WinNT.CREATE_NEW, "Create New"),
    CREATE_ALWAYS(WinNT.CREATE_ALWAYS, "Create Always"),
    OPEN_EXISTING(WinNT.OPEN_EXISTING, "Open Existing"),
    OPEN_ALWAYS(WinNT.OPEN_ALWAYS, "Open Always"),
    TRUNCATE_EXISTING(WinNT.TRUNCATE_EXISTING, "Truncate Existing");

    val isReadOnly: Boolean get() = this == OPEN_EXISTING || this == OPEN_ALWAYS
}