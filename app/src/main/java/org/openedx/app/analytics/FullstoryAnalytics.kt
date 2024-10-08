package org.openedx.app.analytics

import com.fullstory.FS
import com.fullstory.FSSessionData
import org.openedx.core.utils.Logger
import org.openedx.foundation.interfaces.Analytics

class FullstoryAnalytics : Analytics {

    private val logger = Logger(TAG)

    init {
        FS.setReadyListener { sessionData: FSSessionData ->
            val sessionUrl = sessionData.currentSessionURL
            logger.d { "FullStory Session URL is: $sessionUrl" }
        }
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        logger.d { "Page : $screenName $params" }
        FS.page(screenName, params).start()
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        logger.d { "Event: $eventName $params" }
        FS.page(eventName, params).start()
    }

    override fun logUserId(userId: Long) {
        logger.d { "Identify: $userId" }
        FS.identify(
            userId.toString(), mapOf(
                DISPLAY_NAME to userId
            )
        )
    }

    private companion object {
        const val TAG = "FullstoryAnalytics"
        private const val DISPLAY_NAME = "displayName"
    }
}
