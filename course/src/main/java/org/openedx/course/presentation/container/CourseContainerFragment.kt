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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
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
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.extension.tagId
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialog
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialogType
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.RoundTabsBar
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseContainerBinding
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.dates.CourseDatesScreen
import org.openedx.course.presentation.handouts.HandoutsScreen
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.course.presentation.outline.CourseOutlineScreen
import org.openedx.course.presentation.ui.CourseVideosScreen
import org.openedx.course.presentation.ui.DatesShiftedSnackBar
import org.openedx.discussion.presentation.topics.DiscussionTopicsScreen
import java.util.Date

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
    private val courseRouter by inject<CourseRouter>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        viewModel.logCalendarPermissionAccess(!isGranted.containsValue(false))
        if (!isGranted.containsValue(false)) {
            viewModel.setCalendarSyncDialogType(CalendarSyncDialogType.SYNC_DIALOG)
        }
    }

    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(CourseContainerFragment::class.java.simpleName, "Permission granted: $granted")
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
        viewModel.accessStatus.observe(viewLifecycleOwner) { accessStatus ->
            if (accessStatus?.accessError == CourseAccessError.COURSE_NO_ACCESS) {
                viewModel.courseRouter.navigateToNoAccess(
                    requireActivity().supportFragmentManager,
                    viewModel.courseName
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pushNotificationPermissionLauncher.launch(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            snackBar = Snackbar.make(binding.root, it, Snackbar.LENGTH_INDEFINITE)
                .setAction(org.openedx.core.R.string.core_error_try_again) {
                    viewModel.preloadCourseStructure()
                }
            snackBar?.show()

        }
        viewLifecycleOwner.lifecycleScope.launch {
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
            val fm = requireActivity().supportFragmentManager
            CourseDashboard(
                viewModel = viewModel,
                isNavigationEnabled = isNavigationEnabled,
                isResumed = isResumed,
                fragmentManager = fm,
                bundle = requireArguments(),
                onRefresh = { page ->
                    onRefresh(page)
                },
                findNewCourseClick = {
                    courseRouter.navigateToMain(
                        fm = fm,
                        courseId = null,
                        infoType = null,
                        openTab = "DISCOVER"
                    )
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
    isNavigationEnabled: Boolean,
    isResumed: Boolean,
    fragmentManager: FragmentManager,
    bundle: Bundle,
    onRefresh: (page: Int) -> Unit,
    findNewCourseClick: () -> Unit
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
            val openTab =
                bundle.getString(CourseContainerFragment.ARG_OPEN_TAB, CourseContainerTab.HOME.name)
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
            val accessStatus = viewModel.accessStatus.observeAsState()
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
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 16.dp
                                    ),
                                    rowState = tabState,
                                    pagerState = pagerState,
                                    withPager = true,
                                    onTabClicked = viewModel::courseContainerTabClickedEvent
                                )
                            } else {
                                accessStatus.value?.let {
                                    if (it.accessError == null) {
                                        Spacer(modifier = Modifier.height(52.dp))
                                    }
                                }
                            }
                        },
                        isEnabled = accessStatus.value?.accessError == null,
                        onBackClick = {
                            fragmentManager.popBackStack()
                        },
                        bodyContent = {
                            accessStatus.value?.let { accessStatus ->
                                when (accessStatus.accessError) {
                                    CourseAccessError.COURSE_EXPIRED_NOT_UPGRADABLE -> {
                                        CourseExpiredNotUpgradeableMessage(
                                            date = accessStatus.date ?: Date()
                                        )
                                    }

                                    CourseAccessError.COURSE_EXPIRED_UPGRADABLE -> {
                                        CourseExpiredUpgradeableMessage(
                                            date = accessStatus.date ?: Date()
                                        )
                                    }

                                    CourseAccessError.COURSE_NOT_STARTED -> {
                                        CourseNotStartedMessage(
                                            date = accessStatus.date ?: Date()
                                        )
                                    }

                                    CourseAccessError.COURSE_NO_ACCESS -> {

                                    }

                                    null -> {
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

                accessStatus.value?.let { accessStatus ->
                    when (accessStatus.accessError) {
                        CourseAccessError.COURSE_EXPIRED_NOT_UPGRADABLE -> {
                            CourseExpiredNotUpgradeableButtons(onBackClick = { fragmentManager.popBackStack() })
                        }

                        CourseAccessError.COURSE_EXPIRED_UPGRADABLE -> {
                            CourseExpiredUpgradeableButtons(
                                sku = accessStatus.sku,
                                findNewCourseClick = findNewCourseClick
                            )
                        }

                        CourseAccessError.COURSE_NOT_STARTED -> {
                            CourseNotStartedButtons(onBackClick = { fragmentManager.popBackStack() })
                        }

                        CourseAccessError.COURSE_NO_ACCESS, null -> {

                        }
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
                HandoutsScreen(
                    windowSize = windowSize,
                    onHandoutsClick = {
                        viewModel.courseRouter.navigateToHandoutsWebView(
                            fragmentManager,
                            bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                            HandoutsType.Handouts
                        )
                    },
                    onAnnouncementsClick = {
                        viewModel.courseRouter.navigateToHandoutsWebView(
                            fragmentManager,
                            bundle.getString(CourseContainerFragment.ARG_COURSE_ID, ""),
                            HandoutsType.Announcements
                        )
                    })
            }
        }
    }
}

@Composable
private fun CourseExpiredNotUpgradeableMessage(date: Date) {
    CourseErrorMessagePlaceholder(
        iconPainter = painterResource(id = R.drawable.ic_course_update),
        textContent = {
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(
                    R.string.course_error_expired_not_upgradeable_title,
                    TimeUtils.getCourseAccessFormattedDate(LocalContext.current, date)
                ),
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )
        }
    )
}

@Composable
private fun CourseExpiredNotUpgradeableButtons(onBackClick: () -> Unit) {
    CourseErrorButtonsPlaceholder {
        OpenEdXButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(R.string.course_error_back),
            onClick = {
                onBackClick()
            },
        )
    }
}

@Composable
private fun CourseExpiredUpgradeableMessage(date: Date) {
    CourseErrorMessagePlaceholder(
        iconPainter = painterResource(id = R.drawable.ic_course_update),
        textContent = {
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(
                    R.string.course_error_expired_upgradeable_title,
                    TimeUtils.getCourseAccessFormattedDate(LocalContext.current, date)
                ),
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_course_check),
                        contentDescription = null
                    )
                    Text(
                        textAlign = TextAlign.Left,
                        text = stringResource(R.string.course_error_expired_upgradeable_option_1),
                        style = MaterialTheme.appTypography.bodyMedium,
                        color = MaterialTheme.appColors.textDark
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_course_check),
                        contentDescription = null
                    )
                    Text(
                        textAlign = TextAlign.Left,
                        text = stringResource(R.string.course_error_expired_upgradeable_option_2),
                        style = MaterialTheme.appTypography.bodyMedium,
                        color = MaterialTheme.appColors.textDark
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_course_check),
                        contentDescription = null
                    )
                    Text(
                        textAlign = TextAlign.Left,
                        text = stringResource(R.string.course_error_expired_upgradeable_option_3),
                        style = MaterialTheme.appTypography.bodyMedium,
                        color = MaterialTheme.appColors.textDark
                    )
                }
            }
        }
    )
}

