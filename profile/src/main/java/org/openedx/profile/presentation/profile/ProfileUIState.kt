package org.openedx.profile.presentation.profile

import org.openedx.profile.domain.model.Account

sealed class ProfileUIState {
    /**
     * @param account User account data
     * @param versionName Version of the application (1.0.0)
     */
    data class Data(
        val account: Account,
        val versionName: String,
    ) : ProfileUIState()

    object Loading : ProfileUIState()
}
