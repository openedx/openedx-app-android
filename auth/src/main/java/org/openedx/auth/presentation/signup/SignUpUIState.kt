package org.openedx.auth.presentation.signup

import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.system.notifier.AppUpgradeEvent

data class SignUpUIState(
    val fields: List<RegistrationField> = emptyList(),
    val optionalFields: List<RegistrationField> = emptyList(),
    val isFacebookAuthEnabled: Boolean = false,
    val isGoogleAuthEnabled: Boolean = false,
    val isMicrosoftAuthEnabled: Boolean = false,
    val isSocialAuthEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isButtonLoading: Boolean = false,
    val validationError: Boolean = false,
    val successLogin: Boolean = false,
    val appUpgradeEvent: AppUpgradeEvent? = null,
)
