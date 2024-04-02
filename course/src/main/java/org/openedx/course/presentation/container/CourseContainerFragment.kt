package org.openedx.course.presentation.container

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.FragmentViewType
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.CoreAnalyticsScreen
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.presentation.settings.VideoQualityType
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseContainerBinding
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.calendarsync.CalendarSyncDialog
import org.openedx.course.presentation.calendarsync.CalendarSyncDialogType
import org.openedx.course.presentation.dates.CourseDatesScreen
import org.openedx.course.presentation.dates.CourseDatesViewModel
import org.openedx.course.presentation.dates.DatesUIState
import org.openedx.course.presentation.handouts.HandoutsScreen
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.course.presentation.outline.CourseOutlineScreen
import org.openedx.course.presentation.outline.CourseOutlineUIState
import org.openedx.course.presentation.outline.CourseOutlineViewModel
import org.openedx.course.presentation.ui.CourseVideosScreen
import org.openedx.course.presentation.videos.CourseVideoViewModel
import org.openedx.course.presentation.videos.CourseVideosUIState
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.topics.DiscussionTopicsScreen
import org.openedx.discussion.presentation.topics.DiscussionTopicsUIState
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel
import java.io.File

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
        viewModel.preloadCourseStructure(requireContext())
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
                    viewModel.preloadCourseStructure(requireContext())
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
        when(currentPage) {
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
                            Tabs(pagerState = pagerState, viewModel::courseContainerTabClickedEvent)
                        },
                        onBackClick = {
                            requireActivity().supportFragmentManager.popBackStack()
                        },
                        bodyContent = {
                            HorizontalPager(
                                modifier = Modifier.background(Color.White),
                                state = pagerState
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        val uiState by courseOutlineViewModel.uiState.observeAsState(CourseOutlineUIState.Loading)
                                        val uiMessage by courseOutlineViewModel.uiMessage.collectAsState(null)

                                        CourseOutlineScreen(
                                            windowSize = windowSize,
                                            uiState = uiState,
                                            apiHostUrl = courseOutlineViewModel.apiHostUrl,
                                            isCourseNestedListEnabled = courseOutlineViewModel.isCourseNestedListEnabled,
                                            uiMessage = uiMessage,
                                            hasInternetConnection = courseOutlineViewModel.hasInternetConnection,
                                            onReloadClick = {
                                                updateCourseStructure(false)
                                            },
                                            onItemClick = { block ->
                                                courseOutlineViewModel.sequentialClickedEvent(
                                                    block.blockId,
                                                    block.displayName
                                                )
                                                courseRouter.navigateToCourseSubsections(
                                                    fm = requireActivity().supportFragmentManager,
                                                    courseId = courseOutlineViewModel.courseId,
                                                    subSectionId = block.id,
                                                    mode = CourseViewMode.FULL
                                                )
                                            },
                                            onExpandClick = { block ->
                                                if (courseOutlineViewModel.switchCourseSections(block.id)) {
                                                    courseOutlineViewModel.sequentialClickedEvent(
                                                        block.blockId,
                                                        block.displayName
                                                    )
                                                }
                                            },
                                            onSubSectionClick = { subSectionBlock ->
                                                courseOutlineViewModel.courseSubSectionUnit[subSectionBlock.id]?.let { unit ->
                                                    courseOutlineViewModel.logUnitDetailViewedEvent(
                                                        unit.blockId,
                                                        unit.displayName
                                                    )
                                                    courseRouter.navigateToCourseContainer(
                                                        requireActivity().supportFragmentManager,
                                                        courseId = courseOutlineViewModel.courseId,
                                                        unitId = unit.id,
                                                        mode = CourseViewMode.FULL
                                                    )
                                                }
                                            },
                                            onResumeClick = { componentId ->
                                                courseOutlineViewModel.resumeSectionBlock?.let { subSection ->
                                                    courseOutlineViewModel.resumeCourseTappedEvent(subSection.id)
                                                    courseOutlineViewModel.resumeVerticalBlock?.let { unit ->
                                                        if (courseOutlineViewModel.isCourseExpandableSectionsEnabled) {
                                                            courseRouter.navigateToCourseContainer(
                                                                fm = requireActivity().supportFragmentManager,
                                                                courseId = courseOutlineViewModel.courseId,
                                                                unitId = unit.id,
                                                                componentId = componentId,
                                                                mode = CourseViewMode.FULL
                                                            )
                                                        } else {
                                                            courseRouter.navigateToCourseSubsections(
                                                                requireActivity().supportFragmentManager,
                                                                courseId = courseOutlineViewModel.courseId,
                                                                subSectionId = subSection.id,
                                                                mode = CourseViewMode.FULL,
                                                                unitId = unit.id,
                                                                componentId = componentId
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onDownloadClick = {
                                                if (courseOutlineViewModel.isBlockDownloading(it.id)) {
                                                    courseRouter.navigateToDownloadQueue(
                                                        fm = requireActivity().supportFragmentManager,
                                                        courseOutlineViewModel.getDownloadableChildren(it.id)
                                                            ?: arrayListOf()
                                                    )
                                                } else if (courseOutlineViewModel.isBlockDownloaded(it.id)) {
                                                    courseOutlineViewModel.removeDownloadModels(it.id)
                                                } else {
                                                    courseOutlineViewModel.saveDownloadModels(
                                                        requireContext().externalCacheDir.toString() +
                                                                File.separator +
                                                                requireContext()
                                                                    .getString(org.openedx.core.R.string.app_name)
                                                                    .replace(Regex("\\s"), "_"), it.id
                                                    )
                                                }
                                            },
                                            onResetDatesClick = {
                                                courseOutlineViewModel.resetCourseDatesBanner(onResetDates = {
                                                    updateCourseDates()
                                                })
                                            },
                                            onViewDates = {
                                                runBlocking {
                                                    pagerState.animateScrollToPage(4)
                                                }
                                            },
                                            onCertificateClick = {
                                                courseOutlineViewModel.viewCertificateTappedEvent()
                                                it.takeIfNotEmpty()
                                                    ?.let { url -> AndroidUriHandler(requireContext()).openUri(url) }
                                            }
                                        )
                                    }

                                    1 -> {
                                        val uiState by courseVideoViewModel.uiState.observeAsState(CourseVideosUIState.Loading)
                                        val uiMessage by courseVideoViewModel.uiMessage.collectAsState(null)
                                        val videoSettings by courseVideoViewModel.videoSettings.collectAsState()

                                        CourseVideosScreen(
                                            windowSize = windowSize,
                                            uiState = uiState,
                                            uiMessage = uiMessage,
                                            courseTitle = courseVideoViewModel.courseTitle,
                                            apiHostUrl = courseVideoViewModel.apiHostUrl,
                                            isCourseNestedListEnabled = courseVideoViewModel.isCourseNestedListEnabled,
                                            hasInternetConnection = courseVideoViewModel.hasInternetConnection,
                                            videoSettings = videoSettings,
                                            onReloadClick = {
                                                updateCourseStructure(false)
                                            },
                                            onItemClick = { block ->
                                                courseRouter.navigateToCourseSubsections(
                                                    fm = requireActivity().supportFragmentManager,
                                                    courseId = courseVideoViewModel.courseId,
                                                    subSectionId = block.id,
                                                    mode = CourseViewMode.VIDEOS
                                                )
                                            },
                                            onExpandClick = { block ->
                                                courseVideoViewModel.switchCourseSections(block.id)
                                            },
                                            onSubSectionClick = { subSectionBlock ->
                                                courseVideoViewModel.courseSubSectionUnit[subSectionBlock.id]?.let { unit ->
                                                    courseVideoViewModel.sequentialClickedEvent(
                                                        unit.blockId,
                                                        unit.displayName
                                                    )
                                                    courseRouter.navigateToCourseContainer(
                                                        fm = requireActivity().supportFragmentManager,
                                                        courseId = courseVideoViewModel.courseId,
                                                        unitId = unit.id,
                                                        mode = CourseViewMode.VIDEOS
                                                    )
                                                }
                                            },
                                            onDownloadClick = {
                                                if (courseVideoViewModel.isBlockDownloading(it.id)) {
                                                    courseRouter.navigateToDownloadQueue(
                                                        fm = requireActivity().supportFragmentManager,
                                                        courseVideoViewModel.getDownloadableChildren(it.id)
                                                            ?: arrayListOf()
                                                    )
                                                } else if (courseVideoViewModel.isBlockDownloaded(it.id)) {
                                                    courseVideoViewModel.removeDownloadModels(it.id)
                                                } else {
                                                    courseVideoViewModel.saveDownloadModels(
                                                        requireContext().externalCacheDir.toString() +
                                                                File.separator +
                                                                requireContext()
                                                                    .getString(org.openedx.core.R.string.app_name)
                                                                    .replace(Regex("\\s"), "_"), it.id
                                                    )
                                                }
                                            },
                                            onDownloadAllClick = { isAllBlocksDownloadedOrDownloading ->
                                                courseVideoViewModel.logBulkDownloadToggleEvent(!isAllBlocksDownloadedOrDownloading)
                                                if (isAllBlocksDownloadedOrDownloading) {
                                                    courseVideoViewModel.removeAllDownloadModels()
                                                } else {
                                                    courseVideoViewModel.saveAllDownloadModels(
                                                        requireContext().externalCacheDir.toString() +
                                                                File.separator +
                                                                requireContext()
                                                                    .getString(org.openedx.core.R.string.app_name)
                                                                    .replace(Regex("\\s"), "_")
                                                    )
                                                }
                                            },
                                            onDownloadQueueClick = {
                                                if (courseVideoViewModel.hasDownloadModelsInQueue()) {
                                                    courseRouter.navigateToDownloadQueue(fm = requireActivity().supportFragmentManager)
                                                }
                                            },
                                            onVideoDownloadQualityClick = {
                                                if (courseVideoViewModel.hasDownloadModelsInQueue()) {
                                                    courseVideoViewModel.onChangingVideoQualityWhileDownloading()
                                                } else {
                                                    courseRouter.navigateToVideoQuality(
                                                        requireActivity().supportFragmentManager,
                                                        VideoQualityType.Download
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    2 -> {
                                        val uiState by courseDatesViewModel.uiState.observeAsState(DatesUIState.Loading)
                                        val uiMessage by courseDatesViewModel.uiMessage.collectAsState(null)
                                        val calendarSyncUIState by courseDatesViewModel.calendarSyncUIState.collectAsState()

                                        CourseDatesScreen(
                                            windowSize = windowSize,
                                            uiState = uiState,
                                            uiMessage = uiMessage,
                                            isSelfPaced = courseDatesViewModel.isSelfPaced,
                                            hasInternetConnection = courseDatesViewModel.hasInternetConnection,
                                            calendarSyncUIState = calendarSyncUIState,
                                            onReloadClick = {
                                                courseDatesViewModel.getCourseDates()
                                            },
                                            onItemClick = { block ->
                                                if (block.blockId.isNotEmpty()) {
                                                    courseDatesViewModel.getVerticalBlock(block.blockId)
                                                        ?.let { verticalBlock ->
                                                            courseDatesViewModel.logCourseComponentTapped(true, block)
                                                            if (courseDatesViewModel.isCourseExpandableSectionsEnabled) {
                                                                courseRouter.navigateToCourseContainer(
                                                                    fm = requireActivity().supportFragmentManager,
                                                                    courseId = courseDatesViewModel.courseId,
                                                                    unitId = verticalBlock.id,
                                                                    componentId = "",
                                                                    mode = CourseViewMode.FULL
                                                                )
                                                            } else {
                                                                courseDatesViewModel.getSequentialBlock(verticalBlock.id)
                                                                    ?.let { sequentialBlock ->
                                                                        courseRouter.navigateToCourseSubsections(
                                                                            fm = requireActivity().supportFragmentManager,
                                                                            subSectionId = sequentialBlock.id,
                                                                            courseId = courseDatesViewModel.courseId,
                                                                            unitId = verticalBlock.id,
                                                                            mode = CourseViewMode.FULL
                                                                        )
                                                                    }
                                                            }
                                                        } ?: {
                                                        courseDatesViewModel.logCourseComponentTapped(false, block)
                                                        ActionDialogFragment.newInstance(
                                                            title = getString(org.openedx.core.R.string.core_leaving_the_app),
                                                            message = getString(
                                                                org.openedx.core.R.string.core_leaving_the_app_message,
                                                                getString(org.openedx.core.R.string.platform_name)
                                                            ),
                                                            url = block.link,
                                                            source = CoreAnalyticsScreen.COURSE_DATES.screenName
                                                        ).show(
                                                            requireActivity().supportFragmentManager,
                                                            ActionDialogFragment::class.simpleName
                                                        )

                                                    }
                                                }
                                            },
                                            onPLSBannerViewed = {
                                                if (isResumed) {
                                                    courseDatesViewModel.logPlsBannerViewed()
                                                }
                                            },
                                            onSyncDates = {
                                                courseDatesViewModel.logPlsShiftButtonClicked()
                                                courseDatesViewModel.resetCourseDatesBanner {
                                                    courseDatesViewModel.logPlsShiftDates(it)
                                                    if (it) {
                                                        updateCourseStructure(false)
                                                    }
                                                }
                                            },
                                            onCalendarSyncSwitch = { isChecked ->
                                                courseDatesViewModel.handleCalendarSyncState(isChecked)
                                            },
                                        )
                                    }

                                    3 -> {
                                        val uiState by discussionTopicsViewModel.uiState.observeAsState(DiscussionTopicsUIState.Loading)
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

