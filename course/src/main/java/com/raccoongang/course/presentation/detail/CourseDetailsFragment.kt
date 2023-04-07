package com.raccoongang.course.presentation.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Report
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.raccoongang.core.BuildConfig
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.domain.model.Media
import com.raccoongang.core.extension.isEmailValid
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.core.utils.EmailUtil
import com.raccoongang.course.R
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.course.presentation.ui.CourseImageHeader
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.nio.charset.StandardCharsets
import java.util.*
import com.raccoongang.course.R as courseR

class CourseDetailsFragment : Fragment() {

    private val viewModel by viewModel<CourseDetailsViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()

                val colorBackgroundValue = MaterialTheme.appColors.background.value
                val colorTextValue = MaterialTheme.appColors.textPrimary.value

                CourseDetailsScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    uiMessage = uiMessage,
                    htmlBody = viewModel.getCourseAboutBody(
                        colorBackgroundValue,
                        colorTextValue
                    ),
                    hasInternetConnection = viewModel.hasInternetConnection,
                    onReloadClick = {
                        viewModel.getCourseDetail()
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onButtonClick = {
                        val currentState = uiState
                        if (currentState is CourseDetailsUIState.CourseData) {
                            if (currentState.course.isEnrolled) {
                                router.navigateToCourseOutline(
                                    requireActivity().supportFragmentManager,
                                    currentState.course.courseId,
                                    currentState.course.name
                                )
                            } else {
                                viewModel.enrollInACourse(currentState.course.courseId)
                            }
                        }
                    })
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


@Composable
internal fun CourseDetailsScreen(
    windowSize: WindowSize,
    uiState: CourseDetailsUIState,
    uiMessage: UIMessage?,
    htmlBody: String,
    hasInternetConnection: Boolean,
    onReloadClick: () -> Unit,
    onBackClick: () -> Unit,
    onButtonClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
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
                .statusBarsInset(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                screenWidth
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
                        text = stringResource(id = courseR.string.course_details),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .padding(it)
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
                                        course = uiState.course,
                                        onButtonClick = {
                                            onButtonClick()
                                        }
                                    )
                                } else {
                                    CourseDetailNativeContent(
                                        windowSize = windowSize,
                                        course = uiState.course,
                                        onButtonClick = {
                                            onButtonClick()
                                        }
                                    )
                                }
                                if (isPreview) {
                                    Text(htmlBody, Modifier.padding(all = 20.dp))
                                } else {
                                    var webViewAlpha by remember { mutableStateOf(0f) }
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
                                            paddingModifier = webViewPadding,
                                            body = htmlBody,
                                            onWebPageLoaded = {
                                                webViewAlpha = 1f
                                            })
                                    }
                                }
                            }
                        }
                    }
                    if (!isInternetConnectionShown && !hasInternetConnection) {
                        OfflineModeDialog(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            onDismissCLick = {
                                isInternetConnectionShown = true
                            },
                            onReloadClick = {
                                isInternetConnectionShown = true
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
    course: Course,
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
        stringResource(id = R.string.course_view_course)
    } else {
        stringResource(id = R.string.course_enroll_now)
    }

    Column {
        Box(contentAlignment = Alignment.Center) {
            CourseImageHeader(
                modifier = Modifier
                    .aspectRatio(1.86f)
                    .padding(6.dp),
                courseImage = course.media.image?.large,
                courseCertificate = null
            )
            if (!course.media.courseVideo?.uri.isNullOrEmpty()) {
                IconButton(
                    onClick = {
                        uriHandler.openUri(course.media.courseVideo?.uri!!)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(64.dp),
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = null,
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
            if (enrollmentEnd != null && Date() > enrollmentEnd) {
                EnrollOverLabel()
                Spacer(Modifier.height(24.dp))
            }
            Text(
                text = course.shortDescription,
                style = MaterialTheme.appTypography.labelSmall,
                color = MaterialTheme.appColors.textPrimaryVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = course.name,
                style = MaterialTheme.appTypography.titleLarge,
                color = MaterialTheme.appColors.textPrimary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = course.org,
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textAccent
            )
            if (!(enrollmentEnd != null && Date() > enrollmentEnd)) {
                Spacer(Modifier.height(32.dp))
                NewEdxButton(
                    width = buttonWidth,
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
    course: Course,
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
        stringResource(id = R.string.course_view_course)
    } else {
        stringResource(id = R.string.course_enroll_now)
    }

    Row(
        Modifier.heightIn(200.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .weight(3f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = course.shortDescription,
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textPrimaryVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = course.name,
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = course.org,
                    style = MaterialTheme.appTypography.labelMedium,
                    color = MaterialTheme.appColors.textAccent
                )
                Spacer(Modifier.height(42.dp))
            }
            val enrollmentEnd = course.enrollmentEnd
            if (enrollmentEnd != null && Date() > enrollmentEnd) {
                Spacer(Modifier.height(4.dp))
                EnrollOverLabel()
            } else {
                NewEdxButton(
                    width = buttonWidth,
                    text = buttonText,
                    onClick = onButtonClick
                )
            }
        }
        Spacer(Modifier.width(24.dp))
        Box(contentAlignment = Alignment.Center) {
            CourseImageHeader(
                modifier = Modifier
                    .width(263.dp)
                    .height(200.dp),
                courseImage = course.media.image?.large,
                courseCertificate = null
            )
            if (!course.media.courseVideo?.uri.isNullOrEmpty()) {
                IconButton(
                    onClick = {
                        uriHandler.openUri(course.media.courseVideo?.uri!!)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(64.dp),
                        imageVector = Icons.Filled.PlayCircle,
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
    val borderColor = if (!isSystemInDarkTheme()) {
        MaterialTheme.appColors.cardViewBorder
    } else {
        MaterialTheme.appColors.surface
    }
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(
                0.dp,
                MaterialTheme.appShapes.material.medium
            )
            .background(
                MaterialTheme.appColors.surface,
                MaterialTheme.appShapes.material.medium
            )
            .border(
                1.dp,
                borderColor,
                MaterialTheme.appShapes.material.medium
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Report,
                contentDescription = null,
                tint = MaterialTheme.appColors.warning
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(id = courseR.string.course_you_cant_enroll),
                color = MaterialTheme.appColors.textPrimaryVariant,
                style = MaterialTheme.appTypography.titleSmall
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun CourseDescription(
    paddingModifier: Modifier,
    body: String,
    onWebPageLoaded: () -> Unit
) {
    val context = LocalContext.current
    AndroidView(modifier = Modifier.then(paddingModifier), factory = {
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
                BuildConfig.BASE_URL, body, "text/html", StandardCharsets.UTF_8.name(), null
            )
        }
    })
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseDetailNativeContentPreview() {
    NewEdxTheme {
        CourseDetailsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseDetailsUIState.CourseData(mockCourse),
            uiMessage = null,
            hasInternetConnection = false,
            htmlBody = "<b>Preview text</b>",
            onReloadClick = {},
            onBackClick = {},
            onButtonClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseDetailNativeContentTabletPreview() {
    NewEdxTheme {
        CourseDetailsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseDetailsUIState.CourseData(mockCourse),
            uiMessage = null,
            hasInternetConnection = false,
            htmlBody = "<b>Preview text</b>",
            onReloadClick = {},
            onBackClick = {},
            onButtonClick = {}
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
