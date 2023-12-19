package org.openedx.app.analytics

import android.os.Bundle

interface Analytics {
    fun logScreenEvent(screenName: String, bundle: Bundle)
    fun logEvent(eventName: String, bundle: Bundle)
    fun logUserId(userId: Long)
}
