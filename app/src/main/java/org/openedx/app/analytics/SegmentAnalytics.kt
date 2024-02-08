package org.openedx.app.analytics

import android.content.Context
import org.openedx.app.BuildConfig
import org.openedx.core.config.SegmentConfig
import org.openedx.core.utils.Logger
import com.segment.analytics.kotlin.android.Analytics as SegmentAnalyticsBuilder
import com.segment.analytics.kotlin.core.Analytics as SegmentTracker

class SegmentAnalytics(context: Context, config: SegmentConfig) : Analytics {

    private val logger = Logger(this.javaClass.name)
    private var tracker: SegmentTracker

    init {
        // Create an analytics client with the given application context and Segment write key.
        tracker = SegmentAnalyticsBuilder(config.segmentWriteKey, context) {
            // Automatically track Lifecycle events
            trackApplicationLifecycleEvents = true
            flushAt = 20
            flushInterval = 30
        }
        SegmentTracker.debugLogsEnabled = BuildConfig.DEBUG
        logger.d { "Segment Analytics Builder Initialised" }
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        logger.d { "Segment Analytics log Screen Event: $screenName + $params" }
        tracker.screen(screenName, params)
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        logger.d { "Segment Analytics log Event $eventName: $params" }
        tracker.track(eventName, params)
    }

    override fun logUserId(userId: Long) {
        logger.d { "Segment Analytics User Id log Event: $userId" }
        tracker.identify(userId.toString())
    }
}
