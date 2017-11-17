package com.dokany.kotlin.constants

import com.dokany.kotlin.structure.EnumInt
import com.sun.jna.platform.win32.WinNT

/**
 *
 * @see {@linkplain https://msdn.microsoft.com/en-us/library/cc246323.aspx}
 *
 */
enum class FileSystemFeature(
        override val mask: Int
) : EnumInt {
    NONE(0),

    CASE_SENSITIVE_SEARCH(WinNT.FILE_CASE_SENSITIVE_SEARCH),

    CASE_PRESERVED_NAMES(WinNT.FILE_CASE_PRESERVED_NAMES),

    UNICODE_ON_DISK(WinNT.FILE_UNICODE_ON_DISK),

    PERSISTENT_ACLS(WinNT.FILE_PERSISTENT_ACLS),

    VOLUME_QUOTAS(WinNT.FILE_VOLUME_QUOTAS),

    SUPPORTS_SPARSE_FILES(WinNT.FILE_SUPPORTS_SPARSE_FILES),

    SUPPORTS_REPARSE_POINTS(WinNT.FILE_SUPPORTS_REPARSE_POINTS),

    SUPPORTS_REMOTE_STORAGE(WinNT.FILE_SUPPORTS_REMOTE_STORAGE),

    VOLUME_IS_COMPRESSED(WinNT.FILE_VOLUME_IS_COMPRESSED),

    SUPPORTS_OBJECT_IDS(WinNT.FILE_SUPPORTS_OBJECT_IDS),

    SUPPORTS_ENCRYPTION(WinNT.FILE_SUPPORTS_ENCRYPTION),

    NAMED_STREAMS(WinNT.FILE_NAMED_STREAMS),

    READ_ONLY_VOLUME(WinNT.FILE_READ_ONLY_VOLUME),

    SEQUENTIAL_WRITE_ONCE(WinNT.FILE_SEQUENTIAL_WRITE_ONCE),

    SUPPORTS_TRANSACTIONS(WinNT.FILE_SUPPORTS_TRANSACTIONS)
}