package com.dokany.kotlin.structure

import java.util.*

data class FileData(
        val bytes: ByteArray,
        val length: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileData

        if (!Arrays.equals(bytes, other.bytes)) return false
        if (length != other.length) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(bytes)
        result = 31 * result + length
        return result
    }
}