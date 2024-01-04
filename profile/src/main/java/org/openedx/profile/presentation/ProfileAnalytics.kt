package org.openedx.profile.presentation

interface ProfileAnalytics {
    fun profileEditClickedEvent()
    fun profileEditDoneClickedEvent()
    fun profileDeleteAccountClickedEvent()
    fun profileVideoSettingsClickedEvent()
    fun privacyPolicyClickedEvent()
    fun termsOfUseClickedEvent()
    fun cookiePolicyClickedEvent()
    fun dataSellClickedEvent()
    fun faqClickedEvent()
    fun emailSupportClickedEvent()
    fun logoutEvent(force: Boolean)
}
