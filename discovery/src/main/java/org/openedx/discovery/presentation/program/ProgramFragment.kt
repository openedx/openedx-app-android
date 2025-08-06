package org.openedx.discovery.presentation.program

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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.extension.loadUrl
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.dialog.alert.InfoDialogFragment
import org.openedx.core.presentation.global.webview.WebViewUIAction
import org.openedx.core.system.AppCookieManager
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
import org.openedx.foundation.extension.takeIfNotEmpty
import org.openedx.foundation.extension.toastMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as coreR
import org.openedx.discovery.presentation.catalog.WebViewLink.Authority as linkAuthority

class ProgramFragment : Fragment() {

    private val viewModel by viewModel<ProgramViewModel>()
    private var isNestedFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isNestedFragment = arguments?.getBoolean(ARG_NESTED_FRAGMENT, false) ?: false
        if (isNestedFragment.not()) {
            lifecycle.addObserver(viewModel)
        }
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
                val uiState by viewModel.uiState.collectAsState(initial = ProgramUIState.Loading)
                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.hasInternetConnection)
                }

                if (isNestedFragment.not()) {
                    DisposableEffect(uiState is ProgramUIState.CourseEnrolled) {
                        if (uiState is ProgramUIState.CourseEnrolled) {
                            val courseId = (uiState as ProgramUIState.CourseEnrolled).courseId
                            val isEnrolled = (uiState as ProgramUIState.CourseEnrolled).isEnrolled

                            if (isEnrolled) {
                                viewModel.onEnrolledCourseClick(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    courseId = courseId,
                                )
                                context.toastMessage(getString(R.string.discovery_enrolled_successfully))
                            } else {
                                InfoDialogFragment.newInstance(
                                    title = getString(coreR.string.core_enrollment_error),
                                    message = getString(coreR.string.core_enrollment_error_message)
                                ).show(
                                    requireActivity().supportFragmentManager,
                                    InfoDialogFragment::class.simpleName
                                )
                            }
                        }
                        onDispose {}
                    }
                }

                ProgramInfoScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    contentUrl = getInitialUrl(),
                    cookieManager = viewModel.cookieManager,
                    canShowBackBtn = arguments?.getString(ARG_PATH_ID, "")
                        ?.isNotEmpty() == true,
                    isNestedFragment = isNestedFragment,
                    uriScheme = viewModel.uriScheme,
                    userAgent = viewModel.appUserAgent,
                    hasInternetConnection = hasInternetConnection,
                    onWebViewUIAction = { action ->
                        when (action) {
                            WebViewUIAction.WEB_PAGE_LOADED -> {
                                viewModel.showLoading(false)
                            }

                            WebViewUIAction.WEB_PAGE_ERROR -> {
                                viewModel.onPageLoadError()
                            }

                            WebViewUIAction.RELOAD_WEB_PAGE -> {
                                hasInternetConnection = viewModel.hasInternetConnection
                                viewModel.showLoading(true)
                            }
                        }
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onUriClick = { param, type ->
                        when (type) {
                            linkAuthority.ENROLLED_COURSE_INFO -> {
                                viewModel.onEnrolledCourseClick(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    courseId = param
                                )
                            }

                            linkAuthority.ENROLLED_PROGRAM_INFO -> {
                                viewModel.onProgramCardClick(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param
                                )
                            }

                            linkAuthority.PROGRAM_INFO,
                            linkAuthority.COURSE_INFO -> {
                                viewModel.onViewCourseClick(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    courseId = param,
                                    infoType = type.name
                                )
                            }

                            linkAuthority.ENROLL -> {
                                viewModel.enrollInACourse(param)
                            }

                            linkAuthority.COURSE -> {
                                viewModel.navigateToDiscovery()
                            }

                            linkAuthority.EXTERNAL -> {
                                ActionDialogFragment.newInstance(
                                    title = getString(coreR.string.core_leaving_the_app),
                                    message = getString(
                                        coreR.string.core_leaving_the_app_message,
                                        getString(coreR.string.platform_name)
                                    ),
                                    url = param,
                                    source = DiscoveryAnalyticsScreen.PROGRAM.screenName
                                ).show(
                                    requireActivity().supportFragmentManager,
                                    ActionDialogFragment::class.simpleName
                                )
                            }
                        }
                    },
                    onSettingsClick = {
                        viewModel.navigateToSettings(requireActivity().supportFragmentManager)
                    }
                )
            }
        }
    }

    private fun getInitialUrl(): String {
        val pathId = arguments?.getString(ARG_PATH_ID, "")
        return pathId?.takeIfNotEmpty()?.let {
            viewModel.programConfig.programDetailUrlTemplate.replace("{$ARG_PATH_ID}", it)
        } ?: viewModel.programConfig.programUrl
    }

    companion object {
        private const val ARG_PATH_ID = "path_id"
        private const val ARG_NESTED_FRAGMENT = "nested_fragment"

        fun newInstance(
            pathId: String = "",
            isNestedFragment: Boolean = false,
        ): ProgramFragment {
            return ProgramFragment().apply {
                arguments = bundleOf(
                    ARG_PATH_ID to pathId,
                    ARG_NESTED_FRAGMENT to isNestedFragment
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProgramInfoScreen(
    windowSize: WindowSize,
    uiState: ProgramUIState?,
    contentUrl: String,
    cookieManager: AppCookieManager,
    uriScheme: String,
    userAgent: String,
    canShowBackBtn: Boolean,
    isNestedFragment: Boolean,
    hasInternetConnection: Boolean,
    onWebViewUIAction: (WebViewUIAction) -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onUriClick: (String, linkAuthority) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    when (uiState) {
        is ProgramUIState.UiMessage -> {
            HandleUIMessage(uiMessage = uiState.uiMessage, scaffoldState = scaffoldState)
        }

        else -> {}
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->
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

        val statusBarPadding = if (isNestedFragment) {
            Modifier
        } else {
            Modifier.statusBarsInset()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(statusBarPadding)
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isNestedFragment) {
                Toolbar(
                    label = stringResource(id = R.string.discovery_programs),
                    canShowBackBtn = canShowBackBtn,
                    canShowSettingsIcon = !canShowBackBtn,
                    onBackClick = onBackClick,
                    onSettingsClick = onSettingsClick
                )
            }

            Surface {
                Box(
                    modifier = modifierScreenWidth
                        .fillMaxHeight()
                        .background(Color.White),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if ((uiState is ProgramUIState.Error).not()) {
                        if (hasInternetConnection) {
                            val webView = CatalogWebViewScreen(
                                url = contentUrl,
                                uriScheme = uriScheme,
                                userAgent = userAgent,
                                isAllLinksExternal = true,
                                onWebPageLoaded = { onWebViewUIAction(WebViewUIAction.WEB_PAGE_LOADED) },
                                refreshSessionCookie = {
                                    coroutineScope.launch {
                                        cookieManager.tryToRefreshSessionCookie()
                                    }
                                },
                                onUriClick = onUriClick,
                                onWebPageLoadError = { onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR) }
                            )

                            AndroidView(
                                modifier = Modifier
                                    .background(MaterialTheme.appColors.background),
                                factory = {
                                    webView
                                },
                                update = {
                                    webView.loadUrl(contentUrl, coroutineScope, cookieManager)
                                }
                            )
                        } else {
                            onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR)
                        }
                    }

                    if (uiState is ProgramUIState.Error) {
                        FullScreenErrorView(errorType = uiState.errorType) {
                            onWebViewUIAction(WebViewUIAction.RELOAD_WEB_PAGE)
                        }
                    }

                    if (uiState == ProgramUIState.Loading && hasInternetConnection) {
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MyProgramsPreview() {
    OpenEdXTheme {
        ProgramInfoScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = ProgramUIState.Loading,
            contentUrl = "https://www.example.com/",
            cookieManager = koinViewModel<ProgramViewModel>().cookieManager,
            uriScheme = "",
            userAgent = "",
            canShowBackBtn = false,
            isNestedFragment = false,
            hasInternetConnection = false,
            onWebViewUIAction = {},
            onBackClick = {},
            onSettingsClick = {},
            onUriClick = { _, _ -> },
        )
    }
}
