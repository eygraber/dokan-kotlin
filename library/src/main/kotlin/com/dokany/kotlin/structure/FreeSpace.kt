package com.dokany.kotlin.structure

data class FreeSpace(
        private val totalBytes: Long,
        private val totalUsed: Long
) {
    val freeBytes get() = totalBytes - totalUsed
}