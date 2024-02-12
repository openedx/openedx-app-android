package org.openedx.profile.presentation.profile

import org.openedx.profile.domain.model.Account
import org.openedx.profile.domain.model.Configuration

sealed class ProfileUIState {
    /**
     * @param account User account data
     * @param configuration Configuration data
     */
    data class Data(
        val account: Account,
        val configuration: Configuration,
    ) : ProfileUIState()

    object Loading : ProfileUIState()
}
