package org.openedx.core.extension

import kotlin.math.log10
import kotlin.math.pow

fun Long.toFileSize(round: Int = 2, space: Boolean = true): String {
    try {
        if (this <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
        val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
        val size = this / 1024.0.pow(digitGroups.toDouble())
        val formatString = if (size % 1 == 0.0) "%.0f" else "%.${round}f"
        return String.format(formatString, size) + if (space) " " else "" + units[digitGroups]
    } catch (e: Exception) {
        println(e.toString())
    }
    return ""
}
