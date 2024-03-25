package org.openedx.course.presentation.dates

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.UIMessage
import org.openedx.core.data.model.DateType
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.extension.isNotEmptyThenLet
import org.openedx.core.presentation.CoreAnalyticsScreen
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.core.utils.TimeUtils
import org.openedx.core.utils.clearTime
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.R
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.calendarsync.CalendarSyncUIState
import org.openedx.course.presentation.container.CourseContainerFragment
import org.openedx.course.presentation.ui.CourseDatesBanner
import org.openedx.course.presentation.ui.CourseDatesBannerTablet
import org.openedx.course.presentation.ui.DatesShiftedSnackBar
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as coreR

class CourseDatesFragment : Fragment() {

    val viewModel by viewModel<CourseDatesViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_COURSE_NAME, ""),
            requireArguments().getBoolean(ARG_IS_SELF_PACED, true),
            requireArguments().getString(ARG_ENROLLMENT_MODE, "")
        )
    }
    private val router by inject<CourseRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.updateAndFetchCalendarSyncState()
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
                val uiState by viewModel.uiState.observeAsState(DatesUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.updating.observeAsState(false)
                val calendarSyncUIState by viewModel.calendarSyncUIState.collectAsState()

                CourseDatesScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    isSelfPaced = viewModel.isSelfPaced,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    calendarSyncUIState = calendarSyncUIState,
                    onReloadClick = {
                        viewModel.getCourseDates()
                    },
                    onSwipeRefresh = {
                        viewModel.getCourseDates(swipeToRefresh = true)
                    },
                    onItemClick = { block ->
                        if (block.blockId.isNotEmpty()) {
                            viewModel.getVerticalBlock(block.blockId)?.let { verticalBlock ->
                                viewModel.logCourseComponentTapped(true, block)
                                if (viewModel.isCourseExpandableSectionsEnabled) {
                                    router.navigateToCourseContainer(
                                        fm = requireActivity().supportFragmentManager,
                                        courseId = viewModel.courseId,
                                        unitId = verticalBlock.id,
                                        componentId = "",
                                        mode = CourseViewMode.FULL
                                    )
                                } else {
                                    viewModel.getSequentialBlock(verticalBlock.id)
                                        ?.let { sequentialBlock ->
                                            router.navigateToCourseSubsections(
                                                fm = requireActivity().supportFragmentManager,
                                                subSectionId = sequentialBlock.id,
                                                courseId = viewModel.courseId,
                                                unitId = verticalBlock.id,
                                                mode = CourseViewMode.FULL
                                            )
                                        }
                                }
                            } ?: {
                                viewModel.logCourseComponentTapped(false, block)
                                ActionDialogFragment.newInstance(
                                    title = getString(coreR.string.core_leaving_the_app),
                                    message = getString(
                                        coreR.string.core_leaving_the_app_message,
                                        getString(coreR.string.platform_name)
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
                        if (isVisible()) {
                            viewModel.logPlsBannerViewed()
                        }
                    },
                    onSyncDates = {
                        viewModel.logPlsShiftButtonClicked()
                        viewModel.resetCourseDatesBanner {
                            viewModel.logPlsShiftDates(it)
                            if (it) {
                                (parentFragment as CourseContainerFragment)
                                    .updateCourseStructure(false)
                            }
                        }
                    },
                    onCalendarSyncSwitch = { isChecked ->
                        viewModel.handleCalendarSyncState(isChecked)
                    },
                )
            }
        }
    }

    fun updateData() {
        viewModel.getCourseDates()
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_COURSE_NAME = "courseName"
        private const val ARG_IS_SELF_PACED = "selfPaced"
        private const val ARG_ENROLLMENT_MODE = "enrollmentMode"

        fun newInstance(
            courseId: String,
            courseName: String,
            isSelfPaced: Boolean,
            enrollmentMode: String,
        ): CourseDatesFragment {
            val fragment = CourseDatesFragment()
            fragment.arguments =
                bundleOf(
                    ARG_COURSE_ID to courseId,
                    ARG_COURSE_NAME to courseName,
                    ARG_IS_SELF_PACED to isSelfPaced,
                    ARG_ENROLLMENT_MODE to enrollmentMode,
                )
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CourseDatesScreen(
    windowSize: WindowSize,
    uiState: DatesUIState,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    isSelfPaced: Boolean,
    hasInternetConnection: Boolean,
    calendarSyncUIState: CalendarSyncUIState,
    onReloadClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onItemClick: (CourseDateBlock) -> Unit,
    onPLSBannerViewed: () -> Unit,
    onSyncDates: () -> Unit,
    onCalendarSyncSwitch: (Boolean) -> Unit = {},
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) {
        val modifierScreenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val listBottomPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(bottom = 24.dp),
                    compact = PaddingValues(bottom = 24.dp)
                )
            )
        }

        val snackState = remember { SnackbarHostState() }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .displayCutoutForLandscape(), contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = modifierScreenWidth,
                color = MaterialTheme.appColors.background,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .pullRefresh(pullRefreshState)
                ) {
                    when (uiState) {
                        is DatesUIState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is DatesUIState.Dates -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = listBottomPadding
                            ) {
                                val courseBanner = uiState.courseDatesResult.courseBanner
                                val datesSection = uiState.courseDatesResult.datesSection

                                if (calendarSyncUIState.isCalendarSyncEnabled) {
                                    item {
                                        CalendarSyncCard(
                                            modifier = Modifier.padding(top = 24.dp),
                                            checked = calendarSyncUIState.isSynced,
                                            onCalendarSync = onCalendarSyncSwitch
                                        )
                                    }
                                }

                                if (courseBanner.isBannerAvailableForUserType(isSelfPaced)) {
                                    item {
                                        onPLSBannerViewed()
                                        if (windowSize.isTablet) {
                                            CourseDatesBannerTablet(
                                                modifier = Modifier.padding(top = 16.dp),
                                                banner = courseBanner,
                                                resetDates = onSyncDates,
                                            )
                                        } else {
                                            CourseDatesBanner(
                                                modifier = Modifier.padding(top = 16.dp),
                                                banner = courseBanner,
                                                resetDates = onSyncDates
                                            )
                                        }
                                    }
                                }

                                // Handle DatesSection.COMPLETED separately
                                datesSection[DatesSection.COMPLETED]?.isNotEmptyThenLet { section ->
                                    item {
                                        ExpandableView(
                                            sectionKey = DatesSection.COMPLETED,
                                            sectionDates = section,
                                            onItemClick = onItemClick,
                                        )
                                    }
                                }

                                // Handle other sections
                                val sectionsKey =
                                    datesSection.keys.minus(DatesSection.COMPLETED).toList()
                                sectionsKey.forEach { sectionKey ->
                                    datesSection[sectionKey]?.isNotEmptyThenLet { section ->
                                        item {
                                            CourseDateBlockSection(
                                                sectionKey = sectionKey,
                                                sectionDates = section,
                                                onItemClick = onItemClick,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        DatesUIState.Empty -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(id = R.string.course_dates_unavailable_message),
                                    color = MaterialTheme.appColors.textPrimary,
                                    style = MaterialTheme.appTypography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter)
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
                            })
                    }
                }

                SnackbarHost(
                    modifier = Modifier.align(Alignment.BottomStart),
                    hostState = snackState
                ) { snackbarData: SnackbarData ->
                    DatesShiftedSnackBar(onClose = {
                        snackbarData.dismiss()
                    })
                }
            }
        }
    }
}

