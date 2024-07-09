package org.openedx.core.presentation.global.webview

import org.openedx.core.presentation.global.ErrorType

sealed class WebViewState {
    data object Loading : WebViewState()
    data object Loaded : WebViewState()
    data class Error(val errorType: ErrorType) : WebViewState()
}
