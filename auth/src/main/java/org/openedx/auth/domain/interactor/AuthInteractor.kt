package org.openedx.auth.domain.interactor

import org.openedx.auth.data.model.AuthType
import org.openedx.auth.data.model.ValidationFields
import org.openedx.auth.data.repository.AuthRepository
import org.openedx.core.domain.model.RegistrationField

class AuthInteractor(private val repository: AuthRepository) {

    suspend fun login(
        username: String,
        password: String
    ) {
        repository.login(username, password)
    }

    suspend fun loginSocial(token: String?, authType: AuthType) {
        repository.socialLogin(token, authType)
    }

    suspend fun loginAuthCode(authCode: String) {
        repository.browserAuthCodeLogin(authCode)
    }

    suspend fun getRegistrationFields(): List<RegistrationField> {
        return repository.getRegistrationFields()
    }

    suspend fun register(mapFields: Map<String, String>) {
        return repository.register(mapFields)
    }

    suspend fun validateRegistrationFields(mapFields: Map<String, String>): ValidationFields {
        return repository.validateRegistrationFields(mapFields)
    }

    suspend fun passwordReset(email: String): Boolean {
        return repository.passwordReset(email)
    }
}
