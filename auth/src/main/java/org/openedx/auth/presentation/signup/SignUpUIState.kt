package org.openedx.auth.presentation.signup

import org.openedx.core.domain.model.RegistrationField

sealed class SignUpUIState {
    data class Fields(
        val fields: List<RegistrationField>,
        val optionalFields: List<RegistrationField>
    ) : SignUpUIState()
    object Loading : SignUpUIState()
}