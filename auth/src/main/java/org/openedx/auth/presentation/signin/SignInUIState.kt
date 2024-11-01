package org.openedx.auth.presentation.signin

import org.openedx.core.domain.model.RegistrationField

/**
 * Data class to store UI state of the SignIn screen
 *
 * @param isFacebookAuthEnabled is Facebook auth enabled
 * @param isGoogleAuthEnabled is Google auth enabled
 * @param isMicrosoftAuthEnabled is Microsoft auth enabled
 * @param isSocialAuthEnabled is OAuth buttons visible
 * @param showProgress is progress visible
 * @param loginSuccess is login succeed
 */
internal data class SignInUIState(
    val isFacebookAuthEnabled: Boolean = false,
    val isGoogleAuthEnabled: Boolean = false,
    val isMicrosoftAuthEnabled: Boolean = false,
    val isSocialAuthEnabled: Boolean = false,
    val isLogistrationEnabled: Boolean = false,
    val isRegistrationEnabled: Boolean = true,
    val showProgress: Boolean = false,
    val loginSuccess: Boolean = false,
    val agreement: RegistrationField? = null,
)
