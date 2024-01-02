package org.openedx.course.presentation.unit.html

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.openedx.core.config.Config
import org.openedx.core.extension.isEmailValid
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.roundBorderWithoutBottom
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.windowSizeValue
import org.openedx.core.utils.EmailUtil

class HtmlUnitFragment : Fragment() {

    private val config by inject<Config>()
    private val edxCookieManager by inject<AppCookieManager>()
    private val networkConnection by inject<NetworkConnection>()
    private val notifier by inject<CourseNotifier>()
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

                var hasInternetConnection by remember {
                    mutableStateOf(networkConnection.isOnline())
                }

                val configuration = LocalConfiguration.current

                val bottomPadding = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    72.dp
                } else {
                    0.dp
                }

                val border = if (!isSystemInDarkTheme() && !config.isCourseUnitProgressEnabled()) {
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
                        if (hasInternetConnection) {
                            HTMLContentView(
                                windowSize = windowSize,
                                url = blockUrl,
                                cookieManager = edxCookieManager,
                                onCompletionSet = {
                                    lifecycleScope.launch {
                                        notifier.send(CourseCompletionSet())
                                    }
                                },
                                onWebPageLoaded = {
                                    isLoading = false
                                }
                            )
                        } else {
                            ConnectionErrorView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .background(MaterialTheme.appColors.background)
                            ) {
                                hasInternetConnection = networkConnection.isOnline()
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
    onCompletionSet: () -> Unit,
    onWebPageLoaded: () -> Unit,
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

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        Log.d("HTML", "onPageCommitVisible")
                        onWebPageLoaded()

                        evaluateJavascript(
                            """
                            ${'$'}(document).ajaxSuccess(function(event, request, settings)  {
                                if (settings.url.includes("publish_completion") && 
                                    request.responseText.includes("ok")) {
                                    javascript:window.callback.completionSet();
                                }
                            });
                        """.trimIndent(), null
                        )
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
                        if (request.url.toString() == view.url) {
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
        })
}

