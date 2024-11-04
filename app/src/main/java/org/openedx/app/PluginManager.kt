package org.openedx.app

import org.openedx.foundation.interfaces.Analytics

class PluginManager(
    private val analyticsManager: AnalyticsManager
) {

    fun addPlugin(analytics: Analytics) {
        analyticsManager.addAnalyticsTracker(analytics)
    }
}
