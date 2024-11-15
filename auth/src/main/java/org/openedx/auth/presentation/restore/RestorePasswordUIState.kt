package org.openedx.auth.presentation.restore

sealed class RestorePasswordUIState {
    data object Initial : RestorePasswordUIState()
    data object Loading : RestorePasswordUIState()
    class Success(val email: String) : RestorePasswordUIState()
}
