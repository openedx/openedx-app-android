package org.openedx.core.extension

import kotlin.math.log10
import kotlin.math.pow

fun Long.toFileSize(round: Int = 2, space: Boolean = true): String {
    try {
        if (this <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
        val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
        return String.format(
            "%." + round + "f", this / 1024.0.pow(digitGroups.toDouble())
        ) + if (space) " " else "" + units[digitGroups]
    } catch (e: Exception) {
        println(e.toString())
    }
    return ""
}
