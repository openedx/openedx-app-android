package org.openedx.course.presentation.dates

import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import me.saket.extendedspans.ExtendedSpans
import me.saket.extendedspans.RoundedCornerSpanPainter
import me.saket.extendedspans.drawBehind
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.presentation.course.CourseDatesBadge
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.core.utils.TimeUtils
import org.openedx.course.R
import org.openedx.course.presentation.CourseRouter
import java.util.Date

class CourseDatesFragment : Fragment() {

    private val viewModel by viewModel<CourseDatesViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        with(requireArguments()) {
            viewModel.courseTitle = getString(ARG_TITLE, "")
        }
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

                CourseDatesScreen(windowSize = windowSize,
                    uiState = uiState,
                    courseTitle = viewModel.courseTitle,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    onReloadClick = {
                        viewModel.getCourseDates()
                    },
                    onSwipeRefresh = {
                        viewModel.getCourseDates()
                    },
                    onItemClick = { blockId ->
                        if (blockId.isNotEmpty()) {
                            viewModel.getVerticalBlock(blockId)?.let { verticalBlock ->
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
                        }
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    })
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        fun newInstance(courseId: String, title: String): CourseDatesFragment {
            val fragment = CourseDatesFragment()
            fragment.arguments = bundleOf(ARG_COURSE_ID to courseId, ARG_TITLE to title)
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CourseDatesScreen(
    windowSize: WindowSize,
    uiState: DatesUIState?,
    courseTitle: String,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    onReloadClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onItemClick: (String) -> Unit,
    onBackClick: () -> Unit
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

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(), contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifierScreenWidth
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(1f), contentAlignment = Alignment.CenterStart
                ) {
                    BackBtn {
                        onBackClick()
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        text = courseTitle,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .pullRefresh(pullRefreshState)
                    ) {
                        uiState?.let {
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
                                            .padding(10.dp),
                                        contentPadding = listBottomPadding
                                    ) {
                                        itemsIndexed(uiState.courseDates.keys.toList()) { dateIndex, _ ->
                                            CourseDateBlockSection(
                                                courseDates = uiState.courseDates,
                                                dateIndex = dateIndex,
                                                onItemClick = onItemClick
                                            )
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
                }
            }
        }
    }
}

@Composable
private fun CourseDateBlockSection(
    courseDates: LinkedHashMap<String, ArrayList<CourseDateBlock>>,
    dateIndex: Int,
    onItemClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(intrinsicSize = IntrinsicSize.Min) // this make height of all cards to the tallest card.
            .background(MaterialTheme.appColors.background)
    ) {
        val dateBlockKey = courseDates.keys.toList()[dateIndex]
        val dateBlocks = courseDates[dateBlockKey]
        dateBlocks?.let {
            val dateBlockItem = courseDates[dateBlockKey]?.get(0)
            dateBlockItem?.let {
                DateBullet(
                    isFirstIndex = dateIndex == 0,
                    isLastIndex = dateIndex == courseDates.size - 1,
                    dateBlock = dateBlockItem
                )
                DateBlock(dateBlocks, onItemClick)
            }
        }
    }
}

@Composable
private fun DateBullet(
    isFirstIndex: Boolean = false,
    isLastIndex: Boolean = false,
    dateBlock: CourseDateBlock
) {
    Column(
        modifier = Modifier
            .width(40.dp)
            .padding(start = 6.dp)
    ) {
        if (!isFirstIndex) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(6.dp)
                    .background(color = MaterialTheme.appColors.datesBadgeTextToday)
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
        var circleColor: Color = MaterialTheme.appColors.datesBadgeDefault
        var circleSize: Dp = 10.dp
        when (dateBlock.dateBlockBadge) {

            CourseDatesBadge.TODAY -> {
                circleColor = MaterialTheme.appColors.datesBadgeToday
                circleSize = 14.dp
            }

            CourseDatesBadge.PAST_DUE -> {
                circleColor = MaterialTheme.appColors.datesBadgePastDue
            }

            CourseDatesBadge.BLANK,
            CourseDatesBadge.COMPLETED,
            CourseDatesBadge.DUE_NEXT,
            CourseDatesBadge.NOT_YET_RELEASED,
            CourseDatesBadge.COURSE_EXPIRED_DATE,
            CourseDatesBadge.VERIFIED_ONLY -> {
                var isDatePassed = false
                dateBlock.date?.let {
                    isDatePassed = TimeUtils.isDatePassed(Date(), it)
                }
                circleColor =
                    if (isDatePassed && (dateBlock.dateBlockBadge == CourseDatesBadge.VERIFIED_ONLY).not()) {
                        MaterialTheme.appColors.datesBadgePastDue
                    } else {
                        MaterialTheme.appColors.datesBadgeTextToday
                    }
            }

            else -> {}
        }
        Box(
            modifier = Modifier
                .size(circleSize)
                .border(1.dp, MaterialTheme.appColors.datesBadgeTextToday, CircleShape)
                .clip(CircleShape)
                .background(circleColor)
                .align(Alignment.CenterHorizontally)
        )
        if (!isLastIndex) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(color = MaterialTheme.appColors.datesBadgeTextToday)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun DateBlock(dateBlocks: ArrayList<CourseDateBlock>, onItemClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 30.dp)
    ) {
        val firstDateBlock = dateBlocks[0]
        PlaceDateBadge(
            title = TimeUtils.formatDate(TimeUtils.FORMAT_DATE_TAB, firstDateBlock.date),
            titleSize = 18.sp,
            blockBadge = firstDateBlock.dateBlockBadge
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val parentBadgeAdded = hasSameDateTypes(dateBlocks)
            dateBlocks.forEach { courseDateItem ->
                CourseDateItem(courseDateItem, parentBadgeAdded, onItemClick)
            }
        }
    }
}

/**
 * Method to create the Date badge as per given DateType
 */
@Composable
private fun PlaceDateBadge(title: String, titleSize: TextUnit, blockBadge: CourseDatesBadge) {
    var badgeBackground: Color = Color.Transparent
    var textAppearance: Color = Color.Transparent
    var badgeStrokeColor: Color = Color.Transparent
    var badgeIcon: Painter? = null
    when (blockBadge) {
        CourseDatesBadge.TODAY -> {
            badgeBackground = MaterialTheme.appColors.datesBadgeToday
            textAppearance = MaterialTheme.appColors.datesBadgeTextToday
        }

        CourseDatesBadge.VERIFIED_ONLY -> {
            badgeBackground = MaterialTheme.appColors.datesBadgeTextToday
            textAppearance = MaterialTheme.appColors.datesBadgeTextDue
            badgeIcon = painterResource(R.drawable.ic_lock)
        }

        CourseDatesBadge.COMPLETED -> {
            badgeBackground = MaterialTheme.appColors.datesBadgeDefault
            textAppearance = MaterialTheme.appColors.datesBadgeTextDefault
        }

        CourseDatesBadge.PAST_DUE -> {
            badgeBackground = MaterialTheme.appColors.datesBadgePastDue
            textAppearance = MaterialTheme.appColors.datesBadgeTextDefault
        }

        CourseDatesBadge.DUE_NEXT -> {
            badgeBackground = MaterialTheme.appColors.datesBadgeDue
            textAppearance = MaterialTheme.appColors.datesBadgeTextDue
        }

        CourseDatesBadge.NOT_YET_RELEASED -> {
            badgeBackground = Color.Transparent
            textAppearance = MaterialTheme.appColors.datesBadgeDue
            badgeStrokeColor = MaterialTheme.appColors.datesBadgeDue
        }

        else -> {}
    }
    val extendedSpans = remember {
        ExtendedSpans(
            RoundedCornerSpanPainter(
                cornerRadius = 6.sp,
                padding = RoundedCornerSpanPainter.TextPaddingValues(
                    horizontal = 8.sp,
                    vertical = 6.sp
                ), topMargin = 5.sp,
                bottomMargin = 4.sp,
                stroke = RoundedCornerSpanPainter.Stroke(
                    color = badgeStrokeColor
                )
            )
        )
    }
    val titleWithBadge = buildAnnotatedString {
        append(title)
        append("   ")
        withStyle(
            SpanStyle(
                color = textAppearance,
                background = badgeBackground,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp
            )
        ) {
            if (badgeIcon != null) {
                appendInlineContent("icon_id")
                append(" ")
            }
            val badgeTitle = blockBadge.getStringResIdForDateType()
            if (badgeTitle != -1) {
                append(stringResource(id = badgeTitle))
            }
        }
    }
    val inlineContent = HashMap<String, InlineTextContent>()
    badgeIcon?.let {
        inlineContent["icon_id"] = InlineTextContent(
            Placeholder(
                width = 16.sp,
                height = 16.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Icon(badgeIcon, "", tint = MaterialTheme.appColors.datesBadgeTextDue)
        }
    }
    Text(
        modifier = Modifier.drawBehind(extendedSpans),
        text = remember(titleWithBadge) {
            extendedSpans.extend(titleWithBadge)
        },
        onTextLayout = { result ->
            extendedSpans.onTextLayout(result)
        },
        inlineContent = inlineContent,
        fontSize = titleSize,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    )
}

@Composable
private fun CourseDateItem(
    courseDateItem: CourseDateBlock,
    parentBadgeAdded: Boolean,
    onItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (!parentBadgeAdded) {
            // Set update badge with sub date items
            PlaceDateBadge(courseDateItem.title, 16.sp, courseDateItem.dateBlockBadge)
        } else {
            Text(
                modifier = Modifier.clickable {
                    onItemClick(courseDateItem.blockId)
                },
                text = courseDateItem.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                textDecoration = if (courseDateItem.blockId.isNotEmpty()) TextDecoration.Underline else TextDecoration.None
            )
        }
        if (!TextUtils.isEmpty(courseDateItem.description)) {
            Text(
                text = courseDateItem.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Method to check that all Date Items have same badge status or not
 *
 * @return true if all the date items have update badge status else false
 * */
private fun hasSameDateTypes(dateBlockItems: ArrayList<CourseDateBlock>?): Boolean {
    if (!dateBlockItems.isNullOrEmpty() && dateBlockItems.size > 1) {
        val dateType = dateBlockItems.first().dateBlockBadge
        for (i in 1 until dateBlockItems.size) {
            if (dateBlockItems[i].dateBlockBadge != dateType && dateBlockItems[i].dateBlockBadge != CourseDatesBadge.BLANK) {
                return false
            }
        }
    }
    return true
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseDatesScreenPreview() {
    OpenEdXTheme {
        CourseDatesScreen(windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DatesUIState.Dates(mockedCourseDates),
            courseTitle = "Course Dates",
            uiMessage = null,
            hasInternetConnection = true,
            refreshing = false,
            onSwipeRefresh = {},
            onReloadClick = {},
            onItemClick = {},
            onBackClick = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseDatesScreenTabletPreview() {
    OpenEdXTheme {
        CourseDatesScreen(windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = DatesUIState.Dates(mockedCourseDates),
            courseTitle = "Course Dates",
            uiMessage = null,
            hasInternetConnection = true,
            refreshing = false,
            onSwipeRefresh = {},
            onReloadClick = {},
            onItemClick = {},
            onBackClick = {})
    }
}

private var mockedCourseDates = linkedMapOf(
    Pair(
        "2023-10-20T15:08:07Z", arrayListOf(
            CourseDateBlock(
                title = "Course Start",
                description = "After this date, course content will be archived",
                date = TimeUtils.iso8601ToDate("2023-10-20T15:08:07Z"),
                dateBlockBadge = CourseDatesBadge.PAST_DUE
            )
        )
    ), Pair(
        "2023-10-21T15:08:07Z", arrayListOf(
            CourseDateBlock(
                title = "Today",
                description = "After this date, course content will be archived",
                date = TimeUtils.iso8601ToDate("2023-10-21T15:08:07Z"),
                dateBlockBadge = CourseDatesBadge.TODAY
            )
        )
    ),
    Pair(
        "2023-10-22T15:08:07Z", arrayListOf(
            CourseDateBlock(
                title = "Due Next",
                description = "After this date, course content will be archived",
                date = TimeUtils.iso8601ToDate("2023-10-22T15:08:07Z"),
                dateBlockBadge = CourseDatesBadge.DUE_NEXT
            )
        )
    ), Pair(
        "2023-10-23T15:08:07Z", arrayListOf(
            CourseDateBlock(
                title = "Assignment Due",
                description = "After this date, course content will be archived",
                date = TimeUtils.iso8601ToDate("2023-10-23T15:08:07Z"),
                dateBlockBadge = CourseDatesBadge.VERIFIED_ONLY
            )
        )
    ), Pair(
        "2023-10-24T15:08:07Z", arrayListOf(
            CourseDateBlock(
                title = "Not Yet Released",
                description = "After this date, course content will be archived",
                date = TimeUtils.iso8601ToDate("2023-10-24T15:08:07Z"),
                dateBlockBadge = CourseDatesBadge.NOT_YET_RELEASED
            )
        )
    ), Pair(
        "2023-10-25T15:08:07Z", arrayListOf(
            CourseDateBlock(
                title = "Blank",
                description = "After this date, course content will be archived",
                date = TimeUtils.iso8601ToDate("2023-10-25T15:08:07Z"),
                dateBlockBadge = CourseDatesBadge.BLANK
            )
        )
    ), Pair(
        "2023-10-26T15:08:07Z", arrayListOf(
            CourseDateBlock(
                title = "Course End",
                description = "After this date, course content will be archived",
                date = TimeUtils.iso8601ToDate("2023-10-26T15:08:07Z"),
                dateBlockBadge = CourseDatesBadge.COMPLETED
            )
        )
    )
)
