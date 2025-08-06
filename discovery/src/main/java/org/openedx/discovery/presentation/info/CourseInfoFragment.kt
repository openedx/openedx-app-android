package org.openedx.discovery.presentation.info

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.dialog.alert.InfoDialogFragment
import org.openedx.core.presentation.global.webview.WebViewUIAction
import org.openedx.core.presentation.global.webview.WebViewUIState
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.FullScreenErrorView
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.discovery.R
import org.openedx.discovery.presentation.DiscoveryAnalyticsScreen
import org.openedx.discovery.presentation.catalog.CatalogWebViewScreen
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR
import org.openedx.discovery.presentation.catalog.WebViewLink.Authority as linkAuthority

class CourseInfoFragment : Fragment() {

    private val viewModel by viewModel<CourseInfoViewModel> {
        parametersOf(
            requireArguments().getString(ARG_PATH_ID, ""),
            requireArguments().getString(ARG_INFO_TYPE, "")
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val uiMessage by viewModel.uiMessage.collectAsState(initial = null)
                val showAlert by viewModel.showAlert.collectAsState(initial = false)
                val uiState by viewModel.uiState.collectAsState()
                val webViewState by viewModel.webViewState.collectAsState()
                val windowSize = rememberWindowSize()
                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.hasInternetConnection)
                }

                LaunchedEffect(showAlert) {
                    if (showAlert) {
                        InfoDialogFragment.newInstance(
                            title = context.getString(CoreR.string.core_enrollment_error),
                            message = context.getString(
                                CoreR.string.core_enrollment_error_message,
                                getString(CoreR.string.platform_name)
                            )
                        ).show(
                            requireActivity().supportFragmentManager,
                            InfoDialogFragment::class.simpleName
                        )
                    }
                }

                LaunchedEffect((uiState as CourseInfoUIState.CourseInfo).enrollmentSuccess.get()) {
                    if ((uiState as CourseInfoUIState.CourseInfo).enrollmentSuccess.get()
                            .isNotEmpty()
                    ) {
                        viewModel.onSuccessfulCourseEnrollment(
                            fragmentManager = requireActivity().supportFragmentManager,
                            courseId = (uiState as CourseInfoUIState.CourseInfo).enrollmentSuccess.get(),
                        )
                        // Clear after navigation
                        (uiState as CourseInfoUIState.CourseInfo).enrollmentSuccess.set("")
                    }
                }

                CourseInfoScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    webViewUIState = webViewState,
                    uiMessage = uiMessage,
                    uriScheme = viewModel.uriScheme,
                    isRegistrationEnabled = viewModel.isRegistrationEnabled,
                    userAgent = viewModel.appUserAgent,
                    hasInternetConnection = hasInternetConnection,
                    onWebViewUIAction = { action ->
                        when (action) {
                            WebViewUIAction.WEB_PAGE_LOADED -> {
                                viewModel.onWebPageLoaded()
                            }

                            WebViewUIAction.WEB_PAGE_ERROR -> {
                                viewModel.onWebPageError()
                            }

                            WebViewUIAction.RELOAD_WEB_PAGE -> {
                                hasInternetConnection = viewModel.hasInternetConnection
                                viewModel.onWebPageLoading()
                            }
                        }
                    },
                    onRegisterClick = {
                        viewModel.navigateToSignUp(
                            parentFragmentManager,
                            viewModel.pathId,
                            viewModel.infoType
                        )
                    },
                    onSignInClick = {
                        viewModel.navigateToSignIn(
                            parentFragmentManager,
                            viewModel.pathId,
                            viewModel.infoType
                        )
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onUriClick = { param, type ->
                        when (type) {
                            linkAuthority.PROGRAM_INFO -> {
                                viewModel.programInfoClickedEvent(param)
                                viewModel.infoCardClicked(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param,
                                    infoType = type.name
                                )
                            }

                            linkAuthority.COURSE_INFO -> {
                                viewModel.courseInfoClickedEvent(param)
                                viewModel.infoCardClicked(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param,
                                    infoType = type.name
                                )
                            }

                            linkAuthority.EXTERNAL -> {
                                ActionDialogFragment.newInstance(
                                    title = getString(CoreR.string.core_leaving_the_app),
                                    message = getString(
                                        CoreR.string.core_leaving_the_app_message,
                                        getString(CoreR.string.platform_name)
                                    ),
                                    url = param,
                                    source = DiscoveryAnalyticsScreen.COURSE_INFO.screenName
                                ).show(
                                    requireActivity().supportFragmentManager,
                                    ActionDialogFragment::class.simpleName
                                )
                            }

                            linkAuthority.ENROLL -> {
                                viewModel.courseEnrollClickedEvent(param)
                                if ((uiState as CourseInfoUIState.CourseInfo).isPreLogin) {
                                    viewModel.navigateToSignUp(
                                        fragmentManager = requireActivity().supportFragmentManager,
                                        courseId = viewModel.pathId,
                                        infoType = viewModel.infoType
                                    )
                                } else {
                                    viewModel.enrollInACourse(courseId = param)
                                }
                            }

                            else -> {}
                        }
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_PATH_ID = "path_id"
        private const val ARG_INFO_TYPE = "info_type"

        fun newInstance(
            pathId: String,
            infoType: String,
        ): CourseInfoFragment {
            val fragment = CourseInfoFragment()
            fragment.arguments = bundleOf(
                ARG_PATH_ID to pathId,
                ARG_INFO_TYPE to infoType
            )
            return fragment
        }
    }
}

@Composable
private fun CourseInfoScreen(
    windowSize: WindowSize,
    uiState: CourseInfoUIState,
    webViewUIState: WebViewUIState,
    uiMessage: UIMessage?,
    uriScheme: String,
    isRegistrationEnabled: Boolean,
    userAgent: String,
    hasInternetConnection: Boolean,
    onWebViewUIAction: (WebViewUIAction) -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
    onBackClick: () -> Unit,
    onUriClick: (String, linkAuthority) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current

    HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background,
        bottomBar = {
            if ((uiState as CourseInfoUIState.CourseInfo).isPreLogin) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 32.dp,
                        )
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
                label = stringResource(id = R.string.discovery_Discovery),
                canShowBackBtn = true,
                onBackClick = onBackClick
            )

