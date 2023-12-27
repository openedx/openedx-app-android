package org.openedx.core.extension

import android.net.Uri

fun Uri.getQueryParams(): Map<String, String> {
    val paramsMap = mutableMapOf<String, String>()

    queryParameterNames.forEach { name ->
        getQueryParameter(name)?.let { value ->
            paramsMap[name] = value
        }
    }

    return paramsMap
}
