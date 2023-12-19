package org.openedx.auth.data.model

import org.openedx.core.ApiConstants

/**
 * Enum class with types of supported auth types
 *
 * @param postfix postfix to add to the API call
 * @param methodName name of the login type
 */
enum class AuthType(val postfix: String, val methodName: String) {
    PASSWORD("", "Password"),
    GOOGLE(ApiConstants.AUTH_TYPE_GOOGLE, "Google"),
    FACEBOOK(ApiConstants.AUTH_TYPE_FB, "Facebook"),
    MICROSOFT(ApiConstants.AUTH_TYPE_MICROSOFT, "Microsoft"),
}
