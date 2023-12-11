package org.openedx.core.utils

import android.util.Log
import org.openedx.core.BuildConfig

class Logger(private val tag: String) {
    fun d(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(tag, message())
    }

    fun e(message: () -> String) {
        if (BuildConfig.DEBUG) Log.e(tag, message())
    }

    fun w(message: () -> String) {
        if (BuildConfig.DEBUG) Log.w(tag, message())
    }
}
