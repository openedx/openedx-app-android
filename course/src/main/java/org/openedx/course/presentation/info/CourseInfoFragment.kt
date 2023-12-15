package org.openedx.course.presentation.info

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.UIMessage
import org.openedx.core.presentation.catalog.CatalogWebViewScreen
import org.openedx.core.presentation.catalog.WebViewLink.Authority.COURSE_INFO
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.dialog.alert.InfoDialogFragment
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.ToolbarWithBackBtn
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.R
import org.openedx.core.R as CoreR

class CourseInfoFragment : Fragment() {

    private val viewModel by viewModel<CourseInfoViewModel>()

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
                val enrollmentSuccess by viewModel.courseEnrollSuccess.collectAsState(initial = "")

                val windowSize = rememberWindowSize()

                var isLoading by remember {
                    mutableStateOf(true)
                }

                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.hasInternetConnection)
                }

                LaunchedEffect(showAlert) {
                    if (showAlert) {
                        InfoDialogFragment.newInstance(
                            title = context.getString(R.string.course_enrollment_error),
                            message = context.getString(
                                R.string.course_enrollment_error_message,
                                getString(CoreR.string.platform_name)
                            )
                        ).show(
                            requireActivity().supportFragmentManager,
                            InfoDialogFragment::class.simpleName
                        )
                    }
                }

                LaunchedEffect(enrollmentSuccess) {
                    if (enrollmentSuccess.isNotEmpty()) {
                        viewModel.onSuccessfulCourseEnrollment(
                            fragmentManager = requireActivity().supportFragmentManager,
                            courseId = enrollmentSuccess,
                        )
                    }
                }

                Surface {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (hasInternetConnection) {
                            CourseInfoScreen(
                                windowSize = windowSize,
                                uiMessage = uiMessage,
                                url = getInitialUrl(),
                                onWebPageLoaded = {
                                    isLoading = false
                                },
                                onBackClick = {
                                    requireActivity().supportFragmentManager.popBackStackImmediate()
                                },
                                onEnrollClick = { courseId ->
                                    viewModel.enrollInACourse(courseId)
                                },
                                refreshSessionCookie = {
                                    viewModel.tryToRefreshSessionCookie()
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
                                onInfoCardClicked = { pathId, infoType ->
                                    viewModel.infoCardClicked(
                                        fragmentManager = requireActivity().supportFragmentManager,
                                        pathId = pathId,
                                        infoType = infoType
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
                                hasInternetConnection = viewModel.hasInternetConnection
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

    private fun getInitialUrl(): String {
        return arguments?.let { args ->
            val pathId = args.getString(ARG_PATH_ID) ?: ""
            val urlTemplate = if (args.getString(ARG_INFO_TYPE) == COURSE_INFO.name) {
                viewModel.webViewConfig.courseUrlTemplate
            } else {
                viewModel.webViewConfig.programUrlTemplate
            }
            urlTemplate.replace("{$ARG_PATH_ID}", pathId)
        } ?: viewModel.webViewConfig.baseUrl
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
@SuppressLint("SetJavaScriptEnabled")
private fun CourseInfoScreen(
    windowSize: WindowSize,
    uiMessage: UIMessage?,
    url: String,
    onWebPageLoaded: () -> Unit,
    onBackClick: () -> Unit,
    onEnrollClick: (String) -> Unit,
    refreshSessionCookie: () -> Unit,
    onInfoCardClicked: (String, String) -> Unit,
    openExternalLink: (String) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current

    HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
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

        val webView = CatalogWebViewScreen(
            url = url,
            isAllLinksExternal = true,
            onWebPageLoaded = onWebPageLoaded,
            openExternalLink = openExternalLink,
            onEnrollClick = onEnrollClick,
            onInfoCardClicked = onInfoCardClicked,
            refreshSessionCookie = refreshSessionCookie,
        )

        Column(
            modifier = modifierScreenWidth
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ToolbarWithBackBtn(
                label = stringResource(id = R.string.course_discover),
                onBackClick = onBackClick
            )

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
