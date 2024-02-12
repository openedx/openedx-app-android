package org.openedx.auth.domain.model

import org.openedx.auth.data.model.AuthType

data class SocialAuthResponse(
    var accessToken: String = "",
    var name: String = "",
    var email: String = "",
    var authType: AuthType = AuthType.PASSWORD,
)
