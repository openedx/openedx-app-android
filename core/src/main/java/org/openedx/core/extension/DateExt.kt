package org.openedx.core.extension

import java.util.Date

fun Date?.isNotNull(): Boolean {
    return this != null
}
