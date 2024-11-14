package org.openedx.core.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import org.openedx.core.ui.theme.appColors
import org.openedx.core.utils.EmailUtil
import org.openedx.foundation.extension.applyDarkModeIfEnabled
import org.openedx.foundation.extension.isEmailValid
import org.openedx.foundation.extension.replaceLinkTags
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.windowSizeValue
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WebContentScreen(
    windowSize: WindowSize,
    apiHostUrl: String? = null,
    title: String,
    onBackClick: () -> Unit,
    htmlBody: String? = null,
    contentUrl: String? = null,
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
            .semantics {
                testTagsAsResourceId = true
            },
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) {
        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
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
                    if (htmlBody.isNullOrEmpty() && contentUrl.isNullOrEmpty()) {
                        CircularProgress()
                    } else {
                        var webViewAlpha by rememberSaveable { mutableFloatStateOf(0f) }
                        Surface(
                            Modifier.alpha(webViewAlpha),
                            color = MaterialTheme.appColors.background
                        ) {
                            WebViewContent(
                                apiHostUrl = apiHostUrl,
                                body = htmlBody,
                                contentUrl = contentUrl,
                                onWebPageLoaded = {
                                    webViewAlpha = 1f
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun WebViewContent(
    apiHostUrl: String? = null,
    body: String? = null,
    contentUrl: String? = null,
    onWebPageLoaded: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    AndroidView(
        factory = {
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        onWebPageLoaded()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val clickUrl = request?.url?.toString() ?: ""
                        return if (clickUrl.isNotEmpty() && clickUrl.startsWith("http")) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl)))
                            true
                        } else if (clickUrl.startsWith("mailto:")) {
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
                body?.let {
                    loadDataWithBaseURL(
                        apiHostUrl,
                        body.replaceLinkTags(isDarkTheme),
                        "text/html",
                        StandardCharsets.UTF_8.name(),
                        null
                    )
                }
                contentUrl?.let {
                    loadUrl(it)
                }
                applyDarkModeIfEnabled(isDarkTheme)
            }
        },
        update = { webView ->
            body?.let {
                webView.loadDataWithBaseURL(
                    apiHostUrl,
                    body.replaceLinkTags(isDarkTheme),
                    "text/html",
                    StandardCharsets.UTF_8.name(),
                    null
                )
            }
            contentUrl?.let {
                webView.loadUrl(it)
            }
        }
    )
}
