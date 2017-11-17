package com.dokany.kotlin.structure

import java.util.*

interface EnumInt {
    val mask: Int
}

class EnumIntSet<T>(
        private val elements: EnumSet<T>
) : Set<T> by elements
        where T : EnumInt, T : Enum<T> {
    fun toInt() = elements.fold(0) { currentReturn, value ->
        currentReturn or value.mask
    }

    fun containsAll(vararg vals: T): Boolean = containsAll(vals.toList())
}

inline fun <reified T> enumSetOf(vararg values: T) where T : EnumInt, T : Enum<T> = EnumIntSet(EnumSet.noneOf(T::class.java).apply {
    addAll(values)
})

inline fun <reified T> enumSetFromInt(value: Int) where T : EnumInt, T : Enum<T> = enumValues<T>().let { values ->
    val containing = ArrayList<T>(values.size)

    values.fold(value) { remainingValues, enumValue ->
        when {
            enumValue.mask == 0 -> {
                if(value == 0) {
                    containing += enumValue
                }
                remainingValues
            }
            remainingValues and enumValue.mask == enumValue.mask -> {
                containing += enumValue
                remainingValues - enumValue.mask
            }
            else -> remainingValues
        }
    }

    enumSetOf(*containing.toTypedArray())
}

inline fun <reified T> enumFromInt(value: Int): T
        where T : EnumInt, T : Enum<T> =
        enumValues<T>().let { values ->
            values.find {
                when {
                    it.mask == 0 -> value == 0
                    else -> value and it.mask == it.mask
                }
            } ?: throw IllegalArgumentException("Invalid int value: $value")
        }