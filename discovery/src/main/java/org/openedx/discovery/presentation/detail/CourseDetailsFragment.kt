package org.openedx.discovery.presentation.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.domain.model.Media
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.isPreview
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.EmailUtil
import org.openedx.discovery.R
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discovery.presentation.ui.ImageHeader
import org.openedx.discovery.presentation.ui.WarningLabel
import org.openedx.foundation.extension.applyDarkModeIfEnabled
import org.openedx.foundation.extension.isEmailValid
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import java.nio.charset.StandardCharsets
import java.util.Date
import org.openedx.core.R as CoreR

class CourseDetailsFragment : Fragment() {

    private val viewModel by viewModel<CourseDetailsViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<DiscoveryRouter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()

                val colorBackgroundValue = MaterialTheme.appColors.background.value
                val colorTextValue = MaterialTheme.appColors.textPrimary.value

                CourseDetailsScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    uiMessage = uiMessage,
                    apiHostUrl = viewModel.apiHostUrl,
                    htmlBody = viewModel.getCourseAboutBody(
                        colorBackgroundValue,
                        colorTextValue
                    ),
                    hasInternetConnection = viewModel.hasInternetConnection,
                    isUserLoggedIn = viewModel.isUserLoggedIn,
                    isRegistrationEnabled = viewModel.isRegistrationEnabled,
                    onReloadClick = {
                        viewModel.getCourseDetail()
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onButtonClick = {
                        val currentState = uiState
                        if (currentState is CourseDetailsUIState.CourseData) {
                            when {
                                (!currentState.isUserLoggedIn) -> {
                                    val dialog = AuthorizationDialogFragment.newInstance(
                                        viewModel.courseId
                                    )
                                    dialog.show(
                                        requireActivity().supportFragmentManager,
                                        AuthorizationDialogFragment::class.simpleName
                                    )
                                }

                                currentState.course.isEnrolled -> {
                                    router.navigateToCourseOutline(
                                        requireActivity().supportFragmentManager,
                                        currentState.course.courseId,
                                        currentState.course.name,
                                    )
                                }

                                else -> {
                                    viewModel.enrollInACourse(
                                        currentState.course.courseId,
                                        currentState.course.name
                                    )
                                }
                            }
                        }
                    },
                    onRegisterClick = {
                        router.navigateToSignUp(parentFragmentManager, viewModel.courseId, null)
                    },
                    onSignInClick = {
                        router.navigateToSignIn(parentFragmentManager, viewModel.courseId, null)
                    },
                )
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        fun newInstance(courseId: String): CourseDetailsFragment {
            val fragment = CourseDetailsFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CourseDetailsScreen(
    windowSize: WindowSize,
    uiState: CourseDetailsUIState,
    uiMessage: UIMessage?,
    apiHostUrl: String,
    htmlBody: String,
    hasInternetConnection: Boolean,
    isUserLoggedIn: Boolean,
    isRegistrationEnabled: Boolean,
    onReloadClick: () -> Unit,
    onBackClick: () -> Unit,
    onButtonClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current

    val isInternetConnectionShown = rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .semantics {
                testTagsAsResourceId = true
            },
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background,
        bottomBar = {
            if (!isUserLoggedIn) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)) {
                    AuthButtonsPanel(
                        onRegisterClick = onRegisterClick,
                        onSignInClick = onSignInClick,
                        showRegisterButton = isRegistrationEnabled
                    )
                }
            }
        }
    ) {
        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = if (configuration.orientation == ORIENTATION_PORTRAIT) {
                        Modifier.widthIn(Dp.Unspecified, 560.dp)
                    } else {
                        Modifier.widthIn(Dp.Unspecified, 650.dp)
                    },
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val webViewPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.padding(vertical = 24.dp),
                    compact = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                screenWidth
            ) {
                Toolbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    label = stringResource(id = R.string.discovery_course_details),
                    canShowBackBtn = true,
                    onBackClick = onBackClick
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.appColors.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (uiState) {
                        is CourseDetailsUIState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(it),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is CourseDetailsUIState.CourseData -> {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                if (configuration.orientation == ORIENTATION_LANDSCAPE && windowSize.isTablet) {
                                    CourseDetailNativeContentLandscape(
                                        windowSize = windowSize,
                                        apiHostUrl = apiHostUrl,
                                        hasInternetConnection = hasInternetConnection,
                                        isInternetConnectionShown = isInternetConnectionShown,
                                        course = uiState.course,
                                        onButtonClick = {
                                            onButtonClick()
                                        }
                                    )
                                } else {
                                    CourseDetailNativeContent(
                                        windowSize = windowSize,
                                        apiHostUrl = apiHostUrl,
                                        hasInternetConnection = hasInternetConnection,
                                        isInternetConnectionShown = isInternetConnectionShown,
                                        course = uiState.course,
                                        onButtonClick = {
                                            onButtonClick()
                                        }
                                    )
                                }
                                if (isPreview) {
                                    Text(
                                        text = htmlBody,
                                        modifier = Modifier
                                            .testTag("txt_course_description")
                                            .padding(all = 20.dp),
                                    )
                                } else {
                                    var webViewAlpha by remember { mutableFloatStateOf(0f) }
                                    if (webViewAlpha == 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .padding(top = 16.dp)
                                            .fillMaxWidth()
                                            .alpha(webViewAlpha),
                                        color = MaterialTheme.appColors.background
                                    ) {
                                        CourseDescription(
                                            modifier = webViewPadding,
                                            apiHostUrl = apiHostUrl,
                                            body = htmlBody,
                                            onWebPageLoaded = {
                                                webViewAlpha = 1f
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (!isInternetConnectionShown.value && !hasInternetConnection) {
                        OfflineModeDialog(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            onDismissCLick = {
                                isInternetConnectionShown.value = true
                            },
                            onReloadClick = {
                                isInternetConnectionShown.value = true
                                onReloadClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseDetailNativeContent(
    windowSize: WindowSize,
    apiHostUrl: String,
    course: Course,
    hasInternetConnection: Boolean,
    isInternetConnectionShown: MutableState<Boolean>,
    onButtonClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val buttonWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.width(230.dp),
                compact = Modifier.fillMaxWidth()
            )
        )
    }

    val contentHorizontalPadding by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = 6.dp,
                compact = 24.dp
            )
        )
    }

    val buttonText = if (course.isEnrolled) {
        stringResource(id = R.string.discovery_view_course)
    } else {
        stringResource(id = R.string.discovery_enroll_now)
    }

    Column {
        Box(contentAlignment = Alignment.Center) {
            ImageHeader(
                modifier = Modifier
                    .aspectRatio(ratio = 1.86f)
                    .padding(6.dp),
                apiHostUrl = apiHostUrl,
                courseImage = course.media.image?.large,
                courseName = course.name
            )
            if (!course.media.courseVideo?.uri.isNullOrEmpty()) {
                IconButton(
                    modifier = Modifier.testTag("ib_play_video"),
                    onClick = {
                        uriHandler.openUri(course.media.courseVideo?.uri!!)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(R.drawable.discovery_ic_play),
                        contentDescription = stringResource(id = R.string.discovery_accessibility_play_video),
                        tint = Color.LightGray
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = contentHorizontalPadding)
        ) {
            val enrollmentEnd = course.enrollmentEnd
            if (!hasInternetConnection) {
                isInternetConnectionShown.value = true
                NoInternetLabel()
                Spacer(Modifier.height(24.dp))
            } else if (enrollmentEnd != null && Date() > enrollmentEnd) {
                EnrollOverLabel()
                Spacer(Modifier.height(24.dp))
            }
            Text(
                modifier = Modifier.testTag("txt_course_short_description"),
                text = course.shortDescription,
                style = MaterialTheme.appTypography.labelSmall,
                color = MaterialTheme.appColors.textPrimaryVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.testTag("txt_course_name"),
                text = course.name,
                style = MaterialTheme.appTypography.titleLarge,
                color = MaterialTheme.appColors.textPrimary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                modifier = Modifier.testTag("txt_course_org"),
                text = course.org,
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textAccent
            )
            if (!(enrollmentEnd != null && Date() > enrollmentEnd)) {
                Spacer(Modifier.height(32.dp))
                OpenEdXButton(
                    modifier = buttonWidth,
                    text = buttonText,
                    onClick = onButtonClick
                )
            }
        }
    }
}

@Composable
private fun CourseDetailNativeContentLandscape(
    windowSize: WindowSize,
    apiHostUrl: String,
    course: Course,
    hasInternetConnection: Boolean,
    isInternetConnectionShown: MutableState<Boolean>,
    onButtonClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val buttonWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.width(230.dp),
                compact = Modifier.fillMaxWidth()
            )
        )
    }

    val buttonText = if (course.isEnrolled) {
        stringResource(id = R.string.discovery_view_course)
    } else {
        stringResource(id = R.string.discovery_enroll_now)
    }

    Row(
        Modifier.heightIn(200.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .weight(weight = 3f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    modifier = Modifier.testTag("txt_course_short_description"),
                    text = course.shortDescription,
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textPrimaryVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    modifier = Modifier.testTag("txt_course_name"),
                    text = course.name,
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    modifier = Modifier.testTag("txt_course_org"),
                    text = course.org,
                    style = MaterialTheme.appTypography.labelMedium,
                    color = MaterialTheme.appColors.textAccent
                )
                Spacer(Modifier.height(42.dp))
            }
            val enrollmentEnd = course.enrollmentEnd
            if (!hasInternetConnection) {
                isInternetConnectionShown.value = true
                NoInternetLabel()
                Spacer(Modifier.height(24.dp))
            } else if (enrollmentEnd != null && Date() > enrollmentEnd) {
                EnrollOverLabel()
                Spacer(Modifier.height(24.dp))
            } else {
                OpenEdXButton(
                    modifier = buttonWidth,
                    text = buttonText,
                    onClick = onButtonClick
                )
            }
        }
        Spacer(Modifier.width(24.dp))
        Box(contentAlignment = Alignment.Center) {
            ImageHeader(
                modifier = Modifier
                    .width(263.dp)
                    .height(200.dp),
                apiHostUrl = apiHostUrl,
                courseImage = course.media.image?.large,
                courseName = course.name
            )
            if (!course.media.courseVideo?.uri.isNullOrEmpty()) {
                IconButton(
                    modifier = Modifier.testTag("ib_play_video"),
                    onClick = {
                        uriHandler.openUri(course.media.courseVideo?.uri!!)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(R.drawable.discovery_ic_play),
                        contentDescription = null,
                        tint = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun EnrollOverLabel() {
    WarningLabel(
        painter = rememberVectorPainter(Icons.Outlined.Report),
        text = stringResource(id = R.string.discovery_you_cant_enroll)
    )
}

@Composable
private fun NoInternetLabel() {
    WarningLabel(
        painter = painterResource(id = CoreR.drawable.core_ic_offline),
        text = stringResource(id = R.string.discovery_no_internet_label)
    )
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun CourseDescription(
    modifier: Modifier,
    apiHostUrl: String,
    body: String,
    onWebPageLoaded: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    AndroidView(modifier = Modifier.then(modifier), factory = {
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    onWebPageLoaded()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val clickUrl = request?.url?.toString() ?: ""
                    return if (clickUrl.isNotEmpty() && clickUrl.startsWith("http")) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl))
                        )
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
            loadDataWithBaseURL(
                apiHostUrl,
                body,
                "text/html",
                StandardCharsets.UTF_8.name(),
                null
            )
            applyDarkModeIfEnabled(isDarkTheme)
        }
    })
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseDetailNativeContentPreview() {
    OpenEdXTheme {
        CourseDetailsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseDetailsUIState.CourseData(mockCourse),
            uiMessage = null,
            apiHostUrl = "http://localhost:8000",
            hasInternetConnection = false,
            isUserLoggedIn = true,
            isRegistrationEnabled = true,
            htmlBody = "<b>Preview text</b>",
            onReloadClick = {},
            onBackClick = {},
            onButtonClick = {},
            onRegisterClick = {},
            onSignInClick = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseDetailNativeContentTabletPreview() {
    OpenEdXTheme {
        CourseDetailsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseDetailsUIState.CourseData(mockCourse),
            uiMessage = null,
            apiHostUrl = "http://localhost:8000",
            hasInternetConnection = false,
            isUserLoggedIn = true,
            isRegistrationEnabled = true,
            htmlBody = "<b>Preview text</b>",
            onReloadClick = {},
            onBackClick = {},
            onButtonClick = {},
            onRegisterClick = {},
            onSignInClick = {},
        )
    }
}

private val mockCourse = Course(
    id = "id",
    blocksUrl = "blocksUrl",
    courseId = "courseId",
    effort = "effort",
    enrollmentStart = null,
    enrollmentEnd = null,
    hidden = false,
    invitationOnly = false,
    media = Media(),
    mobileAvailable = true,
    name = "Test course",
    number = "number",
    org = "EdX",
    pacing = "pacing",
    shortDescription = "shortDescription",
    start = "start",
    end = "end",
    startDisplay = "startDisplay",
    startType = "startType",
    overview = "",
    isEnrolled = false
)
