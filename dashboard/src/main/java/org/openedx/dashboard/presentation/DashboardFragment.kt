package org.openedx.dashboard.presentation

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.AppUpdateState
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Certificate
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrolledCourseData
import org.openedx.core.presentation.global.app_upgrade.AppUpgradeRecommendedBox
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.core.utils.TimeUtils
import org.openedx.dashboard.R
import java.util.Date
import org.openedx.core.R as CoreR

class DashboardFragment : Fragment() {

    private val viewModel by viewModel<DashboardViewModel>()
    private val router by inject<DashboardRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
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
                val uiState by viewModel.uiState.observeAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.updating.observeAsState(false)
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val appUpgradeEvent by viewModel.appUpgradeEvent.observeAsState()

                MyCoursesScreen(
                    windowSize = windowSize,
                    viewModel.apiHostUrl,
                    uiState!!,
                    uiMessage,
                    canLoadMore = canLoadMore,
                    refreshing = refreshing,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    onReloadClick = {
                        viewModel.getCourses()
                    },
                    onItemClick = {
                        viewModel.dashboardCourseClickedEvent(it.course.id, it.course.name)
                        router.navigateToCourseOutline(
                            requireParentFragment().parentFragmentManager,
                            it.course.id,
                            it.course.name,
                            it.mode
                        )
                    },
                    onSwipeRefresh = {
                        viewModel.updateCourses()
                    },
                    paginationCallback = {
                        viewModel.fetchMore()
                    },
                    appUpgradeParameters = AppUpdateState.AppUpgradeParameters(
                        appUpgradeEvent = appUpgradeEvent,
                        onAppUpgradeRecommendedBoxClick = {
                            AppUpdateState.openPlayMarket(requireContext())
                        },
                    ),
                    onSettingsClick = {
                        router.navigateToSettings(requireActivity().supportFragmentManager)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun MyCoursesScreen(
    windowSize: WindowSize,
    apiHostUrl: String,
    state: DashboardUIState,
    uiMessage: UIMessage?,
    canLoadMore: Boolean,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    onReloadClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    paginationCallback: () -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (EnrolledCourse) -> Unit,
    appUpgradeParameters: AppUpdateState.AppUpgradeParameters,
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableStateOf(scrollState.firstVisibleItemIndex)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->

        val contentPaddings by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(
                        top = 32.dp,
                        bottom = 40.dp
                    ),
                    compact = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
                )
            )
        }

