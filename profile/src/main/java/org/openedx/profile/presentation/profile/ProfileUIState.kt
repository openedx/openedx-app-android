package org.openedx.profile.presentation.profile

import org.openedx.profile.domain.model.Account

sealed class ProfileUIState {
    /**
     * @param account User account data
     * @param configuration Configuration data
     */
    data class Data(
        val account: Account
    ) : ProfileUIState()

    object Loading : ProfileUIState()
}
