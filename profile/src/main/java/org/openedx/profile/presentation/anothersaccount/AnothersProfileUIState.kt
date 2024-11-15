package org.openedx.profile.presentation.anothersaccount

import org.openedx.profile.domain.model.Account

sealed class AnothersProfileUIState {
    data class Data(val account: Account) : AnothersProfileUIState()
    data object Loading : AnothersProfileUIState()
}
