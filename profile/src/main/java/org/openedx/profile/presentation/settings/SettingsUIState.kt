package org.openedx.profile.presentation.settings

import org.openedx.profile.domain.model.Configuration

sealed class SettingsUIState {
    data class Data(
        val configuration: Configuration,
    ) : SettingsUIState()

    object Loading : SettingsUIState()
}
