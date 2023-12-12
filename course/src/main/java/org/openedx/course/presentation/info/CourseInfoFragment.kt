package org.openedx.course.presentation.info

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.UIMessage
import org.openedx.core.WebViewLink
import org.openedx.core.WebViewLink.Authority.COURSE_INFO
import org.openedx.core.WebViewLink.Authority.ENROLL
import org.openedx.core.config.Config
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.dialog.alert.InfoDialogFragment
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.DefaultWebViewClient
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.R
import org.openedx.course.presentation.CourseRouter
import org.openedx.core.R as CoreR

class CourseInfoFragment : Fragment() {

    private val viewModel by viewModel<CourseInfoViewModel>()
    private val edxCookieManager by inject<AppCookieManager>()
    private val networkConnection by inject<NetworkConnection>()
    private val router: CourseRouter by inject()
    private val config by inject<Config>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val uiMessage by viewModel.uiMessage.observeAsState()
                val showAlert by viewModel.showAlert.observeAsState()
                val enrollmentSuccess by viewModel.courseEnrollSuccess.observeAsState()

                val windowSize = rememberWindowSize()

                var isLoading by remember {
                    mutableStateOf(true)
                }

                var hasInternetConnection by remember {
                    mutableStateOf(networkConnection.isOnline())
                }

                LaunchedEffect(showAlert) {
                    if (showAlert == true) {
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
                    if (enrollmentSuccess?.isNotEmpty() == true) {
                        router.navigateToCourseOutline(
                            requireActivity().supportFragmentManager,
                            courseId = enrollmentSuccess!!,
                            courseTitle = ""
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
                                cookieManager = edxCookieManager,
                                onWebPageLoaded = {
                                    isLoading = false
                                },
                                onBackClick = {
                                    requireActivity().supportFragmentManager.popBackStackImmediate()
                                },
                                onEnrollClick = { courseId ->
                                    viewModel.enrollInACourse(courseId!!)
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

    private fun getInitialUrl(): String {
        return arguments?.let { args ->
            val pathId = args.getString(ARG_PATH_ID) ?: ""
            val config = config.getDiscoveryConfig().webViewConfig
            val urlTemplate = if (args.getString(ARG_INFO_TYPE) == COURSE_INFO.name) {
                config.courseUrlTemplate
            } else {
                config.programUrlTemplate
            }
            urlTemplate.replace("{$ARG_PATH_ID}", pathId)
        } ?: config.getDiscoveryConfig().webViewConfig.baseUrl
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
    cookieManager: AppCookieManager,
    onWebPageLoaded: () -> Unit,
    onBackClick: () -> Unit,
    onEnrollClick: (String?) -> Unit,
    openExternalLink: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current

    HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

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
                    BackBtn {
                        onBackClick()
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        text = stringResource(R.string.course_discover),
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
                        WebView(context).apply {
                            webViewClient = object : DefaultWebViewClient(
                                context = context,
                                webView = this@apply,
                                coroutineScope = coroutineScope,
                                cookieManager = cookieManager,
                                allLinksExternal = true,
                                openExternalLink = openExternalLink,
                            ) {

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    super.onPageCommitVisible(view, url)
                                    onWebPageLoaded()
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val clickUrl = request?.url?.toString() ?: ""
                                    if (handleRecognizedLink(clickUrl)) {
                                        return true
                                    }

                                    return super.shouldOverrideUrlLoading(view, request)
                                }

                                private fun handleRecognizedLink(clickUrl: String): Boolean {
                                    val link = WebViewLink.parse(clickUrl) ?: return false

                                    return when (link.authority) {
                                        ENROLL -> {
                                            val courseId = link.params[WebViewLink.Param.COURSE_ID]
                                            onEnrollClick(courseId)
                                            true
                                        }

                                        else -> false
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

                            loadUrl(url)
                        }
                    }
                )
            }
        }
    }
}
