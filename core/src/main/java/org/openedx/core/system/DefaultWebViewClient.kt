package org.openedx.core.system

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.openedx.core.extension.isEmailValid
import org.openedx.core.utils.EmailUtil

open class DefaultWebViewClient(
    val context: Context,
    val webView: WebView,
    val isAllLinksExternal: Boolean,
    val refreshSessionCookie: () -> Unit,
    val openExternalLink: (String) -> Unit,
) : WebViewClient() {

    private var hostForThisPage: String? = null

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        if (hostForThisPage == null && url != null) {
            hostForThisPage = Uri.parse(url).host
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val clickUrl = request?.url?.toString() ?: ""

        if (isAllLinksExternal || isExternalLink(clickUrl)) {
            openExternalLink(clickUrl)
            return true
        }

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
                    refreshSessionCookie()
                    webView.loadUrl(request.url.toString())
                }
            }
        }
        super.onReceivedHttpError(view, request, errorResponse)
    }

    private fun isExternalLink(strUrl: String?): Boolean {
        return strUrl?.let { url ->
            val uri = Uri.parse(url)
            val externalLinkValue = if (uri.isHierarchical) {
                uri.getQueryParameter(QUERY_PARAM_EXTERNAL_LINK)
            } else {
                null
            }
            hostForThisPage != null && hostForThisPage != uri.host ||
                    externalLinkValue?.toBoolean() == true
        } ?: false
    }

    companion object {
        const val QUERY_PARAM_EXTERNAL_LINK = "external_link"
    }
}
