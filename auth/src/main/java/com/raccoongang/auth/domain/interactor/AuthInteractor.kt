package com.raccoongang.auth.domain.interactor

import com.raccoongang.auth.data.model.ValidationFields
import com.raccoongang.auth.data.repository.AuthRepository
import com.raccoongang.core.domain.model.RegistrationField

class AuthInteractor(private val repository: AuthRepository) {

    suspend fun login(
        username: String,
        password: String
    ) {
        repository.login(username, password)
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