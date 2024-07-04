package org.openedx.course.presentation.unit.html

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.extension.applyDarkModeIfEnabled
import org.openedx.core.extension.isEmailValid
import org.openedx.core.extension.loadUrl
import org.openedx.core.system.AppCookieManager
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.SomethingWentWrongErrorView
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.roundBorderWithoutBottom
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.windowSizeValue
import org.openedx.core.utils.EmailUtil

class HtmlUnitFragment : Fragment() {

    private val viewModel by viewModel<HtmlUnitViewModel>()
    private var blockId: String = ""
    private var blockUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        blockUrl = requireArguments().getString(ARG_BLOCK_URL, "")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                var isLoading by remember {
                    mutableStateOf(true)
                }

                var isError by remember {
                    mutableStateOf(false)
                }

                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.isOnline)
                }

                val injectJSList by viewModel.injectJSList.collectAsState()

                val configuration = LocalConfiguration.current

                val bottomPadding =
                    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        72.dp
                    } else {
                        0.dp
                    }

                val border = if (!isSystemInDarkTheme() && !viewModel.isCourseUnitProgressEnabled) {
                    Modifier.roundBorderWithoutBottom(
                        borderWidth = 2.dp,
                        cornerRadius = 30.dp
                    )
                } else {
                    Modifier
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding)
                            .background(Color.White)
                            .then(border),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (hasInternetConnection && !isError) {
                            HTMLContentView(
                                windowSize = windowSize,
                                url = blockUrl,
                                cookieManager = viewModel.cookieManager,
                                apiHostURL = viewModel.apiHostURL,
                                isLoading = isLoading,
                                injectJSList = injectJSList,
                                onCompletionSet = {
                                    viewModel.notifyCompletionSet()
                                },
                                onWebPageLoading = {
                                    isLoading = true
                                    isError = false
                                },
                                onWebPageLoaded = {
                                    isLoading = false
                                    isError = false
                                    if (isAdded) viewModel.setWebPageLoaded(requireContext().assets)
                                },
                                onWebPageLoadError = {
                                    isLoading = false
                                    isError = true
                                }
                            )
                        } else {
                            isError = true
                        }
                        if (isError) {
                            val onReloadClick = {
                                isError = false
                                hasInternetConnection = viewModel.isOnline
                            }
                            if (!hasInternetConnection) {
                                ConnectionErrorView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(MaterialTheme.appColors.background),
                                    onReloadClick = onReloadClick
                                )
                            } else {
                                SomethingWentWrongErrorView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(MaterialTheme.appColors.background),
                                    onReloadClick = onReloadClick
                                )
                            }
                        }
                        if (isLoading && hasInternetConnection) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }
                    }
                }
            }
        }
    }


    companion object {
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_BLOCK_URL = "blockUrl"
        fun newInstance(
            blockId: String,
            blockUrl: String,
        ): HtmlUnitFragment {
            val fragment = HtmlUnitFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_ID to blockId,
                ARG_BLOCK_URL to blockUrl
            )
            return fragment
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun HTMLContentView(
    windowSize: WindowSize,
    url: String,
    cookieManager: AppCookieManager,
    apiHostURL: String,
    isLoading: Boolean,
    injectJSList: List<String>,
    onCompletionSet: () -> Unit,
    onWebPageLoading: () -> Unit,
    onWebPageLoaded: () -> Unit,
    onWebPageLoadError: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val screenWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxWidth()
            )
        )
    }

    val isDarkTheme = isSystemInDarkTheme()

    AndroidView(
        modifier = Modifier
            .then(screenWidth)
            .background(MaterialTheme.appColors.background),
        factory = {
            WebView(context).apply {
                addJavascriptInterface(object {
                    @Suppress("unused")
                    @JavascriptInterface
                    fun completionSet() {
                        onCompletionSet()
                    }
                }, "callback")
                webViewClient = object : WebViewClient() {

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onWebPageLoading()
                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        Log.d("HTML", "onPageCommitVisible")
                        onWebPageLoaded()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val clickUrl = request?.url?.toString() ?: ""
                        return if (clickUrl.isNotEmpty() &&
                            (clickUrl.startsWith("http://") ||
                                    clickUrl.startsWith("https://"))
                        ) {
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

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        if (request.url.toString().startsWith(apiHostURL)) {
                            when (errorResponse.statusCode) {
                                403, 401, 404 -> {
                                    coroutineScope.launch {
                                        cookieManager.tryToRefreshSessionCookie()
                                        loadUrl(url)
                                    }
                                }
                            }
                        }
                        super.onReceivedHttpError(view, request, errorResponse)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        onWebPageLoadError()
                        super.onReceivedError(view, request, error)
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

                loadUrl(url, coroutineScope, cookieManager)
                applyDarkModeIfEnabled(isDarkTheme)
            }
        },
        update = { webView ->
            if (!isLoading && injectJSList.isNotEmpty()) {
                injectJSList.forEach { webView.evaluateJavascript(it, null) }
            }
        }
    )
}

