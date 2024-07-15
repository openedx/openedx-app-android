package org.openedx.core.presentation.global.webview

import org.openedx.core.presentation.global.ErrorType

sealed class WebViewUIState {
    data object Loading : WebViewUIState()
    data object Loaded : WebViewUIState()
    data class Error(val errorType: ErrorType) : WebViewUIState()
}

enum class WebViewUIAction {
    WEB_PAGE_LOADED,
    WEB_PAGE_ERROR,
    RELOAD_WEB_PAGE
}
