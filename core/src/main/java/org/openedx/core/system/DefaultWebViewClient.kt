package org.openedx.core.system

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.openedx.core.extension.isEmailValid
import org.openedx.core.utils.EmailUtil

open class DefaultWebViewClient(
    val context: Context,
    val webView: WebView,
    val coroutineScope: CoroutineScope,
    val cookieManager: AppCookieManager,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val clickUrl = request?.url?.toString() ?: ""

        return if (clickUrl.startsWith("mailto:")) {
            val email = clickUrl.replace("mailto:", "")
            if (email.isEmailValid()) {
                EmailUtil.sendEmailIntent(context, email, "", "")
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (request.url.toString() == view.url) {
            when (errorResponse.statusCode) {
                403, 401, 404 -> {
                    coroutineScope.launch {
                        cookieManager.tryToRefreshSessionCookie()
                        webView.loadUrl(request.url.toString())
                    }
                }
            }
        }
        super.onReceivedHttpError(view, request, errorResponse)
    }
}
