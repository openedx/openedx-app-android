package org.openedx.auth.presentation

interface AuthAnalytics {
    fun setUserIdForSession(userId: Long)
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class LogistrationAnalyticEvent(val event: String) {
    DISCOVERY_COURSES_SEARCH("Logistration:Discovery Courses Search"),
    EXPLORE_ALL_COURSES("Logistration:Explore All Courses"),
    REGISTER_CLICKED("Logistration:Register Clicked"),
    CREATE_ACCOUNT_CLICKED("Logistration:Create Account Clicked"),
    REGISTER_SUCCESSFULLY("Logistration:Register Successfully"),
    SIGN_IN_CLICKED("Logistration:Sign In Clicked"),
    FORGOT_PASSWORD_CLICKED("Logistration:Forgot Password Clicked"),
    RESET_PASSWORD_CLICKED("Logistration:Reset Password Clicked"),
    RESET_PASSWORD("Logistration:Reset Password"),
    SIGN_IN_SUCCESSFULLY("Logistration:Sign In Successfully"),
}

enum class LogistrationAnalyticValues(val biValue: String) {
    DISCOVERY_COURSES_SEARCH("edx.bi.app.discovery.courses_search"),
    EXPLORE_ALL_COURSES("edx.bi.app.discovery.explore.all.courses"),
    REGISTER_CLICKED("edx.bi.app.user.register.clicked"),
    SCREEN_NAVIGATION("edx.bi.app.navigation.screen"),
    CREATE_ACCOUNT_CLICKED("edx.bi.app.user.create_account.clicked"),
    REGISTER_SUCCESSFULLY("edx.bi.app.user.register.success"),
    SIGN_IN_CLICKED("edx.bi.app.user.signin.clicked"),
    RESET_PASSWORD_CLICKED("edx.bi.app.user.reset_password.clicked"),
    RESET_PASSWORD("edx.bi.app.user.reset_password"),
    SIGN_IN_SUCCESSFULLY("edx.bi.app.user.signin"),
}

enum class LogistrationAnalyticKey(val key: String) {
    NAME("name"),
    LABEL("label"),
    SUCCESS("success"),
    METHOD("method"),
    PROVIDER("provider"),
}
