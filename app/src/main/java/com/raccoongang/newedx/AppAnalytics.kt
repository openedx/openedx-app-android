package com.raccoongang.newedx

interface AppAnalytics {
    fun logoutEvent(force: Boolean)
    fun discoveryTabClickedEvent()
    fun dashboardTabClickedEvent()
    fun programsTabClickedEvent()
    fun profileTabClickedEvent()
}