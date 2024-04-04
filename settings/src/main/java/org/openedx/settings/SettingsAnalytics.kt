package org.openedx.settings

interface SettingsAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class SettingsAnalyticsEvent(val eventName: String, val biValue: String) {
    VIDEO_SETTING_CLICKED(
        "Profile:Video Setting Clicked",
        "edx.bi.app.profile.video_setting.clicked"
    ),
    CONTACT_SUPPORT_CLICKED(
        "Profile:Contact Support Clicked",
        "edx.bi.app.profile.email_support.clicked"
    ),
    FAQ_CLICKED(
        "Profile:FAQ Clicked",
        "edx.bi.app.profile.faq.clicked"
    ),
    TERMS_OF_USE_CLICKED(
        "Profile:Terms of Use Clicked",
        "edx.bi.app.profile.terms_of_use.clicked"
    ),
    PRIVACY_POLICY_CLICKED(
        "Profile:Privacy Policy Clicked",
        "edx.bi.app.profile.privacy_policy.clicked"
    ),
    COOKIE_POLICY_CLICKED(
        "Profile:Cookie Policy Clicked",
        "edx.bi.app.profile.cookie_policy.clicked"
    ),
    DATA_SELL_CLICKED(
        "Profile:Data Sell Clicked",
        "edx.bi.app.profile.do_not_sell_data.clicked"
    ),
    WIFI_TOGGLE(
        "Profile:Wifi Toggle",
        "edx.bi.app.profile.wifi_toggle.action"
    ),
    LOGOUT_CLICKED(
        "Profile:Logout Clicked",
        "edx.bi.app.profile.logout.clicked"
    ),
    LOGGED_OUT(
        "Profile:Logged Out",
        "edx.bi.app.user.logout"
    ),
}

enum class SettingsAnalyticsKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    PROFILE("profile"),
    ACTION("action"),
    FORCE("force"),
    TRUE("true"),
    FALSE("false"),
}
