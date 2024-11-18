package org.openedx.core.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.BuildConfig
import org.openedx.core.config.Config

class Logger(private val tag: String) : KoinComponent {

    private val config by inject<Config>()

    fun d(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(tag, message())
    }

    fun e(message: () -> String) {
        if (BuildConfig.DEBUG) Log.e(tag, message())
    }

    fun e(throwable: Throwable, submitCrashReport: Boolean = false) {
        if (BuildConfig.DEBUG) throwable.printStackTrace()
        if (submitCrashReport && config.getFirebaseConfig().enabled) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }

    fun i(message: () -> String) {
        if (BuildConfig.DEBUG) Log.i(tag, message())
    }

    fun w(message: () -> String) {
        if (BuildConfig.DEBUG) Log.w(tag, message())
    }
}
