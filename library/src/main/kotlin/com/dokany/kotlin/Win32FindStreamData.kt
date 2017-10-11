package com.dokany.kotlin

import com.sun.jna.Structure

class Win32FindStreamData(
        @JvmField internal var length: Long,
        @JvmField internal var cFileName: CharArray = kotlin.CharArray(DokanyOperationsProxy.MAX_PATH)
) : Structure(), DokanyOperations.Win32FindStreamDataInterface {
    override fun getFieldOrder() = listOf(
            "length",
            "cFileName"
    )

    override fun length(value: Long) {
        length = value
    }

    override fun cFileName() = cFileName
}