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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.global.ErrorType
import org.openedx.core.presentation.global.webview.WebViewUIAction
import org.openedx.core.presentation.global.webview.WebViewUIState
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.FullScreenErrorView
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.discovery.R
import org.openedx.discovery.presentation.catalog.CatalogWebViewScreen
import org.openedx.discovery.presentation.catalog.WebViewLink
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as CoreR

class WebViewDiscoveryFragment : Fragment() {

    private val viewModel by viewModel<WebViewDiscoveryViewModel> {
        parametersOf(requireArguments().getString(ARG_SEARCH_QUERY, ""))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val uiState by viewModel.uiState.collectAsState()
                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.hasInternetConnection)
                }
                WebViewDiscoveryScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    isPreLogin = viewModel.isPreLogin,
                    contentUrl = viewModel.discoveryUrl,
                    uriScheme = viewModel.uriScheme,
                    userAgent = viewModel.appUserAgent,
                    isRegistrationEnabled = viewModel.isRegistrationEnabled,
                    hasInternetConnection = hasInternetConnection,
                    onWebViewUIAction = { action ->
                        when (action) {
                            WebViewUIAction.WEB_PAGE_LOADED -> {
                                viewModel.onWebPageLoaded()
                            }

                            WebViewUIAction.WEB_PAGE_ERROR -> {
                                viewModel.onWebPageLoadError()
                            }

                            WebViewUIAction.RELOAD_WEB_PAGE -> {
                                hasInternetConnection = viewModel.hasInternetConnection
                                viewModel.onWebPageLoading()
                            }
                        }
                    },
                    onWebPageUpdated = { url ->
                        viewModel.updateDiscoveryUrl(url)
                    },
                    onUriClick = { param, authority ->
                        when (authority) {
                            WebViewLink.Authority.COURSE_INFO -> {
                                viewModel.courseInfoClickedEvent(param)
                                viewModel.infoCardClicked(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param,
                                    infoType = authority.name
                                )
                            }

                            WebViewLink.Authority.PROGRAM_INFO -> {
                                viewModel.programInfoClickedEvent(param)
                                viewModel.infoCardClicked(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param,
                                    infoType = authority.name
                                )
                            }

                            WebViewLink.Authority.EXTERNAL -> {
                                ActionDialogFragment.newInstance(
                                    title = getString(CoreR.string.core_leaving_the_app),
                                    message = getString(
                                        CoreR.string.core_leaving_the_app_message,
                                        getString(CoreR.string.platform_name)
                                    ),
                                    url = param,
                                    source = DiscoveryAnalyticsScreen.DISCOVERY.screenName
                                ).show(
                                    requireActivity().supportFragmentManager,
                                    ActionDialogFragment::class.simpleName
                                )
                            }

                            else -> {}
                        }
                    },
                    onRegisterClick = {
                        viewModel.navigateToSignUp(parentFragmentManager)
                    },
                    onSignInClick = {
                        viewModel.navigateToSignIn(parentFragmentManager)
                    },
                    onSettingsClick = {
                        viewModel.navigateToSettings(requireActivity().supportFragmentManager)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    }
                )
            }
        }
    }

    companion object {

        private const val ARG_SEARCH_QUERY = "query_search"

        fun newInstance(querySearch: String = ""): WebViewDiscoveryFragment {
            val fragment = WebViewDiscoveryFragment()
            fragment.arguments = bundleOf(ARG_SEARCH_QUERY to querySearch)
            return fragment
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun WebViewDiscoveryScreen(
    windowSize: WindowSize,
    uiState: WebViewUIState,
    isPreLogin: Boolean,
    contentUrl: String,
    uriScheme: String,
    isRegistrationEnabled: Boolean,
    userAgent: String,
    hasInternetConnection: Boolean,
    onWebViewUIAction: (WebViewUIAction) -> Unit,
    onWebPageUpdated: (String) -> Unit,
    onUriClick: (String, WebViewLink.Authority) -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        backgroundColor = MaterialTheme.appColors.background,
        bottomBar = {
            if (isPreLogin) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 32.dp,
                        )
                        .navigationBarsPadding()
                ) {
                    AuthButtonsPanel(
                        onRegisterClick = onRegisterClick,
                        onSignInClick = onSignInClick,
                        showRegisterButton = isRegistrationEnabled
                    )
                }
            }
        }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Toolbar(
                label = stringResource(id = R.string.discovery_explore_the_catalog),
                canShowBackBtn = isPreLogin,
                canShowSettingsIcon = !isPreLogin,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick
            )

            Surface {
                Box(
                    modifier = modifierScreenWidth
                        .fillMaxHeight()
                        .background(Color.White),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if ((uiState is WebViewUIState.Error).not()) {
                        if (hasInternetConnection) {
                            DiscoveryWebView(
                                contentUrl = contentUrl,
                                uriScheme = uriScheme,
                                userAgent = userAgent,
                                onWebPageLoaded = {
                                    if ((uiState is WebViewUIState.Error).not()) {
                                        onWebViewUIAction(WebViewUIAction.WEB_PAGE_LOADED)
                                    }
                                },
                                onWebPageUpdated = onWebPageUpdated,
                                onUriClick = onUriClick,
                                onWebPageLoadError = {
                                    onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR)
                                }
                            )
                        } else {
                            onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR)
                        }
                    }
                    if (uiState is WebViewUIState.Error) {
                        FullScreenErrorView(errorType = uiState.errorType) {
                            onWebViewUIAction(WebViewUIAction.RELOAD_WEB_PAGE)
                        }
                    }
                    if (uiState is WebViewUIState.Loading && hasInternetConnection) {
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

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun DiscoveryWebView(
    contentUrl: String,
    uriScheme: String,
    userAgent: String,
    onWebPageLoaded: () -> Unit,
    onWebPageUpdated: (String) -> Unit,
    onUriClick: (String, WebViewLink.Authority) -> Unit,
    onWebPageLoadError: () -> Unit
) {
    val webView = CatalogWebViewScreen(
        url = contentUrl,
        uriScheme = uriScheme,
        userAgent = userAgent,
        onWebPageLoaded = onWebPageLoaded,
        onWebPageUpdated = onWebPageUpdated,
        onUriClick = onUriClick,
        onWebPageLoadError = onWebPageLoadError
    )

    AndroidView(
        modifier = Modifier
            .background(MaterialTheme.appColors.background),
        factory = {
            webView
        }
    )

    HandleWebViewBackNavigation(webView = webView)
}

@Composable
private fun HandleWebViewBackNavigation(
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WebViewDiscoveryScreenPreview() {
    OpenEdXTheme {
        WebViewDiscoveryScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = WebViewUIState.Error(ErrorType.CONNECTION_ERROR),
            isPreLogin = false,
            contentUrl = "https://www.example.com/",
            uriScheme = "",
            isRegistrationEnabled = true,
            userAgent = "",
            hasInternetConnection = false,
            onWebViewUIAction = {},
            onWebPageUpdated = {},
            onUriClick = { _, _ -> },
            onRegisterClick = {},
            onSignInClick = {},
            onSettingsClick = {},
            onBackClick = {},
        )
    }
}
