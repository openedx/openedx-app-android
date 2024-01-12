package org.openedx.core.extension

import android.content.res.AssetManager
import java.io.BufferedReader

fun AssetManager.readAsText(fileName: String): String {
    return open(fileName).bufferedReader().use(BufferedReader::readText)
}
