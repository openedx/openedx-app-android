package org.openedx.core.extension

import java.net.URL

fun String?.equalsHost(host: String?): Boolean {
    return try {
        host?.startsWith(URL(this).host, ignoreCase = true) == true
    } catch (_: Exception) {
        false
    }
}
