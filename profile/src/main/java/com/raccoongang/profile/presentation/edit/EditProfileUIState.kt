package com.raccoongang.profile.presentation.edit

import com.raccoongang.core.domain.model.Account

data class EditProfileUIState(val account: Account, val isUpdating :Boolean = false)


