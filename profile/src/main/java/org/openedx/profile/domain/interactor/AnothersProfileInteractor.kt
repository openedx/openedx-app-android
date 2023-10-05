package org.openedx.profile.domain.interactor

import org.openedx.profile.data.repository.ProfileRepository

class AnothersProfileInteractor(private val repository: ProfileRepository) {

    suspend fun getAccount(username: String) = repository.getAccount(username)
}