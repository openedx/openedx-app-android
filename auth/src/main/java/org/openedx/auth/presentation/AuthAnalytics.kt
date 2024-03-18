package org.openedx.auth.presentation

interface AuthAnalytics {
    fun setUserIdForSession(userId: Long)
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class LogistrationAnalyticEvent(val event: String) {
    DISCOVERY_COURSES_SEARCH("Logistration:Courses Search"),
    EXPLORE_ALL_COURSES("Logistration:Explore All Courses"),
    REGISTER_CLICKED("Logistration:Register Clicked"),
    CREATE_ACCOUNT_CLICKED("Logistration:Create Account Clicked"),
    REGISTER_SUCCESS("Logistration:Register Success"),
    SIGN_IN_CLICKED("Logistration:Sign In Clicked"),
    USER_SIGN_IN_CLICKED("Logistration:User Sign In Clicked"),
    SIGN_IN_SUCCESS("Logistration:Sign In Success"),
    FORGOT_PASSWORD_CLICKED("Logistration:Forgot Password Clicked"),
    RESET_PASSWORD_CLICKED("Logistration:Reset Password Clicked"),
    RESET_PASSWORD_SUCCESS("Logistration:Reset Password Success"),
}

enum class LogistrationAnalyticValues(val biValue: String) {
    DISCOVERY_COURSES_SEARCH("edx.bi.app.logistration.courses_search"),
    EXPLORE_ALL_COURSES("edx.bi.app.logistration.explore.all.courses"),
    REGISTER_CLICKED("edx.bi.app.logistration.register.clicked"),
    CREATE_ACCOUNT_CLICKED("edx.bi.app.logistration.user.create_account.clicked"),
    REGISTER_SUCCESS("edx.bi.app.user.register.success"),
    SIGN_IN_CLICKED("edx.bi.app.logistration.signin.clicked"),
    USER_SIGN_IN_CLICKED("edx.bi.app.logistration.user.signin.clicked"),
    SIGN_IN_SUCCESS("edx.bi.app.user.signin.success"),
    FORGOT_PASSWORD_CLICKED("edx.bi.app.logistration.forgot_password.clicked"),
    RESET_PASSWORD_CLICKED("edx.bi.app.user.reset_password.clicked"),
    RESET_PASSWORD_SUCCESS("edx.bi.app.user.reset_password.success"),
}

enum class LogistrationAnalyticKey(val key: String) {
    NAME("name"),
    SEARCH_QUERY("search_query"),
    SUCCESS("success"),
    METHOD("method"),
}
