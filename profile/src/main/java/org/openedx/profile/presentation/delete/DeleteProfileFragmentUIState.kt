package org.openedx.profile.presentation.delete

sealed class DeleteProfileFragmentUIState {
    data object Initial : DeleteProfileFragmentUIState()
    data object Loading : DeleteProfileFragmentUIState()
    data class Error(val message: String) : DeleteProfileFragmentUIState()
    data object Success : DeleteProfileFragmentUIState()
}
