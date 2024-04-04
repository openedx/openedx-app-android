package org.openedx.settings.presentation.settings

import org.openedx.settings.domain.model.Configuration

sealed class SettingsUIState {
    data class Data(
        val configuration: Configuration,
    ) : SettingsUIState()

    object Loading : SettingsUIState()
}
