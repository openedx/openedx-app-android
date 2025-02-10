package org.openedx.core.extension

import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.openedx.core.system.AppCookieManager

fun WebView.loadUrl(url: String, scope: CoroutineScope, cookieManager: AppCookieManager) {
    if (cookieManager.isSessionCookieMissingOrExpired()) {
        scope.launch {
            cookieManager.tryToRefreshSessionCookie()
            loadUrl(url)
        }
    } else {
        loadUrl(url)
    }
}
