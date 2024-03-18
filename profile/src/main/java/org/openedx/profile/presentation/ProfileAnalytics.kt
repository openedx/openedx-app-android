package org.openedx.profile.presentation

interface ProfileAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class ProfileAnalyticEvent(val event: String) {
    EDIT_CLICKED("Profile:Edit Clicked"),
    SWITCH_PROFILE("Profile:Switch Profile"),
    EDIT_DONE_CLICKED("Profile:Edit Done Clicked"),
    VIDEO_SETTING_CLICKED("Profile:Video Setting Clicked"),
    CONTACT_SUPPORT_CLICKED("Profile:Contact Support Clicked"),
    FAQ_CLICKED("Profile:FAQ Clicked"),
    TERMS_OF_USE_CLICKED("Profile:Terms of Use Clicked"),
    PRIVACY_POLICY_CLICKED("Profile:Privacy Policy Clicked"),
    COOKIE_POLICY_CLICKED("Profile:Cookie Policy Clicked"),
    DATA_SELL_CLICKED("Profile:Data Sell Clicked"),
    DELETE_ACCOUNT_CLICKED("Profile:Delete Account Clicked"),
    USER_DELETE_ACCOUNT_CLICKED("Profile:User Delete Account Success"),
    DELETE_ACCOUNT_SUCCESS("Profile:Delete Account Success"),
    WIFI_TOGGLE("Profile:Wifi Toggle"),
    LOGOUT_CLICKED("Profile:Logout Clicked"),
    LOGGED_OUT("Profile:Logged Out"),
}

enum class ProfileAnalyticValue(val biValue: String) {
    EDIT_CLICKED("edx.bi.app.profile.edit.clicked"),
    SWITCH_PROFILE("edx.bi.app.profile.switch_profile.clicked"),
    EDIT_DONE_CLICKED("edx.bi.app.profile.edit_done.clicked"),
    VIDEO_SETTING_CLICKED("edx.bi.app.profile.video_setting.clicked"),
    CONTACT_SUPPORT_CLICKED("edx.bi.app.profile.email_support.clicked"),
    FAQ_CLICKED("edx.bi.app.profile.faq.clicked"),
    TERMS_OF_USE_CLICKED("edx.bi.app.profile.terms_of_use.clicked"),
    PRIVACY_POLICY_CLICKED("edx.bi.app.profile.privacy_policy.clicked"),
    COOKIE_POLICY_CLICKED("edx.bi.app.profile.cookie_policy.clicked"),
    DATA_SELL_CLICKED("edx.bi.app.profile.do_not_sell_data.clicked"),
    DELETE_ACCOUNT_CLICKED("edx.bi.app.profile.delete_account.clicked"),
    USER_DELETE_ACCOUNT_CLICKED("edx.bi.app.profile.user.delete_account.clicked"),
    DELETE_ACCOUNT_SUCCESS("edx.bi.app.profile.delete_account.success"),
    WIFI_TOGGLE("edx.bi.app.profile.wifi_toggle.action"),
    LOGOUT_CLICKED("edx.bi.app.profile.logout.clicked"),
    LOGGED_OUT("edx.bi.app.user.logout"),
}

enum class ProfileAnalyticKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    PROFILE("profile"),
    ACTION("action"),
    FULL_PROFILE("full_profile"),
    LIMITED_PROFILE("limited_profile"),
    ON("on"),
    OFF("off"),
    SUCCESS("success"),
}
