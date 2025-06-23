package org.openedx.course.presentation.container

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.domain.model.CourseAccessError
import org.openedx.core.extension.isFalse
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.RoundTabsBar
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseContainerBinding
import org.openedx.course.presentation.dates.CourseDatesScreen
import org.openedx.course.presentation.handouts.HandoutsScreen
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.course.presentation.offline.CourseOfflineScreen
import org.openedx.course.presentation.outline.CourseOutlineScreen
import org.openedx.course.presentation.progress.CourseProgressScreen
import org.openedx.course.presentation.ui.CourseVideosScreen
import org.openedx.course.presentation.ui.DatesShiftedSnackBar
import org.openedx.discussion.presentation.topics.DiscussionTopicsScreen
import org.openedx.foundation.extension.takeIfNotEmpty
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.rememberWindowSize
import java.util.Date

class CourseContainerFragment : Fragment(R.layout.fragment_course_container) {

    private val binding by viewBinding(FragmentCourseContainerBinding::bind)

    private val viewModel by viewModel<CourseContainerViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TITLE, ""),
            requireArguments().getString(ARG_RESUME_BLOCK, "")
        )
    }

    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(CourseContainerFragment::class.java.simpleName, "Permission granted: $granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.fetchCourseDetails()
    }

    private var snackBar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCourseView()
        if (viewModel.calendarSyncUIState.value.isCalendarSyncEnabled) {
            setUpCourseCalendar()
        }
        observe()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.courseAccessStatus.value == CourseAccessError.NONE) {
            viewModel.updateData()
        }
    }

    override fun onDestroyView() {
        snackBar?.dismiss()
        super.onDestroyView()
    }

    private fun observe() {
        viewModel.dataReady.observe(viewLifecycleOwner) { isReady ->
            if (isReady.isFalse()) {
                viewModel.courseRouter.navigateToNoAccess(
                    requireActivity().supportFragmentManager,
                    viewModel.courseName
                )
            } else {
                if (viewModel.calendarSyncUIState.value.isCalendarSyncEnabled) {
                    setUpCourseCalendar()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pushNotificationPermissionLauncher.launch(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            snackBar = Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT)
            snackBar?.show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showProgress.collect {
                binding.progressBar.isVisible = it
            }
        }
    }

    private fun onRefresh(currentPage: Int) {
        if (viewModel.courseAccessStatus.value == CourseAccessError.NONE) {
            viewModel.onRefresh(CourseContainerTab.entries[currentPage])
        } else {
            viewModel.fetchCourseDetails()
        }
    }

    private fun initCourseView() {
        binding.composeCollapsingLayout.setContent {
            val isNavigationEnabled by viewModel.isNavigationEnabled.collectAsState()
            val fm = requireActivity().supportFragmentManager
            CourseDashboard(
                viewModel = viewModel,
                isNavigationEnabled = isNavigationEnabled,
                isResumed = isResumed,
                openTab = requireArguments().getString(ARG_OPEN_TAB, CourseContainerTab.HOME.name),
                fragmentManager = fm,
                onRefresh = { page ->
                    onRefresh(page)
                }
            )
        }
    }

    private fun setUpCourseCalendar() {
        binding.composeContainer.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenEdXTheme {
                    val syncState by viewModel.calendarSyncUIState.collectAsState()

                    LaunchedEffect(syncState.uiMessage.get()) {
                        syncState.uiMessage.get().takeIfNotEmpty()?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            syncState.uiMessage.set("")
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ARG_COURSE_ID = "courseId"
        const val ARG_TITLE = "title"
        const val ARG_OPEN_TAB = "open_tab"
        const val ARG_RESUME_BLOCK = "resume_block"
        fun newInstance(
            courseId: String,
            courseTitle: String,
            openTab: String = CourseContainerTab.HOME.name,
            resumeBlockId: String = "",
        ): CourseContainerFragment {
            val fragment = CourseContainerFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to courseTitle,
                ARG_OPEN_TAB to openTab,
                ARG_RESUME_BLOCK to resumeBlockId
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun CourseDashboard(
    viewModel: CourseContainerViewModel,
    isNavigationEnabled: Boolean,
    isResumed: Boolean,
    openTab: String,
    fragmentManager: FragmentManager,
    onRefresh: (page: Int) -> Unit,
) {
    OpenEdXTheme {
        val windowSize = rememberWindowSize()
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            scaffoldState = scaffoldState,
            backgroundColor = MaterialTheme.appColors.background
        ) { paddingValues ->
            val refreshing by viewModel.refreshing.collectAsState(true)
            val courseImage by viewModel.courseImage.collectAsState()
            val uiMessage by viewModel.uiMessage.collectAsState(null)
            val requiredTab = when (openTab.uppercase()) {
                CourseContainerTab.HOME.name -> CourseContainerTab.HOME
                CourseContainerTab.VIDEOS.name -> CourseContainerTab.VIDEOS
                CourseContainerTab.DATES.name -> CourseContainerTab.DATES
                CourseContainerTab.DISCUSSIONS.name -> CourseContainerTab.DISCUSSIONS
                CourseContainerTab.PROGRESS.name -> CourseContainerTab.PROGRESS
                CourseContainerTab.MORE.name -> CourseContainerTab.MORE
                else -> CourseContainerTab.HOME
            }

            val pagerState = rememberPagerState(
                initialPage = CourseContainerTab.entries.indexOf(requiredTab),
                pageCount = { CourseContainerTab.entries.size }
            )
            val accessStatus = viewModel.courseAccessStatus.observeAsState()
            val tabState = rememberLazyListState()
            val snackState = remember { SnackbarHostState() }
            val pullRefreshState = rememberPullRefreshState(
                refreshing = refreshing,
                onRefresh = { onRefresh(pagerState.currentPage) }
            )
            if (uiMessage is DatesShiftedSnackBar) {
                val datesShiftedMessage = stringResource(id = R.string.course_dates_shifted_message)
                LaunchedEffect(uiMessage) {
                    snackState.showSnackbar(
                        message = datesShiftedMessage,
                        duration = SnackbarDuration.Long
                    )
                }
            }
            HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

            LaunchedEffect(pagerState.currentPage) {
                tabState.animateScrollToItem(pagerState.currentPage)
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    CollapsingLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddingValues)
                            .pullRefresh(pullRefreshState),
                        courseImage = courseImage,
                        imageHeight = 200,
                        expandedTop = {
                            ExpandedHeaderContent(
                                courseTitle = viewModel.courseName,
                                org = viewModel.courseDetails?.courseInfoOverview?.org ?: ""
                            )
                        },
                        collapsedTop = {
                            CollapsedHeaderContent(
                                courseTitle = viewModel.courseName
                            )
                        },
                        navigation = {
                            if (isNavigationEnabled) {
                                RoundTabsBar(
                                    items = CourseContainerTab.entries,
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 16.dp
                                    ),
                                    rowState = tabState,
                                    pagerState = pagerState,
                                    withPager = true,
                                    onTabClicked = viewModel::courseContainerTabClickedEvent
                                )
                            }
                        },
                        isEnabled = CourseAccessError.NONE == accessStatus.value,
                        onBackClick = {
                            fragmentManager.popBackStack()
                        },
                        bodyContent = {
                            when (accessStatus.value) {
                                CourseAccessError.AUDIT_EXPIRED_NOT_UPGRADABLE,
                                CourseAccessError.NOT_YET_STARTED,
                                CourseAccessError.UNKNOWN -> {
                                    CourseAccessErrorView(
                                        viewModel = viewModel,
                                        accessError = accessStatus.value,
                                        fragmentManager = fragmentManager,
                                    )
                                }

                                CourseAccessError.NONE -> {
                                    DashboardPager(
                                        windowSize = windowSize,
                                        viewModel = viewModel,
                                        pagerState = pagerState,
                                        isNavigationEnabled = isNavigationEnabled,
                                        isResumed = isResumed,
                                        fragmentManager = fragmentManager,
                                    )
                                }

                                else -> {
                                }
                            }
                        }
                    )
                    PullRefreshIndicator(
                        refreshing,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter)
                    )

                    var isInternetConnectionShown by rememberSaveable {
                        mutableStateOf(false)
                    }
                    if (!isInternetConnectionShown && !viewModel.hasInternetConnection) {
                        OfflineModeDialog(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            onDismissCLick = {
                                isInternetConnectionShown = true
                            },
                            onReloadClick = {
                                isInternetConnectionShown = viewModel.hasInternetConnection
                                onRefresh(pagerState.currentPage)
                            }
                        )
                    }

                    SnackbarHost(
                        modifier = Modifier.align(Alignment.BottomStart),
                        hostState = snackState
                    ) { snackbarData: SnackbarData ->
                        DatesShiftedSnackBar(
                            showAction = CourseContainerTab.entries[pagerState.currentPage] != CourseContainerTab.DATES,
                            onViewDates = {
                                scrollToDates(scope, pagerState)
                            },
                            onClose = {
                                snackbarData.dismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardPager(
    windowSize: WindowSize,
    viewModel: CourseContainerViewModel,
    pagerState: PagerState,
    isNavigationEnabled: Boolean,
    isResumed: Boolean,
    fragmentManager: FragmentManager,
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = isNavigationEnabled,
        beyondViewportPageCount = CourseContainerTab.entries.size
    ) { page ->
        when (CourseContainerTab.entries[page]) {
            CourseContainerTab.HOME -> {
                CourseOutlineScreen(
                    windowSize = windowSize,
                    viewModel = koinViewModel(
                        parameters = { parametersOf(viewModel.courseId, viewModel.courseName) }
                    ),
                    fragmentManager = fragmentManager,
                    onResetDatesClick = {
                        viewModel.onRefresh(CourseContainerTab.DATES)
                    }
                )
            }

            CourseContainerTab.VIDEOS -> {
                CourseVideosScreen(
                    windowSize = windowSize,
                    viewModel = koinViewModel(
                        parameters = { parametersOf(viewModel.courseId, viewModel.courseName) }
                    ),
                    fragmentManager = fragmentManager
                )
            }

            CourseContainerTab.DATES -> {
                CourseDatesScreen(
                    viewModel = koinViewModel(
                        parameters = {
                            parametersOf(
                                viewModel.courseId,
                                viewModel.courseName,
                                viewModel.courseDetails?.enrollmentDetails?.mode ?: ""
                            )
                        }
                    ),
                    windowSize = windowSize,
                    fragmentManager = fragmentManager,
                    isFragmentResumed = isResumed,
                    updateCourseStructure = {
                        viewModel.updateData()
                    }
                )
            }

            CourseContainerTab.OFFLINE -> {
                CourseOfflineScreen(
                    windowSize = windowSize,
                    viewModel = koinViewModel(
                        parameters = { parametersOf(viewModel.courseId, viewModel.courseName) }
                    ),
                    fragmentManager = fragmentManager,
                )
            }

            CourseContainerTab.DISCUSSIONS -> {
                DiscussionTopicsScreen(
                    discussionTopicsViewModel = koinViewModel(
                        parameters = { parametersOf(viewModel.courseId, viewModel.courseName) }
                    ),
                    windowSize = windowSize,
                    fragmentManager = fragmentManager
                )
            }

            CourseContainerTab.PROGRESS -> {
                CourseProgressScreen(
                    windowSize = windowSize,
                    viewModel = koinViewModel(parameters = { parametersOf(viewModel.courseId) }),
                )
            }

            CourseContainerTab.MORE -> {
                HandoutsScreen(
                    windowSize = windowSize,
                    onHandoutsClick = {
                        viewModel.courseRouter.navigateToHandoutsWebView(
                            fragmentManager,
                            viewModel.courseId,
                            HandoutsType.Handouts
                        )
                    },
                    onAnnouncementsClick = {
                        viewModel.courseRouter.navigateToHandoutsWebView(
                            fragmentManager,
                            viewModel.courseId,
                            HandoutsType.Announcements
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CourseAccessErrorView(
    viewModel: CourseContainerViewModel,
    accessError: CourseAccessError?,
    fragmentManager: FragmentManager,
) {
    var icon: Painter = painterResource(id = R.drawable.course_ic_circled_arrow_up)
    var message = ""
    when (accessError) {
        CourseAccessError.AUDIT_EXPIRED_NOT_UPGRADABLE -> {
            message = stringResource(
                R.string.course_error_expired_not_upgradeable_title,
                TimeUtils.getCourseAccessFormattedDate(
                    LocalContext.current,
                    viewModel.courseDetails?.courseAccessDetails?.auditAccessExpires ?: Date()
                )
            )
        }

        CourseAccessError.NOT_YET_STARTED -> {
            icon = painterResource(id = R.drawable.course_ic_calendar)
            message = stringResource(
                R.string.course_error_not_started_title,
                viewModel.courseDetails?.courseInfoOverview?.startDisplay ?: ""
            )
        }

        CourseAccessError.UNKNOWN -> {
            icon = painterResource(id = R.drawable.course_ic_not_supported_block)
            message = stringResource(R.string.course_an_error_occurred)
        }

        else -> {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsInset()
            .background(MaterialTheme.appColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Image(
                        modifier = Modifier
                            .size(96.dp)
                            .padding(bottom = 12.dp),
                        painter = icon,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.appColors.progressBarBackgroundColor),
                    )
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = TextAlign.Center,
                    text = message,
                    style = MaterialTheme.appTypography.bodyMedium,
                    color = MaterialTheme.appColors.textDark
                )
            }
            SetupCourseAccessErrorButtons(
                viewModel = viewModel,
                accessError = accessError,
                fragmentManager = fragmentManager,
            )
        }
    }
}

@Composable
private fun SetupCourseAccessErrorButtons(
    viewModel: CourseContainerViewModel,
    accessError: CourseAccessError?,
    fragmentManager: FragmentManager,
) {
    when (accessError) {
        CourseAccessError.AUDIT_EXPIRED_NOT_UPGRADABLE,
        CourseAccessError.NOT_YET_STARTED -> {
            OpenEdXButton(
                text = stringResource(R.string.course_label_back),
                onClick = { fragmentManager.popBackStack() },
            )
        }

        CourseAccessError.UNKNOWN -> {
            if (viewModel.hasInternetConnection) {
                OpenEdXButton(
                    text = stringResource(R.string.course_label_back),
                    onClick = { fragmentManager.popBackStack() },
                )
            }
        }

        else -> {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun scrollToDates(scope: CoroutineScope, pagerState: PagerState) {
    scope.launch {
        pagerState.animateScrollToPage(CourseContainerTab.entries.indexOf(CourseContainerTab.DATES))
    }
}
