package org.openedx.app.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import org.openedx.core.utils.Logger
import org.openedx.foundation.extension.toBundle

class FirebaseAnalytics(context: Context) : Analytics {

    private val logger = Logger(TAG)
    private var tracker: FirebaseAnalytics

    init {
        tracker = FirebaseAnalytics.getInstance(context)
        logger.d { "Firebase Analytics Builder Initialised" }
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        tracker.logEvent(screenName, params.toBundle())
        logger.d { "Firebase Analytics log Screen Event: $screenName + $params" }
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        tracker.logEvent(eventName, params.toBundle())
        logger.d { "Firebase Analytics log Event $eventName: $params" }
    }

    override fun logUserId(userId: Long) {
        tracker.setUserId(userId.toString())
        logger.d { "Firebase Analytics User Id log Event" }
    }

    private companion object {
        const val TAG = "FirebaseAnalytics"
    }
}