            Surface {
                Box(
                    modifier = modifierScreenWidth
                        .fillMaxHeight()
                        .background(Color.White)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if ((webViewUIState is WebViewUIState.Error).not()) {
                        if (hasInternetConnection) {
                            CourseInfoWebView(
                                contentUrl = (uiState as CourseInfoUIState.CourseInfo).initialUrl,
                                uriScheme = uriScheme,
                                userAgent = userAgent,
                                onWebPageLoaded = { onWebViewUIAction(WebViewUIAction.WEB_PAGE_LOADED) },
                                onUriClick = onUriClick,
                                onWebPageLoadError = {
                                    onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR)
                                }
                            )
                        } else {
                            onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR)
                        }
                    }
                    if (webViewUIState is WebViewUIState.Error) {
                        FullScreenErrorView(errorType = webViewUIState.errorType) {
                            onWebViewUIAction(WebViewUIAction.RELOAD_WEB_PAGE)
                        }
                    }
                    if (webViewUIState is WebViewUIState.Loading && hasInternetConnection) {
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
private fun CourseInfoWebView(
    contentUrl: String,
    uriScheme: String,
    userAgent: String,
    onWebPageLoaded: () -> Unit,
    onUriClick: (String, linkAuthority) -> Unit,
    onWebPageLoadError: () -> Unit
) {
    val webView = CatalogWebViewScreen(
        url = contentUrl,
        uriScheme = uriScheme,
        userAgent = userAgent,
        isAllLinksExternal = true,
        onWebPageLoaded = onWebPageLoaded,
        onUriClick = onUriClick,
        onWebPageLoadError = onWebPageLoadError
    )

    AndroidView(
        modifier = Modifier
            .background(MaterialTheme.appColors.background),
        factory = {
            webView
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CourseInfoScreenPreview() {
    OpenEdXTheme {
        CourseInfoScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseInfoUIState.CourseInfo(
                initialUrl = "https://www.example.com/",
                isPreLogin = false,
                enrollmentSuccess = AtomicReference("")
            ),
            uiMessage = null,
            uriScheme = "",
            isRegistrationEnabled = true,
            userAgent = "",
            hasInternetConnection = false,
            onWebViewUIAction = {},
            onRegisterClick = {},
            onSignInClick = {},
            onBackClick = {},
            onUriClick = { _, _ -> },
            webViewUIState = WebViewUIState.Loading,
        )
    }
}
