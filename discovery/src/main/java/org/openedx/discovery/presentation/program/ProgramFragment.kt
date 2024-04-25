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
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.extension.toastMessage
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.dialog.alert.InfoDialogFragment
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.windowSizeValue
import org.openedx.discovery.R
import org.openedx.discovery.presentation.DiscoveryAnalyticsScreen
import org.openedx.discovery.presentation.catalog.CatalogWebViewScreen
import org.openedx.discovery.presentation.catalog.WebViewLink
import org.openedx.core.R as coreR
import org.openedx.discovery.presentation.catalog.WebViewLink.Authority as linkAuthority

class ProgramFragment(private val myPrograms: Boolean = false) : Fragment() {

    private val viewModel by viewModel<ProgramViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (myPrograms.not()) {
            lifecycle.addObserver(viewModel)
        }
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
                val uiState by viewModel.uiState.collectAsState(initial = ProgramUIState.Loading)
                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.hasInternetConnection)
                }

                if (myPrograms.not()) {
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
                    canShowBackBtn = arguments?.getString(ARG_PATH_ID, "")
                        ?.isNotEmpty() == true,
                    uriScheme = viewModel.uriScheme,
                    hasInternetConnection = hasInternetConnection,
                    checkInternetConnection = {
                        hasInternetConnection = viewModel.hasInternetConnection
                    },
                    onWebPageLoaded = { viewModel.showLoading(false) },
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
                    },
                    refreshSessionCookie = {
                        viewModel.refreshCookie()
                    },
                )
            }
        }
    }


    private fun getInitialUrl(): String {
        return arguments?.let { args ->
            val pathId = args.getString(ARG_PATH_ID) ?: ""
            viewModel.programConfig.programDetailUrlTemplate.replace("{$ARG_PATH_ID}", pathId)
        } ?: viewModel.programConfig.programUrl
    }

    companion object {
        private const val ARG_PATH_ID = "path_id"

        fun newInstance(
            pathId: String,
        ): ProgramFragment {
            val fragment = ProgramFragment(false)
            fragment.arguments = bundleOf(
                ARG_PATH_ID to pathId,
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProgramInfoScreen(
    windowSize: WindowSize,
    uiState: ProgramUIState?,
    contentUrl: String,
    uriScheme: String,
    canShowBackBtn: Boolean,
    hasInternetConnection: Boolean,
    checkInternetConnection: () -> Unit,
    onWebPageLoaded: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onUriClick: (String, WebViewLink.Authority) -> Unit,
    refreshSessionCookie: () -> Unit = {},
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current
    val isLoading = uiState is ProgramUIState.Loading

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
                label = stringResource(id = R.string.discovery_programs),
                canShowBackBtn = canShowBackBtn,
                canShowSettingsIcon = !canShowBackBtn,
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
                    if (hasInternetConnection) {
                        val webView = CatalogWebViewScreen(
                            url = contentUrl,
                            uriScheme = uriScheme,
                            isAllLinksExternal = true,
                            onWebPageLoaded = onWebPageLoaded,
                            refreshSessionCookie = refreshSessionCookie,
                            onUriClick = onUriClick,
                        )

                        AndroidView(
                            modifier = Modifier
                                .background(MaterialTheme.appColors.background),
                            factory = {
                                webView
                            },
                            update = {
                                webView.loadUrl(contentUrl)
                            }
                        )
                    } else {
                        ConnectionErrorView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(MaterialTheme.appColors.background)
                        ) {
                            checkInternetConnection()
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MyProgramsPreview() {
    OpenEdXTheme {
        ProgramInfoScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = ProgramUIState.Loading,
            contentUrl = "https://www.example.com/",
            uriScheme = "",
            canShowBackBtn = false,
            hasInternetConnection = false,
            checkInternetConnection = {},
            onBackClick = {},
            onWebPageLoaded = {},
            onSettingsClick = {},
            onUriClick = { _, _ -> },
        )
    }
}
