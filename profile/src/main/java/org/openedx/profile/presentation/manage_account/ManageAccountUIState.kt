package org.openedx.profile.presentation.manage_account

import org.openedx.profile.domain.model.Account

sealed class ManageAccountUIState {
    /**
     * @param account User account data
     */
    data class Data(
        val account: Account
    ) : ManageAccountUIState()

    object Loading : ManageAccountUIState()
}
