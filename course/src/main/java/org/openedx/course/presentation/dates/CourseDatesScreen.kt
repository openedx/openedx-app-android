package org.openedx.course.presentation.dates

import android.content.res.Configuration
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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.NoContentScreenType
import org.openedx.core.data.model.DateType
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.presentation.CoreAnalyticsScreen
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncState
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.NoContentScreen
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.core.utils.TimeUtils.formatToString
import org.openedx.core.utils.clearTime
import org.openedx.course.presentation.ui.CourseDatesBanner
import org.openedx.course.presentation.ui.CourseDatesBannerTablet
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.foundation.extension.isNotEmptyThenLet
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import java.util.Date
import org.openedx.core.R as CoreR

@Composable
fun CourseDatesScreen(
    windowSize: WindowSize,
    viewModel: CourseDatesViewModel,
    fragmentManager: FragmentManager,
    isFragmentResumed: Boolean,
    updateCourseStructure: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState(CourseDatesUIState.Loading)
    val uiMessage by viewModel.uiMessage.collectAsState(null)
    val context = LocalContext.current

    CourseDatesUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = uiMessage,
        isSelfPaced = viewModel.isSelfPaced,
        useRelativeDates = viewModel.useRelativeDates,
        onItemClick = { block ->
            if (block.blockId.isNotEmpty()) {
                viewModel.getVerticalBlock(block.blockId)
                    ?.let { verticalBlock ->
                        viewModel.logCourseComponentTapped(true, block)
                        if (viewModel.isCourseExpandableSectionsEnabled) {
                            viewModel.courseRouter.navigateToCourseContainer(
                                fm = fragmentManager,
                                courseId = viewModel.courseId,
                                unitId = verticalBlock.id,
                                componentId = "",
                                mode = CourseViewMode.FULL
                            )
                        } else {
                            viewModel.getSequentialBlock(verticalBlock.id)
                                ?.let { sequentialBlock ->
                                    viewModel.courseRouter.navigateToCourseSubsections(
                                        fm = fragmentManager,
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
                        title = context.getString(CoreR.string.core_leaving_the_app),
                        message = context.getString(
                            CoreR.string.core_leaving_the_app_message,
                            context.getString(CoreR.string.platform_name)
                        ),
                        url = block.link,
                        source = CoreAnalyticsScreen.COURSE_DATES.screenName
                    ).show(
                        fragmentManager,
                        ActionDialogFragment::class.simpleName
                    )
                }
            }
        },
        onPLSBannerViewed = {
            if (isFragmentResumed) {
                viewModel.logPlsBannerViewed()
            }
        },
        onSyncDates = {
            viewModel.logPlsShiftButtonClicked()
            viewModel.resetCourseDatesBanner {
                viewModel.logPlsShiftDates(it)
                if (it) {
                    updateCourseStructure()
                }
            }
        },
        onCalendarSyncStateClick = {
            viewModel.calendarRouter.navigateToCalendarSettings(fragmentManager)
        }
    )
}

@Composable
private fun CourseDatesUI(
    windowSize: WindowSize,
    uiState: CourseDatesUIState,
    uiMessage: UIMessage?,
    isSelfPaced: Boolean,
    useRelativeDates: Boolean,
    onItemClick: (CourseDateBlock) -> Unit,
    onPLSBannerViewed: () -> Unit,
    onSyncDates: () -> Unit,
    onCalendarSyncStateClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

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

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        val isPLSBannerAvailable = (uiState as? CourseDatesUIState.CourseDates)
            ?.courseDatesResult
            ?.courseBanner
            ?.isBannerAvailableForUserType(isSelfPaced)

        LaunchedEffect(key1 = isPLSBannerAvailable) {
            if (isPLSBannerAvailable == true) {
                onPLSBannerViewed()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = modifierScreenWidth,
                color = MaterialTheme.appColors.background,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                ) {
                    when (uiState) {
                        is CourseDatesUIState.CourseDates -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = listBottomPadding
                            ) {
                                val courseBanner = uiState.courseDatesResult.courseBanner
                                val datesSection = uiState.courseDatesResult.datesSection

                                if (courseBanner.isBannerAvailableForUserType(isSelfPaced)) {
                                    item {
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

                                // Handle calendar sync state
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp)
                                            .background(
                                                MaterialTheme.appColors.cardViewBackground,
                                                MaterialTheme.shapes.medium
                                            )
                                            .border(
                                                0.75.dp,
                                                MaterialTheme.appColors.cardViewBorder,
                                                MaterialTheme.shapes.medium
                                            )
                                            .clickable {
                                                onCalendarSyncStateClick()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = 8.dp,
                                                    start = 16.dp,
                                                    end = 8.dp,
                                                    bottom = 8.dp
                                                ),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = uiState.calendarSyncState.icon,
                                                tint = uiState.calendarSyncState.tint,
                                                contentDescription = null
                                            )
                                            Text(
                                                text = stringResource(uiState.calendarSyncState.longTitle),
                                                style = MaterialTheme.appTypography.labelLarge,
                                                color = MaterialTheme.appColors.textDark
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
                                            useRelativeDates = useRelativeDates
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
                                                useRelativeDates = useRelativeDates
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CourseDatesUIState.Error -> {
                            NoContentScreen(noContentScreenType = NoContentScreenType.COURSE_DATES)
                        }

                        CourseDatesUIState.Loading -> {
                            CircularProgress()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableView(
    sectionKey: DatesSection = DatesSection.NONE,
    useRelativeDates: Boolean,
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
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(initialAlpha = 0.3f, animationSpec = tween(durationMillis = 300))
    }
    val exitTransition = remember {
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = 300)
        ) + fadeOut(animationSpec = tween(durationMillis = 300))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.appColors.cardViewBackground, MaterialTheme.shapes.medium)
            .border(0.75.dp, MaterialTheme.appColors.cardViewBorder, MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 8.dp, bottom = 8.dp)
                .clickable { expanded = !expanded }
        ) {
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
                            id = CoreR.plurals.core_date_items_hidden,
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
                useRelativeDates = useRelativeDates
            )
        }
    }
}

@Composable
private fun CourseDateBlockSection(
    sectionKey: DatesSection = DatesSection.NONE,
    useRelativeDates: Boolean,
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
            DateBlock(dateBlocks = sectionDates, onItemClick = onItemClick, useRelativeDates = useRelativeDates)
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
                color = barColor,
                shape = MaterialTheme.shapes.medium
            )
    )
}

