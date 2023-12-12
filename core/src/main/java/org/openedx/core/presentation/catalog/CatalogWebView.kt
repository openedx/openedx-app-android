package org.openedx.core.presentation.catalog

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.DefaultWebViewClient

@SuppressLint("SetJavaScriptEnabled", "ComposableNaming")
@Composable
fun CatalogWebViewScreen(
    url: String,
    isAllLinksExternal: Boolean = false,
    cookieManager: AppCookieManager,
    onWebPageLoaded: () -> Unit,
    openExternalLink: (String) -> Unit,
    onWebPageUpdated: (String) -> Unit = {},
    onEnrollClick: (String) -> Unit = {},
    onInfoCardClicked: (String, String) -> Unit = { _, _ -> },
): WebView {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return remember {
        WebView(context).apply {
            webViewClient = object : DefaultWebViewClient(
                context = context,
                webView = this@apply,
                isAllLinksExternal = isAllLinksExternal,
                coroutineScope = coroutineScope,
                cookieManager = cookieManager,
                openExternalLink = openExternalLink,
            ) {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let { onWebPageUpdated(it) }
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    onWebPageLoaded()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val clickUrl = request?.url?.toString() ?: ""
                    if (handleRecognizedLink(clickUrl)) {
                        return true
                    }

                    return super.shouldOverrideUrlLoading(view, request)
                }

                private fun handleRecognizedLink(clickUrl: String): Boolean {
                    val link = WebViewLink.parse(clickUrl) ?: return false

                    return when (link.authority) {
                        WebViewLink.Authority.COURSE_INFO,
                        WebViewLink.Authority.PROGRAM_INFO -> {
                            val pathId = link.params[WebViewLink.Param.PATH_ID] ?: ""
                            onInfoCardClicked(pathId, link.authority.name)
                            true
                        }

                        WebViewLink.Authority.ENROLL -> {
                            val courseId = link.params[WebViewLink.Param.COURSE_ID]
                            courseId?.let { onEnrollClick(it) }
                            true
                        }

                        else -> false
                    }
                }
            }

            with(settings) {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                setSupportZoom(true)
                loadsImagesAutomatically = true
                domStorageEnabled = true
            }
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            loadUrl(url)
        }
    }
}
