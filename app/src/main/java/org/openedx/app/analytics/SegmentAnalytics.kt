package org.openedx.app.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import org.openedx.app.BuildConfig
import org.openedx.core.config.SegmentConfig
import com.segment.analytics.kotlin.android.Analytics as SegmentAnalyticsBuilder
import com.segment.analytics.kotlin.core.Analytics as SegmentTracker


class SegmentAnalytics(context: Context, config: SegmentConfig) : Analytics {

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
    }

    override fun logScreenEvent(screenName: String, bundle: Bundle) {
        Log.d("Analytics", "Segment log Screen Event: $screenName + $bundle")
    }

    override fun logEvent(eventName: String, bundle: Bundle) {
        Log.d("Analytics", "Segment log Event $eventName: $bundle")
    }

    override fun logUserId(userId: Long) {
        Log.d("Analytics", "Segment User Id log Event")
    }
}
