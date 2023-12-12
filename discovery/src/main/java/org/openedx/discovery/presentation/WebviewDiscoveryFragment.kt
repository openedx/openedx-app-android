package org.openedx.discovery.presentation

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.presentation.catalog.CatalogWebViewScreen
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.discovery.R
import org.openedx.core.R as CoreR

class WebviewDiscoveryFragment : Fragment() {

    private val edxCookieManager by inject<AppCookieManager>()
    private val networkConnection by inject<NetworkConnection>()
    private val router: DiscoveryRouter by inject()
    private val viewModel by viewModel<WebViewDiscoveryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                var isLoading by remember { mutableStateOf(true) }
                var hasInternetConnection by remember {
                    mutableStateOf(networkConnection.isOnline())
                }

                Surface {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (hasInternetConnection) {
                            WebViewDiscoverScreen(
                                windowSize = windowSize,
                                url = viewModel.discoveryUrl,
                                cookieManager = edxCookieManager,
                                onWebPageLoaded = {
                                    isLoading = false
                                },
                                onWebPageUpdated = { url ->
                                    viewModel.updateDiscoveryUrl(url)
                                },
                                onInfoCardClicked = { pathId, infoType ->
                                    router.navigateToCourseInfo(
                                        requireActivity().supportFragmentManager,
                                        pathId,
                                        infoType
                                    )
                                },
                                openExternalLink = { url ->
                                    ActionDialogFragment.newInstance(
                                        title = getString(CoreR.string.core_leaving_the_app),
                                        message = getString(
                                            CoreR.string.core_leaving_the_app_message,
                                            getString(CoreR.string.platform_name)
                                        ),
                                        url = url,
                                    ).show(
                                        requireActivity().supportFragmentManager,
                                        ActionDialogFragment::class.simpleName
                                    )
                                },
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
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun WebViewDiscoverScreen(
    windowSize: WindowSize,
    url: String,
    cookieManager: AppCookieManager,
    onWebPageLoaded: () -> Unit,
    onWebPageUpdated: (String) -> Unit,
    onInfoCardClicked: (String, String) -> Unit,
    openExternalLink: (String) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current

    val webView = CatalogWebViewScreen(
        url = url,
        cookieManager = cookieManager,
        onWebPageLoaded = onWebPageLoaded,
        onWebPageUpdated = onWebPageUpdated,
        openExternalLink = openExternalLink,
        onInfoCardClicked = onInfoCardClicked,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) {
        val modifierScreenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        Modifier.widthIn(Dp.Unspecified, 560.dp)
                    } else {
                        Modifier.widthIn(Dp.Unspecified, 650.dp)
                    },
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = modifierScreenWidth
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        text = stringResource(R.string.discovery_explore_the_catalog),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))

                AndroidView(
                    modifier = Modifier
                        .then(modifierScreenWidth)
                        .background(MaterialTheme.appColors.background),
                    factory = {
                        webView
                    }
                )
            }
        }
    }

    HandleWebViewBackNavigation(webView = webView)
}

@Composable
fun HandleWebViewBackNavigation(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    webView: WebView?
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val onBackPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView?.canGoBack() == true) {
                    webView.goBack()
                } else {
                    this.isEnabled = false
                    backDispatcher?.onBackPressed()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onBackPressedCallback.isEnabled = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                onBackPressedCallback.isEnabled = false
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(backDispatcher) {
        backDispatcher?.addCallback(onBackPressedCallback)
        onDispose {
            onBackPressedCallback.remove()
        }
    }
}
