package com.dokany.kotlin.constants

import com.dokany.kotlin.structure.EnumInteger

enum class ErrorCode(
        override val mask: Int
) : EnumInteger {
    SUCCESS(0),

    ERROR_WRITE_FAULT(29),

    ERROR_READ_FAULT(30),

    ERROR_FILE_NOT_FOUND(-0x3fffffcc),

    OBJECT_NAME_COLLISION(-0x3fffffcb),

    ERROR_FILE_EXISTS(80),

    ERROR_ALREADY_EXISTS(183)
}