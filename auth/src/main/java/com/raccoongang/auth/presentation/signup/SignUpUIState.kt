package com.raccoongang.auth.presentation.signup

import com.raccoongang.core.domain.model.RegistrationField

sealed class SignUpUIState {
    data class Fields(
        val fields: List<RegistrationField>,
        val optionalFields: List<RegistrationField>
    ) : SignUpUIState()
    object Loading : SignUpUIState()
}