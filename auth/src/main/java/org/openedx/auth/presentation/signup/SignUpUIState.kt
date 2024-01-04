package org.openedx.auth.presentation.signup

import org.openedx.core.domain.model.RegistrationField

data class SignUpUIState(
    val fields: List<RegistrationField> = emptyList(),
    val optionalFields: List<RegistrationField> = emptyList(),
    val isFacebookAuthEnabled: Boolean = false,
    val isGoogleAuthEnabled: Boolean = false,
    val isMicrosoftAuthEnabled: Boolean = false,
    val isSocialAuthEnabled: Boolean = false,
    val isLoading: Boolean = false,
)
