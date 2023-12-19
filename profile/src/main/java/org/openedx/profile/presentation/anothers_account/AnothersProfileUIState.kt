package org.openedx.profile.presentation.anothers_account

import org.openedx.profile.domain.model.Account

sealed class AnothersProfileUIState {
    data class Data(val account: Account) : AnothersProfileUIState()
    object Loading : AnothersProfileUIState()
}