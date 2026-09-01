package org.openedx.core.ui
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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

@Composable
fun SSOWebContentScreen(
    windowSize: WindowSize,
    url: String,
    title: String,
    ssoFinishedUrl: String,
    onBackClick: () -> Unit,
    onWebPageLoaded: () -> Unit,
    onWebPageUpdated: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                    super.onPageFinished(view, pageUrl)
                    if (pageUrl == null) return
                    if (pageUrl.contains(ssoFinishedUrl)) {
                        val header = getCookie(pageUrl, "edx-jwt-cookie-header-payload")
                        val signature = getCookie(pageUrl, "edx-jwt-cookie-signature")
                        if (!header.isNullOrEmpty() && !signature.isNullOrEmpty()) {
                            onWebPageUpdated("$header.$signature")
                        } else {
                            // Handle error logic here if needed
                        }
                    }
                }
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    onWebPageLoaded()
                }
            }
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            loadUrl(url)
        }
    }

    val screenWidthModifier = windowSize.windowSizeValue(
        expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
        compact = Modifier.fillMaxWidth()
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(), // Fix 2: Changed from statusBarsInset()
        contentAlignment = Alignment.TopCenter
    ) {
        Column(screenWidthModifier) {
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
                        factory = { webView }
                    )
                }
            }
        }
    }
}

fun getCookie(siteName: String?, cookieName: String?): String? {
    val cookieValue = siteName
        ?.takeIf { it.isNotEmpty() }
        ?.let { sn ->
            cookieName?.takeIf { it.isNotEmpty() }?.let { cn ->
                CookieManager.getInstance().getCookie(sn)
                    ?.split(";")
                    ?.firstOrNull { it.trim().startsWith("$cn=") }
                    ?.substringAfter("=")
            }
        }
    return cookieValue
}
