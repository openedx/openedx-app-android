package com.raccoongang.course.presentation.unit.html

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.raccoongang.core.system.AppCookieManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.ui.WindowSize
import com.raccoongang.core.ui.rememberWindowSize
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.windowSizeValue
import com.raccoongang.course.presentation.ui.ConnectionErrorView
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class HtmlUnitFragment : Fragment() {

    private val edxCookieManager by inject<AppCookieManager>()
    private val networkConnection by inject<NetworkConnection>()
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
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                var isLoading by remember {
                    mutableStateOf(true)
                }

                var hasInternetConnection by remember {
                    mutableStateOf(networkConnection.isOnline())
                }
                Surface(
                    shape = MaterialTheme.appShapes.screenBackgroundShape,
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (hasInternetConnection) {
                            HTMLContentView(
                                windowSize = windowSize,
                                url = blockUrl,
                                cookieManager = edxCookieManager,
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
                                CircularProgressIndicator()
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
                webViewClient = object : WebViewClient() {

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        Log.d("HTML", "onPageCommitVisible")
                        onWebPageLoaded()
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