@Composable
private fun DateBlock(
    dateBlocks: List<CourseDateBlock>,
    useRelativeDates: Boolean,
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
            CourseDateItem(dateBlock, canShowDate, index != 0, useRelativeDates, onItemClick)
            lastAssignmentDate = dateBlock.date
        }
    }
}

@Composable
private fun CourseDateItem(
    dateBlock: CourseDateBlock,
    canShowDate: Boolean,
    isMiddleChild: Boolean,
    useRelativeDates: Boolean,
    onItemClick: (CourseDateBlock) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        if (isMiddleChild) {
            Spacer(modifier = Modifier.height(20.dp))
        }
        if (canShowDate) {
            val timeTitle = formatToString(context, dateBlock.date, useRelativeDates)
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
                .clickable(
                    enabled = dateBlock.blockId.isNotEmpty() && dateBlock.learnerHasAccess,
                    onClick = { onItemClick(dateBlock) }
                )
        ) {
            dateBlock.dateType.drawableResId?.let { icon ->
                Icon(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .align(Alignment.CenterVertically),
                    painter = painterResource(
                        id = if (dateBlock.learnerHasAccess.not()) {
                            CoreR.drawable.core_ic_lock
                        } else {
                            icon
                        }
                    ),
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
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
private fun EmptyCourseDatesScreenPreview() {
    OpenEdXTheme {
        CourseDatesUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseDatesUIState.Error,
            uiMessage = null,
            isSelfPaced = true,
            useRelativeDates = true,
            onItemClick = {},
            onPLSBannerViewed = {},
            onSyncDates = {},
            onCalendarSyncStateClick = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseDatesScreenPreview() {
    OpenEdXTheme {
        CourseDatesUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseDatesUIState.CourseDates(
                CourseDatesResult(mockedResponse, mockedCourseBannerInfo),
                CalendarSyncState.SYNCED
            ),
            uiMessage = null,
            isSelfPaced = true,
            useRelativeDates = true,
            onItemClick = {},
            onPLSBannerViewed = {},
            onSyncDates = {},
            onCalendarSyncStateClick = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseDatesScreenTabletPreview() {
    OpenEdXTheme {
        CourseDatesUI(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseDatesUIState.CourseDates(
                CourseDatesResult(mockedResponse, mockedCourseBannerInfo),
                CalendarSyncState.SYNCED
            ),
            uiMessage = null,
            isSelfPaced = true,
            useRelativeDates = true,
            onItemClick = {},
            onPLSBannerViewed = {},
            onSyncDates = {},
            onCalendarSyncStateClick = {},
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
            DatesSection.COMPLETED,
            listOf(
                CourseDateBlock(
                    title = "Homework 1: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-20T15:08:07Z")!!,
                )
            )
        ),

        Pair(
            DatesSection.COMPLETED,
            listOf(
                CourseDateBlock(
                    title = "Homework 1: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-20T15:08:07Z")!!,
                )
            )
        ),

        Pair(
            DatesSection.PAST_DUE,
            listOf(
                CourseDateBlock(
                    title = "Homework 1: ABCD",
                    description = "After this date, course content will be archived",
                    date = Date(),
                    dateType = DateType.ASSIGNMENT_DUE_DATE,
                )
            )
        ),

        Pair(
            DatesSection.TODAY,
            listOf(
                CourseDateBlock(
                    title = "Homework 2: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-21T15:08:07Z")!!,
                )
            )
        ),

        Pair(
            DatesSection.THIS_WEEK,
            listOf(
                CourseDateBlock(
                    title = "Assignment Due: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-22T15:08:07Z")!!,
                    dateType = DateType.ASSIGNMENT_DUE_DATE,
                ),

                CourseDateBlock(
                    title = "Assignment Due",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-23T15:08:07Z")!!,
                    dateType = DateType.ASSIGNMENT_DUE_DATE,
                ),

                CourseDateBlock(
                    title = "Surprise Assignment",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-24T15:08:07Z")!!,
                )
            )
        ),

        Pair(
            DatesSection.NEXT_WEEK,
            listOf(
                CourseDateBlock(
                    title = "Homework 5: ABCD",
                    description = "After this date, course content will be archived",
                    date = TimeUtils.iso8601ToDate("2023-10-25T15:08:07Z")!!,
                )
            )
        ),

        Pair(
            DatesSection.UPCOMING,
            listOf(
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
