package org.openedx.course.presentation.container

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
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
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.RoundTabsBar
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseContainerBinding
import org.openedx.course.presentation.dates.CourseDatesScreen
import org.openedx.course.presentation.handouts.HandoutsScreen
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.course.presentation.outline.CourseOutlineScreen
import org.openedx.course.presentation.ui.CourseVideosScreen
import org.openedx.course.presentation.ui.DatesShiftedSnackBar
import org.openedx.discussion.presentation.topics.DiscussionTopicsScreen

class CourseContainerFragment : Fragment(R.layout.fragment_course_container) {

    private val binding by viewBinding(FragmentCourseContainerBinding::bind)

    private val viewModel by viewModel<CourseContainerViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TITLE, ""),
            requireArguments().getString(ARG_ENROLLMENT_MODE, ""),
            requireArguments().getString(ARG_RESUME_BLOCK, "")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.preloadCourseStructure()
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
        viewModel.updateData()
    }

    override fun onDestroyView() {
        snackBar?.dismiss()
        super.onDestroyView()
    }

    private fun observe() {
        viewModel.dataReady.observe(viewLifecycleOwner) { isReady ->
            if (isReady == false) {
                viewModel.courseRouter.navigateToNoAccess(
                    requireActivity().supportFragmentManager,
                    viewModel.courseName
                )
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            snackBar = Snackbar.make(binding.root, it, Snackbar.LENGTH_INDEFINITE)
                .setAction(org.openedx.core.R.string.core_error_try_again) {
                    viewModel.preloadCourseStructure()
                }
            snackBar?.show()

        }
        lifecycleScope.launch {
            viewModel.showProgress.collect {
                binding.progressBar.isVisible = it
            }
        }
    }

    private fun onRefresh(currentPage: Int) {
        viewModel.onRefresh(CourseContainerTab.entries[currentPage])
    }

    private fun initCourseView() {
        binding.composeCollapsingLayout.setContent {
            val isNavigationEnabled by viewModel.isNavigationEnabled.collectAsState()
            CourseDashboard(
                viewModel = viewModel,
                isNavigationEnabled = isNavigationEnabled,
                isResumed = isResumed,
                fragmentManager = requireActivity().supportFragmentManager,
                bundle = requireArguments(),
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
        const val ARG_ENROLLMENT_MODE = "enrollmentMode"
        const val ARG_OPEN_TAB = "open_tab"
        const val ARG_RESUME_BLOCK = "resume_block"
        fun newInstance(
            courseId: String,
            courseTitle: String,
            enrollmentMode: String,
            openTab: String = CourseContainerTab.HOME.name,
            resumeBlockId: String = ""
        ): CourseContainerFragment {
            val fragment = CourseContainerFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to courseTitle,
                ARG_ENROLLMENT_MODE to enrollmentMode,
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
    onRefresh: (page: Int) -> Unit,
    isNavigationEnabled: Boolean,
    isResumed: Boolean,
    fragmentManager: FragmentManager,
    bundle: Bundle
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
            val openTab = bundle.getString(CourseContainerFragment.ARG_OPEN_TAB, CourseContainerTab.HOME.name)
            val requiredTab = when (openTab.uppercase()) {
                CourseContainerTab.HOME.name -> CourseContainerTab.HOME
                CourseContainerTab.VIDEOS.name -> CourseContainerTab.VIDEOS
                CourseContainerTab.DATES.name -> CourseContainerTab.DATES
                CourseContainerTab.DISCUSSIONS.name -> CourseContainerTab.DISCUSSIONS
                CourseContainerTab.MORE.name -> CourseContainerTab.MORE
                else -> CourseContainerTab.HOME
            }

            val pagerState = rememberPagerState(
                initialPage = CourseContainerTab.entries.indexOf(requiredTab),
                pageCount = { CourseContainerTab.entries.size }
            )
            val dataReady = viewModel.dataReady.observeAsState()
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

            Box {
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
                            org = viewModel.organization
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                                rowState = tabState,
                                pagerState = pagerState,
                                withPager = true,
                                onTabClicked = viewModel::courseContainerTabClickedEvent
                            )
                        } else {
                            Spacer(modifier = Modifier.height(52.dp))
                        }
                    },
                    onBackClick = {
                        fragmentManager.popBackStack()
                    },
                    bodyContent = {
                        if (dataReady.value == true) {
                            DashboardPager(
                                windowSize = windowSize,
                                viewModel = viewModel,
                                pagerState = pagerState,
                                isNavigationEnabled = isNavigationEnabled,
                                isResumed = isResumed,
                                fragmentManager = fragmentManager,
                                bundle = bundle
                            )
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
                            isInternetConnectionShown = true
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardPager(
    windowSize: WindowSize,
    viewModel: CourseContainerViewModel,
    pagerState: PagerState,
    isNavigationEnabled: Boolean,
    isResumed: Boolean,
    fragmentManager: FragmentManager,
    bundle: Bundle,
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = isNavigationEnabled,
        beyondBoundsPageCount = CourseContainerTab.entries.size
    ) { page ->
        when (CourseContainerTab.entries[page]) {
            CourseContainerTab.HOME -> {
                CourseOutlineScreen(
                    windowSize = windowSize,
                    viewModel = koinViewModel(
                        parameters = {
                            parametersOf(
                                bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                                bundle.getString(CourseContainerFragment.ARG_TITLE, "")
                            )
                        }
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
                        parameters = {
                            parametersOf(
                                bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                                bundle.getString(CourseContainerFragment.ARG_TITLE, "")
                            )
                        }
                    ),
                    fragmentManager = fragmentManager
                )
            }

            CourseContainerTab.DATES -> {
                CourseDatesScreen(
                    viewModel = koinViewModel(
                        parameters = {
                            parametersOf(
                                bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                                bundle.getString(CourseContainerFragment.ARG_TITLE, ""),
                                bundle.getString(CourseContainerFragment.ARG_ENROLLMENT_MODE, "")
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

            CourseContainerTab.DISCUSSIONS -> {
                DiscussionTopicsScreen(
                    discussionTopicsViewModel = koinViewModel(
                        parameters = {
                            parametersOf(
                                bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                                bundle.getString(CourseContainerFragment.ARG_TITLE, ""),
                            )
                        }
                    ),
                    windowSize = windowSize,
                    fragmentManager = fragmentManager
                )
            }

            CourseContainerTab.MORE -> {
                val announcementsString = stringResource(id = R.string.course_announcements)
                val handoutsString = stringResource(id = R.string.course_handouts)
                HandoutsScreen(
                    windowSize = windowSize,
                    onHandoutsClick = {
                        viewModel.courseRouter.navigateToHandoutsWebView(
                            fragmentManager,
                            bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                            handoutsString,
                            HandoutsType.Handouts
                        )
                    },
                    onAnnouncementsClick = {
                        viewModel.courseRouter.navigateToHandoutsWebView(
                            fragmentManager,
                            bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                            announcementsString,
                            HandoutsType.Announcements
                        )
                    })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun scrollToDates(scope: CoroutineScope, pagerState: PagerState) {
    scope.launch {
        pagerState.animateScrollToPage(CourseContainerTab.entries.indexOf(CourseContainerTab.DATES))
    }
}