@Composable
private fun CourseExpiredUpgradeableButtons(
    sku: String?,
    findNewCourseClick: () -> Unit
) {
    CourseErrorButtonsPlaceholder {
        OpenEdXOutlinedButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(R.string.course_error_expired_upgradeable_find_new_course_button),
            backgroundColor = MaterialTheme.appColors.background,
            textColor = MaterialTheme.appColors.primary,
            borderColor = MaterialTheme.appColors.primary,
            onClick = {
                findNewCourseClick()
            }
        )
        if (!sku.isNullOrEmpty()) {
            OpenEdXButton(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = {

                },
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_course_arrow_circle_up),
                            contentDescription = null
                        )
                        val buttonText =
                            stringResource(
                                R.string.course_error_expired_upgradeable_upgrade_now_button, sku
                            )
                        Text(
                            modifier = Modifier.testTag("txt_${buttonText.tagId()}"),
                            text = buttonText,
                            color = MaterialTheme.appColors.primaryButtonText,
                            style = MaterialTheme.appTypography.labelLarge
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun CourseNotStartedMessage(date: Date) {
    CourseErrorMessagePlaceholder(
        iconPainter = painterResource(id = R.drawable.ic_course_dates),
        textContent = {
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(
                    R.string.course_error_not_started_title,
                    TimeUtils.getCourseAccessFormattedDate(LocalContext.current, date)
                ),
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )
        }
    )
}

@Composable
private fun CourseNotStartedButtons(onBackClick: () -> Unit) {
    CourseErrorButtonsPlaceholder {
        OpenEdXButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(R.string.course_error_back),
            onClick = {
                onBackClick()
            },
        )
    }
}

@Composable
private fun CourseErrorMessagePlaceholder(
    iconPainter: Painter,
    textContent: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsInset()
            .displayCutoutForLandscape()
            .background(MaterialTheme.appColors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        Image(
                            painter = iconPainter,
                            contentDescription = null
                        )
                    }
                    textContent()
                }
            }
        }
    }
}

@Composable
private fun CourseErrorButtonsPlaceholder(
    buttonContent: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 27.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        buttonContent()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun scrollToDates(scope: CoroutineScope, pagerState: PagerState) {
    scope.launch {
        pagerState.animateScrollToPage(CourseContainerTab.entries.indexOf(CourseContainerTab.DATES))
    }
}
