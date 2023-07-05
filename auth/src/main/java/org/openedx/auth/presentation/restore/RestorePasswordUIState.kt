package org.openedx.auth.presentation.restore

sealed class RestorePasswordUIState {
    object Initial : RestorePasswordUIState()
    object Loading : RestorePasswordUIState()
    class Success(val email: String) : RestorePasswordUIState()
}