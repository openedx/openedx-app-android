package org.openedx.core.ui

import android.annotation.SuppressLint
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import org.openedx.core.ui.theme.appColors
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.windowSizeValue


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SSOWebContentScreen(
    windowSize: WindowSize,
    url: String,
    uriScheme: String,
    title: String,
    onBackClick: () -> Unit,
    onWebPageLoaded: () -> Unit,
    onWebPageUpdated: (String) -> Unit = {},
){
    val webView = SSOWebView(
        url = url,
        uriScheme = uriScheme,
        onWebPageLoaded = onWebPageLoaded,
        onWebPageUpdated = onWebPageUpdated
    )
    val screenWidth by remember(key1 = windowSize) {
        mutableStateOf(
           windowSize.windowSizeValue(
               expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
               compact = Modifier.fillMaxWidth(),
               )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsInset()
            .displayCutoutForLandscape(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(screenWidth) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Toolbar(
                    label = title,
                    canShowBackBtn = true,
                    onBackClick = onBackClick
                )
            }
            Surface(
                Modifier.fillMaxSize(),
                color = MaterialTheme.appColors.background
            ) {

                val webViewAlpha by rememberSaveable { mutableFloatStateOf(1f) }
                Surface(
                    Modifier.alpha(webViewAlpha),
                    color = MaterialTheme.appColors.background
                ) {
                    AndroidView(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.background),
                        factory = {
                            webView
                        }
                    )
                }

            }
        }
    }



}

@SuppressLint("SetJavaScriptEnabled", "ComposableNaming")
@Composable
fun SSOWebView(
    url: String,
    uriScheme: String,
    onWebPageLoaded: () -> Unit,
    onWebPageUpdated: (String) -> Unit = {},
): WebView {
    val context = LocalContext.current

    return remember {
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                        val jwtToken = getCookie(url, "edx-jwt-cookie-header-payload")  + getCookie(url, "edx-jwt-cookie-signature")
                        onWebPageUpdated(jwtToken)
                    }
                }

                override fun onReceivedLoginRequest(
                    view: WebView?,
                    realm: String?,
                    account: String?,
                    args: String?
                ) {
                    super.onReceivedLoginRequest(view, realm, account, args)
                }

                override fun onFormResubmission(
                    view: WebView?,
                    dontResend: Message?,
                    resend: Message?
                ) {
                    super.onFormResubmission(view, dontResend, resend)
                }
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    onWebPageLoaded()
                }

            }

            with(settings) {
                javaScriptEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                setSupportZoom(true)
                loadsImagesAutomatically = true
                domStorageEnabled = true

            }
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true

            loadUrl(url)
        }
    }
}

fun getCookie(siteName: String?, cookieName: String?): String? {
    var cookieValue: String? = ""

    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(siteName)
    val temp = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()
    for (ar1 in temp) {
        if (ar1.contains(cookieName!!)) {
            val temp1 = ar1.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            cookieValue = temp1[1]
            break
        }
    }
    return cookieValue
}