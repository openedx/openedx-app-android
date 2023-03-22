package com.raccoongang.profile.domain.interactor

import com.raccoongang.profile.data.repository.ProfileRepository
import java.io.File

class ProfileInteractor(private val repository: ProfileRepository) {

    suspend fun getAccount() = repository.getAccount()

    suspend fun updateAccount(fields: Map<String, Any?>) = repository.updateAccount(fields)

    suspend fun setProfileImage(file: File, mimeType: String) = repository.setProfileImage(file, mimeType)

    suspend fun deleteProfileImage() = repository.deleteProfileImage()

    suspend fun deactivateAccount(password: String) = repository.deactivateAccount(password)

    suspend fun logout() {
        repository.logout()
    }
}