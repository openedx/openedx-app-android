package org.openedx.app.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalytics(context: Context) : Analytics {

    private var tracker: FirebaseAnalytics? = null

    init {
        tracker = FirebaseAnalytics.getInstance(context)
        Log.d("Analytics", "Firebase Builder Initialised")
    }

    override fun logScreenEvent(screenName: String, bundle: Bundle) {
        Log.d("Analytics", "Firebase log Screen Event: $screenName + $bundle")
    }

    override fun logEvent(eventName: String, bundle: Bundle) {
        tracker?.logEvent(eventName, bundle)
        Log.d("Analytics", "Firebase log Event $eventName: $bundle")
    }

    override fun logUserId(userId: Long) {
        tracker?.setUserId(userId.toString())
        Log.d("Analytics", "Firebase User Id log Event")
    }
}
