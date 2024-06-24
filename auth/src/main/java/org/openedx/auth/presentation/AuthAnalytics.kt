package org.openedx.auth.presentation

interface AuthAnalytics {
    fun setUserIdForSession(userId: Long)
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class AuthAnalyticsEvent(val eventName: String, val biValue: String) {
    Logistration(
        "Logistration",
        "edx.bi.app.logistration"
    ),
    DISCOVERY_COURSES_SEARCH(
        "Logistration:Courses Search",
        "edx.bi.app.logistration.courses_search"
    ),
    EXPLORE_ALL_COURSES(
        "Logistration:Explore All Courses",
        "edx.bi.app.logistration.explore.all.courses"
    ),
    SIGN_IN(
        "Logistration:Sign In",
        "edx.bi.app.logistration.signin"
    ),
    REGISTER(
        "Logistration:Register",
        "edx.bi.app.logistration.register"
    ),
    REGISTER_CLICKED(
        "Logistration:Register Clicked",
        "edx.bi.app.logistration.register.clicked"
    ),
    CREATE_ACCOUNT_CLICKED(
        "Logistration:Create Account Clicked",
        "edx.bi.app.logistration.user.create_account.clicked"
    ),
    REGISTER_SUCCESS(
        "Logistration:Register Success",
        "edx.bi.app.user.register.success"
    ),
    SIGN_IN_CLICKED(
        "Logistration:Sign In Clicked",
        "edx.bi.app.logistration.signin.clicked"
    ),
    USER_SIGN_IN_CLICKED(
        "Logistration:User Sign In Clicked",
        "edx.bi.app.logistration.user.signin.clicked"
    ),
    SIGN_IN_SUCCESS(
        "Logistration:Sign In Success",
        "edx.bi.app.user.signin.success"
    ),
    FORGOT_PASSWORD_CLICKED(
        "Logistration:Forgot Password Clicked",
        "edx.bi.app.logistration.forgot_password.clicked"
    ),
    RESET_PASSWORD_CLICKED(
        "Logistration:Reset Password Clicked",
        "edx.bi.app.user.reset_password.clicked"
    ),
    RESET_PASSWORD_SUCCESS(
        "Logistration:Reset Password Success",
        "edx.bi.app.user.reset_password.success"
    ),
}

enum class AuthAnalyticsKey(val key: String) {
    NAME("name"),
    SEARCH_QUERY("search_query"),
    SUCCESS("success"),
    METHOD("method"),
}
