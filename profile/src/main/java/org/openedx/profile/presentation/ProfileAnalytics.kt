package org.openedx.profile.presentation

interface ProfileAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class ProfileAnalyticsEvent(val eventName: String, val biValue: String) {
    EDIT_PROFILE(
        "Profile:Edit Profile",
        "edx.bi.app.profile.edit"
    ),
    EDIT_CLICKED(
        "Profile:Edit Clicked",
        "edx.bi.app.profile.edit.clicked"
    ),
    SWITCH_PROFILE(
        "Profile:Switch Profile",
        "edx.bi.app.profile.switch_profile.clicked"
    ),
    EDIT_DONE_CLICKED(
        "Profile:Edit Done Clicked",
        "edx.bi.app.profile.edit_done.clicked"
    ),
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
    DELETE_ACCOUNT_CLICKED(
        "Profile:Delete Account Clicked",
        "edx.bi.app.profile.delete_account.clicked"
    ),
    USER_DELETE_ACCOUNT_CLICKED(
        "Profile:User Delete Account Clicked",
        "edx.bi.app.profile.user.delete_account.clicked"
    ),
    DELETE_ACCOUNT_SUCCESS(
        "Profile:Delete Account Success",
        "edx.bi.app.profile.delete_account.success"
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

enum class ProfileAnalyticsKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    PROFILE("profile"),
    ACTION("action"),
    FULL_PROFILE("full_profile"),
    LIMITED_PROFILE("limited_profile"),
    SUCCESS("success"),
    FORCE("force"),
    FALSE("false"),
}
