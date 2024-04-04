package org.openedx.settings.domain.interactor

import org.openedx.settings.data.repository.SettingsRepository

class SettingsInteractor(private val repository: SettingsRepository) {

    suspend fun logout() {
        repository.logout()
    }
}