@Composable
fun CalendarSyncCard(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCalendarSync: (Boolean) -> Unit,
) {
    val cardModifier = modifier
        .background(
            MaterialTheme.appColors.cardViewBackground,
            MaterialTheme.appShapes.material.medium
        )
        .border(
            1.dp,
            MaterialTheme.appColors.cardViewBorder,
            MaterialTheme.appShapes.material.medium
        )
        .padding(16.dp)

    Column(modifier = cardModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = painterResource(id = R.drawable.course_ic_calenday_sync),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .weight(1f),
                text = stringResource(id = R.string.course_header_sync_to_calendar),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark
            )
            Switch(
                checked = checked,
                onCheckedChange = onCalendarSync,
                modifier = Modifier.size(48.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.appColors.primary,
                    checkedTrackColor = MaterialTheme.appColors.primary
                )
            )
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(40.dp),
            text = stringResource(id = R.string.course_body_sync_to_calendar),
            style = MaterialTheme.appTypography.bodyMedium,
            color = MaterialTheme.appColors.textDark,
        )
    }
}

@Composable
fun ExpandableView(
    sectionKey: DatesSection = DatesSection.NONE,
    sectionDates: List<CourseDateBlock>,
    onItemClick: (CourseDateBlock) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // expandable view Animation
    val transition = updateTransition(targetState = expanded, label = "expandable")
    val iconRotationDeg by transition.animateFloat(label = "icon rotation") { if (it) 180f else 0f }
    val enterTransition = remember {
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeIn(initialAlpha = 0.3f, animationSpec = tween(300))
    }
    val exitTransition = remember {
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.appColors.cardViewBackground, MaterialTheme.shapes.medium)
            .border(0.75.dp, MaterialTheme.appColors.cardViewBorder, MaterialTheme.shapes.medium)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 16.dp, end = 8.dp, bottom = 8.dp)
            .clickable { expanded = !expanded }) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent)
            ) {
                Text(
                    text = stringResource(id = sectionKey.stringResId),
                    style = MaterialTheme.appTypography.titleMedium,
                    color = MaterialTheme.appColors.textDark,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = expanded.not()) {
                    Text(
                        text = pluralStringResource(
                            id = coreR.plurals.core_date_items_hidden,
                            count = sectionDates.size,
                            formatArgs = arrayOf(sectionDates.size)
                        ),
                        style = MaterialTheme.appTypography.labelMedium,
                        color = MaterialTheme.appColors.textDark,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                tint = MaterialTheme.appColors.textDark,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(iconRotationDeg)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = expanded,
            enter = enterTransition,
            exit = exitTransition,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .padding(top = 52.dp, bottom = 8.dp)
        ) {
            CourseDateBlockSection(
                sectionKey = sectionKey,
                sectionDates = sectionDates,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun CourseDateBlockSection(
    sectionKey: DatesSection = DatesSection.NONE,
    sectionDates: List<CourseDateBlock>,
    onItemClick: (CourseDateBlock) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        if (sectionKey != DatesSection.COMPLETED) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                text = stringResource(id = sectionKey.stringResId),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleMedium,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Min) // this make height of all cards to the tallest card.
        ) {
            if (sectionKey != DatesSection.COMPLETED) {
                DateBullet(section = sectionKey)
            }
            DateBlock(dateBlocks = sectionDates, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun DateBullet(
    section: DatesSection = DatesSection.NONE,
) {
    val barColor = when (section) {
        DatesSection.COMPLETED -> MaterialTheme.appColors.cardViewBackground
        DatesSection.PAST_DUE -> MaterialTheme.appColors.datesSectionBarPastDue
        DatesSection.TODAY -> MaterialTheme.appColors.datesSectionBarToday
        DatesSection.THIS_WEEK -> MaterialTheme.appColors.datesSectionBarThisWeek
        DatesSection.NEXT_WEEK -> MaterialTheme.appColors.datesSectionBarNextWeek
        DatesSection.UPCOMING -> MaterialTheme.appColors.datesSectionBarUpcoming
        else -> MaterialTheme.appColors.background
    }
    Box(
        modifier = Modifier
            .width(8.dp)
            .fillMaxHeight()
            .padding(top = 2.dp, bottom = 2.dp)
            .background(
                color = barColor, shape = MaterialTheme.shapes.medium
            )
    )
}

@Composable
private fun DateBlock(
    dateBlocks: List<CourseDateBlock>,
    onItemClick: (CourseDateBlock) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 8.dp, end = 8.dp),
    ) {
        var lastAssignmentDate = dateBlocks.first().date.clearTime()
        dateBlocks.forEachIndexed { index, dateBlock ->
            var canShowDate = index == 0
            if (index != 0) {
                canShowDate = (lastAssignmentDate != dateBlock.date)
            }
            CourseDateItem(dateBlock, canShowDate, index != 0, onItemClick)
            lastAssignmentDate = dateBlock.date
        }
    }
}

@Composable
private fun CourseDateItem(
    dateBlock: CourseDateBlock,
    canShowDate: Boolean,
    isMiddleChild: Boolean,
    onItemClick: (CourseDateBlock) -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        if (isMiddleChild) {
            Spacer(modifier = Modifier.height(20.dp))
        }
        if (canShowDate) {
            val timeTitle = if (dateBlock.isTimeDifferenceLessThan24Hours()) {
                TimeUtils.getFormattedTime(dateBlock.date)
            } else {
                TimeUtils.getCourseFormattedDate(LocalContext.current, dateBlock.date)
            }
            Text(
                text = timeTitle,
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textDark,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp)
                .clickable(enabled = dateBlock.blockId.isNotEmpty() && dateBlock.learnerHasAccess,
                    onClick = { onItemClick(dateBlock) })
        ) {
            dateBlock.dateType.drawableResId?.let { icon ->
                Icon(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .align(Alignment.CenterVertically),
                    painter = painterResource(id = if (dateBlock.learnerHasAccess.not()) coreR.drawable.core_ic_lock else icon),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textDark
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                // append assignment type if available with title
                text = if (dateBlock.assignmentType.isNullOrEmpty().not()) {
                    "${dateBlock.assignmentType}: ${dateBlock.title}"
                } else {
                    dateBlock.title
                },
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(7.dp))

            if (dateBlock.blockId.isNotEmpty() && dateBlock.learnerHasAccess) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    tint = MaterialTheme.appColors.textDark,
                    contentDescription = "Open Block Arrow",
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
        if (dateBlock.description.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                text = dateBlock.description,
                style = MaterialTheme.appTypography.labelMedium,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseDatesScreenPreview() {
    OpenEdXTheme {
        CourseDatesScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DatesUIState.Dates(CourseDatesResult(mockedResponse, mockedCourseBannerInfo)),
            uiMessage = null,
            refreshing = false,
            isSelfPaced = true,
            hasInternetConnection = true,
            calendarSyncUIState = mockCalendarSyncUIState,
            onReloadClick = {},
            onSwipeRefresh = {},
            onItemClick = {},
            onPLSBannerViewed = {},
            onSyncDates = {},
            onCalendarSyncSwitch = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseDatesScreenTabletPreview() {
    OpenEdXTheme {
        CourseDatesScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = DatesUIState.Dates(CourseDatesResult(mockedResponse, mockedCourseBannerInfo)),
            uiMessage = null,
            refreshing = false,
            isSelfPaced = true,
            hasInternetConnection = true,
            calendarSyncUIState = mockCalendarSyncUIState,
            onReloadClick = {},
            onSwipeRefresh = {},
            onItemClick = {},
            onPLSBannerViewed = {},
            onSyncDates = {},
            onCalendarSyncSwitch = {},
        )
    }
}

val mockedCourseBannerInfo = CourseDatesBannerInfo(
    missedDeadlines = true,
    missedGatedContent = false,
    verifiedUpgradeLink = "",
    contentTypeGatingEnabled = false,
    hasEnded = false,
)

private val mockedResponse: LinkedHashMap<DatesSection, List<CourseDateBlock>> =
    linkedMapOf(
        Pair(
            DatesSection.COMPLETED, listOf(
                CourseDateBlock(
                    title = "Homework 1: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-20T15:08:07Z")!!,
                )
            )
        ), Pair(
            DatesSection.COMPLETED, listOf(
                CourseDateBlock(
                    title = "Homework 1: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-20T15:08:07Z")!!,
                )
            )
        ), Pair(
            DatesSection.PAST_DUE, listOf(
                CourseDateBlock(
                    title = "Homework 1: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-20T15:08:07Z")!!,
                    dateType = DateType.ASSIGNMENT_DUE_DATE,
                )
            )
        ), Pair(
            DatesSection.TODAY, listOf(
                CourseDateBlock(
                    title = "Homework 2: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-21T15:08:07Z")!!,
                )
            )
        ), Pair(
            DatesSection.THIS_WEEK, listOf(
                CourseDateBlock(
                    title = "Assignment Due: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-22T15:08:07Z")!!,
                    dateType = DateType.ASSIGNMENT_DUE_DATE,
                ), CourseDateBlock(
                    title = "Assignment Due",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-23T15:08:07Z")!!,
                    dateType = DateType.ASSIGNMENT_DUE_DATE,
                ), CourseDateBlock(
                    title = "Surprise Assignment",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-24T15:08:07Z")!!,
                )
            )
        ), Pair(
            DatesSection.NEXT_WEEK, listOf(
                CourseDateBlock(
                    title = "Homework 5: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-25T15:08:07Z")!!,
                )
            )
        ), Pair(
            DatesSection.UPCOMING, listOf(
                CourseDateBlock(
                    title = "Last Assignment",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-26T15:08:07Z")!!,
                    assignmentType = "Module 1",
                    dateType = DateType.VERIFICATION_DEADLINE_DATE,
                )
            )
        )
    )

val mockCalendarSyncUIState = CalendarSyncUIState(
    isCalendarSyncEnabled = true,
    isSynced = true,
    checkForOutOfSync = AtomicReference()
)
