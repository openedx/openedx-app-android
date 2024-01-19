package org.openedx.core.extension

fun Int.nonZero(): Int? {
    return if (this != 0) this else null
}
