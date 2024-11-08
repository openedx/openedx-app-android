package org.openedx.core.extension

fun <T> T?.isNotNull(): Boolean {
    return this != null
}

fun <T> T?.isNull(): Boolean {
    return this == null
}