        val emptyStatePaddings by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.padding(
                        top = 32.dp,
                        bottom = 40.dp
                    ),
                    compact = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                )
            )
        }

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth(),
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Toolbar(
                label = stringResource(id = R.string.dashboard_title),
                canShowSettingsIcon = true,
                onSettingsClick = onSettingsClick
            )

            Surface(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.screenBackgroundShape
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .pullRefresh(pullRefreshState),
                ) {
                    when (state) {
                        is DashboardUIState.Loading -> {
                            Box(
                                Modifier
                                    .fillMaxSize(), contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is DashboardUIState.Courses -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .then(contentWidth),
                                    state = scrollState,
                                    contentPadding = contentPaddings,
                                    content = {
                                        item() {
                                            Column {
                                                Header()
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }
                                        items(state.courses) { course ->
                                            CourseItem(
                                                apiHostUrl,
                                                course,
                                                windowSize,
                                                onClick = { onItemClick(it) })
                                            Divider()
                                        }
                                        item {
                                            if (canLoadMore) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                                }
                                            }
                                        }
                                    })
                                if (scrollState.shouldLoadMore(firstVisibleIndex, 4)) {
                                    paginationCallback()
                                }
                            }
                        }

                        is DashboardUIState.Empty -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxHeight()
                                        .then(contentWidth)
                                        .then(emptyStatePaddings)
                                ) {
                                    Header()
                                    EmptyState()
                                }
                            }
                        }
                    }
                    PullRefreshIndicator(
                        refreshing,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        when (appUpgradeParameters.appUpgradeEvent) {
                            is AppUpgradeEvent.UpgradeRecommendedEvent -> {
                                AppUpgradeRecommendedBox(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = appUpgradeParameters.onAppUpgradeRecommendedBoxClick
                                )
                            }

                            else -> {}
                        }

                        if (!isInternetConnectionShown && !hasInternetConnection) {
                            OfflineModeDialog(
                                Modifier
                                    .fillMaxWidth(),
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
}

@Composable
private fun CourseItem(
    apiHostUrl: String,
    enrolledCourse: EnrolledCourse,
    windowSize: WindowSize,
    onClick: (EnrolledCourse) -> Unit
) {
    val imageWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = 170.dp,
                compact = 105.dp
            )
        )
    }
    val imageUrl = apiHostUrl.dropLast(1) + enrolledCourse.course.courseImage
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .testTag("btn_course_item")
            .height(142.dp)
            .fillMaxWidth()
            .clickable { onClick(enrolledCourse) }
            .background(MaterialTheme.appColors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.appColors.background),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .error(CoreR.drawable.core_no_image_course)
                    .placeholder(CoreR.drawable.core_no_image_course)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(105.dp)
                    .width(imageWidth)
                    .clip(MaterialTheme.appShapes.courseImageShape)
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .background(MaterialTheme.appColors.background)
            ) {
                Text(
                    modifier = Modifier.testTag("txt_course_org"),
                    text = enrolledCourse.course.org,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.appColors.background),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.testTag("txt_course_name"),
                        text = enrolledCourse.course.name,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.appColors.background),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.testTag("txt_course_date"),
                            text = TimeUtils.getCourseFormattedDate(
                                context,
                                Date(),
                                enrolledCourse.auditAccessExpires,
                                enrolledCourse.course.start,
                                enrolledCourse.course.end,
                                enrolledCourse.course.startType,
                                enrolledCourse.course.startDisplay
                            ),
                            color = MaterialTheme.appColors.textFieldHint,
                            style = MaterialTheme.appTypography.labelMedium
                        )
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier
                                    .testTag("ic_course_item")
                                    .size(15.dp),
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.appColors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Text(
        modifier = Modifier.testTag("txt_courses_title"),
        text = stringResource(id = R.string.dashboard_courses),
        color = MaterialTheme.appColors.textPrimary,
        style = MaterialTheme.appTypography.displaySmall
    )
    Text(
        modifier = Modifier
            .testTag("txt_courses_description")
            .padding(top = 4.dp),
        text = stringResource(id = R.string.dashboard_welcome_back),
        color = MaterialTheme.appColors.textPrimaryVariant,
        style = MaterialTheme.appTypography.titleSmall
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(185.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.dashboard_ic_empty),
                contentDescription = null,
                tint = MaterialTheme.appColors.textFieldBorder
            )
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_description")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.dashboard_you_are_not_enrolled),
                color = MaterialTheme.appColors.textPrimaryVariant,
                style = MaterialTheme.appTypography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseItemPreview() {
    OpenEdXTheme() {
        CourseItem(
            "http://localhost:8000",
            mockCourseEnrolled,
            WindowSize(WindowType.Compact, WindowType.Compact),
            onClick = {})
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MyCoursesScreenDay() {
    OpenEdXTheme {
        MyCoursesScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            apiHostUrl = "http://localhost:8000",
            state = DashboardUIState.Courses(
                listOf(
                    mockCourseEnrolled,
                    mockCourseEnrolled,
                    mockCourseEnrolled,
                    mockCourseEnrolled,
                    mockCourseEnrolled,
                    mockCourseEnrolled
                )
            ),
            uiMessage = null,
            onSwipeRefresh = {},
            onItemClick = {},
            onReloadClick = {},
            hasInternetConnection = true,
            refreshing = false,
            canLoadMore = false,
            paginationCallback = {},
            onSettingsClick = {},
            appUpgradeParameters = AppUpdateState.AppUpgradeParameters()
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun MyCoursesScreenTabletPreview() {
    OpenEdXTheme {
        MyCoursesScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            apiHostUrl = "http://localhost:8000",
            state = DashboardUIState.Courses(
                listOf(
                    mockCourseEnrolled,
                    mockCourseEnrolled,
                    mockCourseEnrolled,
                    mockCourseEnrolled,
                    mockCourseEnrolled,
                    mockCourseEnrolled
                )
            ),
            uiMessage = null,
            onSwipeRefresh = {},
            onItemClick = {},
            onReloadClick = {},
            hasInternetConnection = true,
            refreshing = false,
            canLoadMore = false,
            paginationCallback = {},
            onSettingsClick = {},
            appUpgradeParameters = AppUpdateState.AppUpgradeParameters()
        )
    }
}

private val mockCourseEnrolled = EnrolledCourse(
    auditAccessExpires = Date(),
    created = "created",
    certificate = Certificate(""),
    mode = "mode",
    isActive = true,
    course = EnrolledCourseData(
        id = "id",
        name = "name",
        number = "",
        org = "Org",
        start = Date(),
        startDisplay = "",
        startType = "",
        end = Date(),
        dynamicUpgradeDeadline = "",
        subscriptionId = "",
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            ""
        ),
        media = null,
        courseImage = "",
        courseAbout = "",
        courseSharingUtmParameters = CourseSharingUtmParameters("", ""),
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        videoOutline = "",
        isSelfPaced = false
    )
)
