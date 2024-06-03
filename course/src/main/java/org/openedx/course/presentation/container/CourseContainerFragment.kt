package org.openedx.course.presentation.container

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
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
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.IAPAnalyticsScreen
import org.openedx.core.presentation.course.CourseContainerTab
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.presentation.iap.IAPViewModel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IAPDialog
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.RoundTabsBar
import org.openedx.core.ui.UpgradeToAccessView
import org.openedx.core.ui.UpgradeToAccessViewType
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseContainerBinding
import org.openedx.course.presentation.calendarsync.CalendarSyncDialog
import org.openedx.course.presentation.calendarsync.CalendarSyncDialogType
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
            requireArguments().getString(ARG_ENROLLMENT_MODE, "")
        )
    }

    private val iapViewModel by viewModel<IAPViewModel> {
        parametersOf(IAPAnalyticsScreen.COURSE_DASHBOARD.screenName)
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
        initCourseView()
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
                iapViewModel = iapViewModel,
                isNavigationEnabled = isNavigationEnabled,
                isResumed = isResumed,
                fragmentManager = requireActivity().supportFragmentManager,
                bundle = requireArguments(),
                onRefresh = { page ->
                    onRefresh(page)
                },
                requireActivity = { requireActivity() }
            )
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

    companion object {
        const val ARG_COURSE_ID = "courseId"
        const val ARG_TITLE = "title"
        const val ARG_ENROLLMENT_MODE = "enrollmentMode"
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun CourseDashboard(
    viewModel: CourseContainerViewModel,
    iapViewModel: IAPViewModel,
    onRefresh: (page: Int) -> Unit,
    isNavigationEnabled: Boolean,
    isResumed: Boolean,
    fragmentManager: FragmentManager,
    bundle: Bundle,
    requireActivity: () -> FragmentActivity
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
            val dataReady = viewModel.dataReady.observeAsState()
            val iapState by iapViewModel.uiState.collectAsState()

            val pagerState = rememberPagerState(pageCount = { CourseContainerTab.entries.size })
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
                when (iapState) {
                    is IAPUIState.Loading -> {
                        IAPDialog(
                            courseTitle = (iapState as IAPUIState.Loading).courseName,
                            isLoading = true,
                            onDismiss = {
                                iapViewModel.clearIAPFLow()
                            })
                    }

                    is IAPUIState.ProductData -> {
                        IAPDialog(
                            courseTitle = (iapState as IAPUIState.ProductData).courseName,
                            formattedPrice = (iapState as IAPUIState.ProductData).formattedPrice,
                            onUpgradeNow = {
                                iapViewModel.startPurchaseFlow()
                            }, onDismiss = {
                                iapViewModel.clearIAPFLow()
                            })
                    }

                    is IAPUIState.Error -> {
                        IAPDialog(
                            courseTitle = (iapState as IAPUIState.Error).courseName,
                            isError = true,
                            onDismiss = {
                                iapViewModel.clearIAPFLow()
                            }, onGetHelp = {
                                iapViewModel.showFeedbackScreen(
                                    requireActivity(),
                                    (iapState as IAPUIState.Error).feedbackErrorMessage
                                )
                                iapViewModel.clearIAPFLow()
                            })
                    }

                    is IAPUIState.PurchaseProduct -> {
                        iapViewModel.purchaseItem(requireActivity())
                    }

                    is IAPUIState.FlowComplete -> {
                        viewModel.forceReloadCourseStructure()
                        viewModel.updateEnrolledCourses()
                        if (iapViewModel.isInProgress()) {
                            iapViewModel.upgradeSuccessEvent()
                        }
                    }

                    else -> {
                        iapViewModel.clearIAPFLow()
                    }
                }

                CollapsingLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues)
                        .pullRefresh(pullRefreshState),
                    courseImage = courseImage,
                    imageHeight = 200,
                    expandedTop = {
                        if (dataReady.value == true) {
                            ExpandedHeaderContent(
                                courseTitle = viewModel.courseName,
                                org = viewModel.courseStructure?.org!!
                            )
                        }
                    },
                    collapsedTop = {
                        CollapsedHeaderContent(
                            courseTitle = viewModel.courseName
                        )
                    },
                    upgradeButton = {
                        if (dataReady.value == true) {
                            if (viewModel.courseStructure?.isUpgradeable == true &&
                                viewModel.isValuePropEnabled
                            ) {
                                val horizontalPadding = if (!windowSize.isTablet) 16.dp else 98.dp
                                UpgradeToAccessView(
                                    modifier = Modifier.padding(
                                        start = horizontalPadding,
                                        end = 16.dp,
                                        top = 16.dp
                                    ),
                                    type = UpgradeToAccessViewType.COURSE,
                                ) {
                                    viewModel.courseStructure?.takeIf { it.productInfo != null }
                                        ?.let {
                                            iapViewModel.loadPrice(
                                                viewModel.courseId,
                                                viewModel.courseName,
                                                it.isSelfPaced,
                                                it.productInfo!!
                                            )
                                        }
                                }
                            } else if (iapViewModel.isInProgress()) {
                                iapViewModel.clearIAPFLow()
                                for (page in 0..<pagerState.pageCount) {
                                    onRefresh(page)
                                }
                            }
                        }
                    },
                    navigation = {
                        if (isNavigationEnabled) {
                            RoundTabsBar(
                                items = CourseContainerTab.entries,
                                rowState = tabState,
                                pagerState = pagerState,
                                onPageChange = viewModel::courseContainerTabClickedEvent
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
    bundle: Bundle
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
                    courseOutlineViewModel = koinViewModel(
                        parameters = {
                            parametersOf(
                                bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                                bundle.getString(CourseContainerFragment.ARG_TITLE, "")
                            )
                        }
                    ),
                    courseRouter = viewModel.courseRouter,
                    fragmentManager = fragmentManager,
                    onResetDatesClick = {
                        viewModel.onRefresh(CourseContainerTab.DATES)
                    }
                )
            }

            CourseContainerTab.VIDEOS -> {
                CourseVideosScreen(
                    windowSize = windowSize,
                    courseVideoViewModel = koinViewModel(
                        parameters = {
                            parametersOf(
                                bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                                bundle.getString(CourseContainerFragment.ARG_TITLE, "")
                            )
                        }
                    ),
                    fragmentManager = fragmentManager,
                    courseRouter = viewModel.courseRouter,
                )
            }

            CourseContainerTab.DATES -> {
                CourseDatesScreen(
                    courseDatesViewModel = koinViewModel(
                        parameters = {
                            parametersOf(
                                bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                                bundle.getString(CourseContainerFragment.ARG_TITLE, ""),
                                bundle.getString(CourseContainerFragment.ARG_ENROLLMENT_MODE, "")
                            )
                        }
                    ),
                    windowSize = windowSize,
                    courseRouter = viewModel.courseRouter,
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
