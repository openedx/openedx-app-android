package org.openedx.courses.presentation

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.openedx.core.CourseContainerTabEntity
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Certificate
import org.openedx.core.domain.model.CourseAssignments
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CourseStatus
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrolledCourseData
import org.openedx.core.domain.model.Progress
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
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
import org.openedx.dashboard.presentation.DashboardRouter
import java.util.Date

class AllEnrolledCoursesFragment : Fragment() {

    private val viewModel by viewModel<AllEnrolledCoursesViewModel>()
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
                val uiState by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.collectAsState(null)
                val refreshing by viewModel.updating.collectAsState(false)
                val canLoadMore by viewModel.canLoadMore.collectAsState(false)

                AllEnrolledCoursesScreen(
                    windowSize = windowSize,
                    viewModel.apiHostUrl,
                    uiState,
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
                            requireActivity().supportFragmentManager,
                            it.course.id,
                            it.course.name,
                            it.mode,
                            CourseContainerTabEntity.COURSE
                        )
                    },
                    onSwipeRefresh = {
                        viewModel.updateCourses()
                    },
                    paginationCallback = {
                        viewModel.fetchMore()
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onSearchClick = {
                        router.navigateToCourseSearch(
                            requireActivity().supportFragmentManager, ""
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun AllEnrolledCoursesScreen(
    windowSize: WindowSize,
    apiHostUrl: String,
    state: AllEnrolledCoursesUIState,
    uiMessage: UIMessage?,
    canLoadMore: Boolean,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    onReloadClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    paginationCallback: () -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onItemClick: (EnrolledCourse) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { onSwipeRefresh() }
    )

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }
    val scrollState = rememberLazyGridState()
    val firstVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
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
                    compact = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
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
            BackBtn(
                modifier = Modifier.align(Alignment.Start),
                tint = MaterialTheme.appColors.textDark
            ) {
                onBackClick()
            }

            Surface(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.screenBackgroundShape
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .pullRefresh(pullRefreshState),
                ) {
                    when (state) {
                        is AllEnrolledCoursesUIState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is AllEnrolledCoursesUIState.Courses -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.padding(contentPaddings)
                                ) {
                                    Header(onSearchClick = onSearchClick)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyVerticalGrid(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .then(contentWidth),
                                        state = scrollState,
                                        columns = GridCells.Fixed(2),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        content = {
                                            items(state.courses) { course ->
                                                CourseItem(
                                                    course,
                                                    apiHostUrl,
                                                    onClick = { onItemClick(it) }
                                                )
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
                                        }
                                    )
                                }
                                if (scrollState.shouldLoadMore(firstVisibleIndex, 4)) {
                                    paginationCallback()
                                }
                            }
                        }

                        is AllEnrolledCoursesUIState.Empty -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .then(contentWidth)
                                        .then(emptyStatePaddings)
                                ) {
                                    Header(onSearchClick = onSearchClick)
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
private fun CourseItem(
    course: EnrolledCourse,
    apiHostUrl: String,
    onClick: (EnrolledCourse) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable {
                onClick(course)
            },
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 2.dp
    ) {
        Box {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(apiHostUrl + course.course.courseImage)
                        .error(org.openedx.core.R.drawable.core_no_image_course)
                        .placeholder(org.openedx.core.R.drawable.core_no_image_course)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    progress = course.progress.numPointsEarned.toFloat(),
                    color = MaterialTheme.appColors.primary,
                    backgroundColor = MaterialTheme.appColors.divider
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 4.dp),
                    style = MaterialTheme.appTypography.labelMedium,
                    color = MaterialTheme.appColors.textFieldHint,
                    text = stringResource(
                        R.string.dashboard_course_date,
                        TimeUtils.getCourseFormattedDate(
                            LocalContext.current,
                            Date(),
                            course.auditAccessExpires,
                            course.course.start,
                            course.course.end,
                            course.course.startType,
                            course.course.startDisplay
                        )
                    )
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    text = course.course.name,
                    style = MaterialTheme.appTypography.titleSmall,
                    color = MaterialTheme.appColors.textDark,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2,
                    maxLines = 2
                )
            }
            if (!course.course.coursewareAccess?.errorCode.isNullOrEmpty()) {
                Icon(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(top = 8.dp, end = 8.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .align(Alignment.TopEnd),
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textWarning
                )
            }
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = stringResource(id = R.string.dashboard_all_courses),
            color = MaterialTheme.appColors.textDark,
            style = MaterialTheme.appTypography.headlineBolt
        )
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 12.dp),
            onClick = {
                onSearchClick()
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.appColors.textDark
            )
        }
    }
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
            mockCourseEnrolled,
            "http://localhost:8000",
            onClick = {})
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AllEnrolledCoursesPreview() {
    OpenEdXTheme {
        AllEnrolledCoursesScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            apiHostUrl = "http://localhost:8000",
            state = AllEnrolledCoursesUIState.Courses(
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
            onBackClick = {},
            onSearchClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun AllEnrolledCoursesTabletPreview() {
    OpenEdXTheme {
        AllEnrolledCoursesScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            apiHostUrl = "http://localhost:8000",
            state = AllEnrolledCoursesUIState.Courses(
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
            onBackClick = {},
            onSearchClick = {}
        )
    }
}

private val mockCourseAssignments = CourseAssignments(null, emptyList())
private val mockCourseEnrolled = EnrolledCourse(
    auditAccessExpires = Date(),
    created = "created",
    certificate = Certificate(""),
    mode = "mode",
    isActive = true,
    progress = Progress.DEFAULT_PROGRESS,
    courseStatus = CourseStatus("", emptyList(), "", ""),
    courseAssignments = mockCourseAssignments,
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
