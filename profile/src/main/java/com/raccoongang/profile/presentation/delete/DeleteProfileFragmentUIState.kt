package com.raccoongang.profile.presentation.delete

sealed class DeleteProfileFragmentUIState {
    object Initial: DeleteProfileFragmentUIState()
    object Loading: DeleteProfileFragmentUIState()
    data class Error(val message: String): DeleteProfileFragmentUIState()
    object Success: DeleteProfileFragmentUIState()
}