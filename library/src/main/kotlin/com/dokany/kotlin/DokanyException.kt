package com.dokany.kotlin

import com.dokany.kotlin.constants.ErrorCode
import com.dokany.kotlin.constants.WinError
import java.io.IOException

class DokanyException(
        val value: Long,
        exception: IOException
) : RuntimeException() {
    private var serialVersionUID = -862591089502909563L

    init {
        if (value < 0 || value > 4294967295L) {
            throw IllegalArgumentException("error code ($value) is not in range [0, 4294967295]", exception)
        }
    }

    constructor(errorCode: WinError, exception: IOException) : this(errorCode.mask.toLong(), exception)

    constructor(errorCode: ErrorCode, exception: IOException) : this(errorCode.mask.toLong(), exception)
}