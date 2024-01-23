package org.openedx.auth.presentation.sso

import androidx.fragment.app.Fragment
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.domain.model.SocialAuthResponse

class OAuthHelper(
    private val facebookAuthHelper: FacebookAuthHelper,
    private val googleAuthHelper: GoogleAuthHelper,
    private val microsoftAuthHelper: MicrosoftAuthHelper,
) {
    internal suspend fun socialAuth(fragment: Fragment, authType: AuthType): SocialAuthResponse? {
        return when (authType) {
            AuthType.PASSWORD -> null
            AuthType.GOOGLE -> googleAuthHelper.socialAuth(fragment.requireActivity())
            AuthType.FACEBOOK -> facebookAuthHelper.socialAuth(fragment)
            AuthType.MICROSOFT -> microsoftAuthHelper.socialAuth(fragment.requireActivity())
        }
    }

    fun clear() {
        facebookAuthHelper.clear()
    }
}
