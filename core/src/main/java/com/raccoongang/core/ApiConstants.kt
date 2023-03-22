package com.raccoongang.core

object ApiConstants {
    const val URL_LOGIN = "/oauth2/login/"
    const val URL_ACCESS_TOKEN = "/oauth2/access_token/"
    const val GET_USER_PROFILE = "/api/mobile/v0.5/my_user_info"
    const val URL_REVOKE_TOKEN = "/oauth2/revoke_token/"
    const val URL_REGISTRATION_FIELDS = "/user_api/v1/account/registration"
    const val URL_VALIDATE_REGISTRATION_FIELDS = "/api/user/v1/validation/registration"
    const val URL_REGISTER = "/api/user/v1/account/registration/"
    const val URL_PASSWORD_RESET = "/password_reset/"

    const val GRANT_TYPE_PASSWORD = "password"

    const val TOKEN_TYPE_REFRESH = "refresh_token"

    const val TOKEN_TYPE_BEARER = "Bearer"

    const val EMAIL = "email"
    const val PASSWORD = "password"
}