package org.openedx.profile.presentation.profile

import org.openedx.profile.domain.model.Account
import org.openedx.profile.domain.model.AppConfig

sealed class ProfileUIState {
    data class Data(val appConfig: AppConfig, val account: Account) : ProfileUIState()
    object Loading : ProfileUIState()
}
