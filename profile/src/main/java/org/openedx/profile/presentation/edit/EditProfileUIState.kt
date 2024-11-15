package org.openedx.profile.presentation.edit

import org.openedx.profile.domain.model.Account

data class EditProfileUIState(
    val account: Account,
    val isUpdating: Boolean = false,
    val isLimited: Boolean
)
