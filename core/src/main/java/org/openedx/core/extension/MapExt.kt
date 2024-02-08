package org.openedx.core.extension

import android.os.Bundle

fun Map<String, Any?>.toBundle(): Bundle {
    val bundle = Bundle()
    for ((key, value) in this.entries) {
        value?.let {
            bundle.putString(key, it.toString())
        }
    }
    return bundle
}
