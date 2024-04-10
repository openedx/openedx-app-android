package org.openedx.course.presentation.container

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.FragmentViewType
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseContainerBinding
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.calendarsync.CalendarSyncDialog
import org.openedx.course.presentation.calendarsync.CalendarSyncDialogType
import org.openedx.course.presentation.dates.CourseDatesScreen
import org.openedx.course.presentation.dates.CourseDatesViewModel
import org.openedx.course.presentation.handouts.HandoutsScreen
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.course.presentation.outline.CourseOutlineScreen
import org.openedx.course.presentation.outline.CourseOutlineViewModel
import org.openedx.course.presentation.ui.CourseVideosScreen
import org.openedx.course.presentation.videos.CourseVideoViewModel
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.topics.DiscussionTopicsScreen
import org.openedx.discussion.presentation.topics.DiscussionTopicsUIState
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel

class CourseContainerFragment : Fragment(R.layout.fragment_course_container) {

    private val binding by viewBinding(FragmentCourseContainerBinding::bind)

    private val courseRouter by inject<CourseRouter>()
    private val discussionRouter by inject<DiscussionRouter>()

    private val viewModel by viewModel<CourseContainerViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TITLE, ""),
            requireArguments().getString(ARG_ENROLLMENT_MODE, "")
        )
    }

    private val discussionTopicsViewModel by viewModel<DiscussionTopicsViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TITLE, "")
        )
    }

    private val courseDatesViewModel by viewModel<CourseDatesViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_ENROLLMENT_MODE, ""),
            requireArguments().getString(ARG_TITLE, "")
        )
    }

    private val courseVideoViewModel by viewModel<CourseVideoViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TITLE, "")
        )
    }

    private val courseOutlineViewModel by viewModel<CourseOutlineViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TITLE, "")
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        viewModel.logCalendarPermissionAccess(!isGranted.containsValue(false))
        if (!isGranted.containsValue(false)) {
            viewModel.setCalendarSyncDialogType(CalendarSyncDialogType.SYNC_DIALOG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.preloadCourseStructure()
    }

    private var snackBar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCollapsingLayout()
        if (viewModel.calendarSyncUIState.value.isCalendarSyncEnabled) {
            setUpCourseCalendar()
        }
        observe()
    }

    override fun onDestroyView() {
        snackBar?.dismiss()
        super.onDestroyView()
    }

    private fun observe() {
        viewModel.dataReady.observe(viewLifecycleOwner) { isReady ->
            if (isReady == true) {
                setupCollapsingLayout()
                courseOutlineViewModel.getCourseData()
                courseDatesViewModel.isSelfPaced = viewModel.isSelfPaced
                courseDatesViewModel.updateAndFetchCalendarSyncState()
            } else {
                courseRouter.navigateToNoAccess(
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
        when (currentPage) {
            0 -> {
                courseOutlineViewModel.setIsUpdating()
                updateCourseStructure(true)
            }

            1 -> {
                courseVideoViewModel.setIsUpdating()
                updateCourseStructure(true)
            }

            2 -> {
                courseDatesViewModel.getCourseDates(swipeToRefresh = true)
            }

            3 -> {
                discussionTopicsViewModel.updateCourseTopics()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
    private fun setupCollapsingLayout() {
        binding.composeCollapsingLayout.setContent {
            OpenEdXTheme {
                val refreshing by viewModel.showProgress.collectAsState(true)
                val courseImage by viewModel.courseImage.collectAsState()
                val pagerState = rememberPagerState(pageCount = { 5 })
                val pullRefreshState = rememberPullRefreshState(
                    refreshing = refreshing,
                    onRefresh = { onRefresh(pagerState.currentPage) }
                )
                val windowSize = rememberWindowSize()
                Box {
                    CollapsingLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pullRefresh(pullRefreshState),
                        courseImage = courseImage,
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
                            CourseHomeTabs(pagerState = pagerState, viewModel::courseContainerTabClickedEvent)
                        },
                        onBackClick = {
                            requireActivity().supportFragmentManager.popBackStack()
                        },
                        bodyContent = {
                            HorizontalPager(
                                state = pagerState
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        val scope = rememberCoroutineScope()
                                        CourseOutlineScreen(
                                            windowSize = windowSize,
                                            courseOutlineViewModel = courseOutlineViewModel,
                                            courseRouter = courseRouter,
                                            fragmentManager = requireActivity().supportFragmentManager,
                                            onReloadClick = {
                                                updateCourseStructure(false)
                                            },
                                            onResetDatesClick = {
                                                courseOutlineViewModel.resetCourseDatesBanner(onResetDates = {
                                                    updateCourseDates()
                                                })
                                            },
                                            onViewDates = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(4)
                                                }
                                            }
                                        )
                                    }

                                    1 -> {
                                        CourseVideosScreen(
                                            windowSize = windowSize,
                                            courseVideoViewModel = courseVideoViewModel,
                                            fragmentManager = requireActivity().supportFragmentManager,
                                            courseRouter = courseRouter,
                                            onReloadClick = {
                                                updateCourseStructure(false)
                                            }
                                        )
                                    }

                                    2 -> {
                                        CourseDatesScreen(
                                            windowSize = windowSize,
                                            courseDatesViewModel = courseDatesViewModel,
                                            courseRouter = courseRouter,
                                            fragmentManager = requireActivity().supportFragmentManager,
                                            isFragmentResumed = isResumed,
                                            updateCourseStructure = {
                                                updateCourseStructure(false)
                                            }
                                        )
                                    }

                                    3 -> {
                                        val uiState by discussionTopicsViewModel.uiState.observeAsState(
                                            DiscussionTopicsUIState.Loading
                                        )
                                        val uiMessage by discussionTopicsViewModel.uiMessage.collectAsState(null)

                                        DiscussionTopicsScreen(
                                            windowSize = windowSize,
                                            uiState = uiState,
                                            uiMessage = uiMessage,

                                            onItemClick = { action, data, title ->
                                                discussionTopicsViewModel.discussionClickedEvent(action, data, title)
                                                discussionRouter.navigateToDiscussionThread(
                                                    requireActivity().supportFragmentManager,
                                                    action,
                                                    discussionTopicsViewModel.courseId,
                                                    data,
                                                    title,
                                                    FragmentViewType.FULL_CONTENT
                                                )
                                            },
                                            onSearchClick = {
                                                discussionRouter.navigateToSearchThread(
                                                    requireActivity().supportFragmentManager,
                                                    discussionTopicsViewModel.courseId
                                                )
                                            }
                                        )
                                    }

                                    4 -> {
                                        HandoutsScreen(
                                            windowSize = windowSize,
                                            onHandoutsClick = {
                                                courseRouter.navigateToHandoutsWebView(
                                                    requireActivity().supportFragmentManager,
                                                    requireArguments().getString(ARG_COURSE_ID, ""),
                                                    getString(R.string.course_handouts),
                                                    HandoutsType.Handouts
                                                )
                                            },
                                            onAnnouncementsClick = {
                                                courseRouter.navigateToHandoutsWebView(
                                                    requireActivity().supportFragmentManager,
                                                    requireArguments().getString(ARG_COURSE_ID, ""),
                                                    getString(R.string.course_announcements),
                                                    HandoutsType.Announcements
                                                )
                                            })
                                    }
                                }
                            }
                        }
                    )
                    PullRefreshIndicator(
                        refreshing,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }

    private fun setUpCourseCalendar() {
        binding.composeContainer.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenEdXTheme {
                    val syncState by viewModel.calendarSyncUIState.collectAsState()

                    LaunchedEffect(key1 = syncState.checkForOutOfSync) {
                        if (syncState.isCalendarSyncEnabled && syncState.checkForOutOfSync.get()) {
                            viewModel.checkIfCalendarOutOfDate()
                        }
                    }

                    LaunchedEffect(syncState.uiMessage.get()) {
                        syncState.uiMessage.get().takeIfNotEmpty()?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            syncState.uiMessage.set("")
                        }
                    }

                    CalendarSyncDialog(
                        syncDialogType = syncState.dialogType,
                        calendarTitle = syncState.calendarTitle,
                        syncDialogPosAction = { dialog ->
                            when (dialog) {
                                CalendarSyncDialogType.SYNC_DIALOG -> {
                                    viewModel.logCalendarAddDates(true)
                                    viewModel.addOrUpdateEventsInCalendar(
                                        updatedEvent = false,
                                    )
                                }

                                CalendarSyncDialogType.UN_SYNC_DIALOG -> {
                                    viewModel.logCalendarRemoveDates(true)
                                    viewModel.deleteCourseCalendar()
                                }

                                CalendarSyncDialogType.PERMISSION_DIALOG -> {
                                    permissionLauncher.launch(viewModel.calendarPermissions)
                                }

                                CalendarSyncDialogType.OUT_OF_SYNC_DIALOG -> {
                                    viewModel.logCalendarSyncUpdate(true)
                                    viewModel.addOrUpdateEventsInCalendar(
                                        updatedEvent = true,
                                    )
                                }

                                CalendarSyncDialogType.EVENTS_DIALOG -> {
                                    viewModel.logCalendarSyncedConfirmation(true)
                                    viewModel.openCalendarApp()
                                }

                                else -> {}
                            }
                        },
                        syncDialogNegAction = { dialog ->
                            when (dialog) {
                                CalendarSyncDialogType.SYNC_DIALOG ->
                                    viewModel.logCalendarAddDates(false)

                                CalendarSyncDialogType.UN_SYNC_DIALOG ->
                                    viewModel.logCalendarRemoveDates(false)

                                CalendarSyncDialogType.OUT_OF_SYNC_DIALOG -> {
                                    viewModel.logCalendarSyncUpdate(false)
                                    viewModel.deleteCourseCalendar()
                                }

                                CalendarSyncDialogType.EVENTS_DIALOG ->
                                    viewModel.logCalendarSyncedConfirmation(false)

                                CalendarSyncDialogType.LOADING_DIALOG,
                                CalendarSyncDialogType.PERMISSION_DIALOG,
                                CalendarSyncDialogType.NONE,
                                -> {
                                }
                            }

                            viewModel.setCalendarSyncDialogType(CalendarSyncDialogType.NONE)
                        },
                        dismissSyncDialog = {
                            viewModel.setCalendarSyncDialogType(CalendarSyncDialogType.NONE)
                        }
                    )
                }
            }
        }
    }

    private fun updateCourseStructure(withSwipeRefresh: Boolean) {
        viewModel.updateData(withSwipeRefresh)
    }

    private fun updateCourseDates() {
        courseDatesViewModel.getCourseDates()
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        private const val ARG_ENROLLMENT_MODE = "enrollmentMode"
        fun newInstance(
            courseId: String,
            courseTitle: String,
            enrollmentMode: String,
        ): CourseContainerFragment {
            val fragment = CourseContainerFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to courseTitle,
                ARG_ENROLLMENT_MODE to enrollmentMode
            )
            return fragment
        }
    }
}

