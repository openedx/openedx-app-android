package org.openedx.auth.presentation.signin

/**
 * Data class to store UI state of the SignIn screen
 *
 * @param shouldShowSocialLogin is SSO buttons visible
 * @param showProgress is progress visible
 * @param loginSuccess is login succeed
 */
internal data class SignInUIState(
    val shouldShowSocialLogin: Boolean = false,
    val showProgress: Boolean = false,
    val loginSuccess: Boolean = false,
)
