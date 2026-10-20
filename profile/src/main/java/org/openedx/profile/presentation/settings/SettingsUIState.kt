package org.openedx.profile.presentation.settings

import org.openedx.core.instance.Instance
import org.openedx.profile.domain.model.Configuration

sealed class SettingsUIState {
    data class Data(
        val configuration: Configuration,
        /** The selected site, shown only when there are several to switch between. */
        val learningSite: Instance? = null,
    ) : SettingsUIState()

    object Loading : SettingsUIState()
}
