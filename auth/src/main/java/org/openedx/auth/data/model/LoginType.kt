package org.openedx.auth.data.model

import org.openedx.core.ApiConstants

/**
 * Enum class with types of supported login types
 *
 * @param postfix postfix to add to the API call
 * @param methodName name of the login type
 */
enum class LoginType(val postfix: String, val methodName: String) {
    PASSWORD("", "Password"),
    GOOGLE(ApiConstants.LOGIN_TYPE_GOOGLE, "Google"),
    FACEBOOK(ApiConstants.LOGIN_TYPE_FB, "Facebook"),
    MICROSOFT(ApiConstants.LOGIN_TYPE_MICROSOFT, "Microsoft"),
}
