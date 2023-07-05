package org.openedx.app

interface AppAnalytics {
    fun logoutEvent(force: Boolean)
    fun discoveryTabClickedEvent()
    fun dashboardTabClickedEvent()
    fun programsTabClickedEvent()
    fun profileTabClickedEvent()
    fun setUserIdForSession(userId: Long)
}