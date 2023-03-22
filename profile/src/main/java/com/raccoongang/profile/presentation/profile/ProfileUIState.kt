package com.raccoongang.profile.presentation.profile

import com.raccoongang.core.domain.model.Account

sealed class ProfileUIState {
    data class Data(val account: Account) : ProfileUIState()
    object Loading : ProfileUIState()
}