package org.openedx.course.presentation.outline

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.CourseDatesBanner
import org.openedx.course.presentation.ui.CourseDatesBannerTablet
import org.openedx.course.presentation.ui.CourseExpandableChapterCard
import org.openedx.course.presentation.ui.CourseSectionCard
import org.openedx.course.presentation.ui.CourseSubSectionItem
import org.openedx.course.presentation.ui.DatesShiftedSnackBar
import java.io.File
import java.util.Date

fun getUnitBlockIcon(block: Block): Int {
    return when (block.type) {
        BlockType.VIDEO -> org.openedx.course.R.drawable.ic_course_video
        BlockType.PROBLEM -> org.openedx.course.R.drawable.ic_course_pen
        BlockType.DISCUSSION -> org.openedx.course.R.drawable.ic_course_discussion
        else -> org.openedx.course.R.drawable.ic_course_block
    }
}

@Composable
fun CourseOutlineScreen(
    windowSize: WindowSize,
    courseOutlineViewModel: CourseOutlineViewModel,
    courseRouter: CourseRouter,
    fragmentManager: FragmentManager,
    onReloadClick: () -> Unit,
    onResetDatesClick: () -> Unit,
    onViewDates: () -> Unit
) {
    val uiState by courseOutlineViewModel.uiState.observeAsState(CourseOutlineUIState.Loading)
    val uiMessage by courseOutlineViewModel.uiMessage.collectAsState(null)
    val context = LocalContext.current

    CourseOutlineScreen(
        windowSize = windowSize,
        uiState = uiState,
        isCourseNestedListEnabled = courseOutlineViewModel.isCourseNestedListEnabled,
        uiMessage = uiMessage,
        hasInternetConnection = courseOutlineViewModel.hasInternetConnection,
        onReloadClick = onReloadClick,
        onItemClick = { block ->
            courseOutlineViewModel.sequentialClickedEvent(
                block.blockId,
                block.displayName
            )
            courseRouter.navigateToCourseSubsections(
                fm = fragmentManager,
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
                    fragmentManager,
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
                            fm = fragmentManager,
                            courseId = courseOutlineViewModel.courseId,
                            unitId = unit.id,
                            componentId = componentId,
                            mode = CourseViewMode.FULL
                        )
                    } else {
                        courseRouter.navigateToCourseSubsections(
                            fragmentManager,
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
                    fm = fragmentManager,
                    courseOutlineViewModel.getDownloadableChildren(it.id)
                        ?: arrayListOf()
                )
            } else if (courseOutlineViewModel.isBlockDownloaded(it.id)) {
                courseOutlineViewModel.removeDownloadModels(it.id)
            } else {
                courseOutlineViewModel.saveDownloadModels(
                    context.externalCacheDir.toString() +
                            File.separator +
                            context
                                .getString(R.string.app_name)
                                .replace(Regex("\\s"), "_"), it.id
                )
            }
        },
        onResetDatesClick = onResetDatesClick,
        onViewDates = onViewDates,
        onCertificateClick = {
            courseOutlineViewModel.viewCertificateTappedEvent()
            it.takeIfNotEmpty()
                ?.let { url -> AndroidUriHandler(context).openUri(url) }
        }
    )
}

@Composable
internal fun CourseOutlineScreen(
    windowSize: WindowSize,
    uiState: CourseOutlineUIState,
    isCourseNestedListEnabled: Boolean,
    uiMessage: UIMessage?,
    hasInternetConnection: Boolean,
    onReloadClick: () -> Unit,
    onItemClick: (Block) -> Unit,
    onExpandClick: (Block) -> Unit,
    onSubSectionClick: (Block) -> Unit,
    onResumeClick: (String) -> Unit,
    onDownloadClick: (Block) -> Unit,
    onResetDatesClick: () -> Unit,
    onViewDates: () -> Unit?,
    onCertificateClick: (String) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) {

        val screenWidth by remember(key1 = windowSize) {
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

        val listPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.padding(horizontal = 6.dp),
                    compact = Modifier.padding(horizontal = 24.dp)
                )
            )
        }

        val snackState = remember { SnackbarHostState() }
        if (uiMessage is DatesShiftedSnackBar) {
            val datesShiftedMessage =
                stringResource(id = org.openedx.course.R.string.course_dates_shifted_message)
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
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = screenWidth,
                color = MaterialTheme.appColors.background
            ) {
                Box {
                    when (uiState) {
                        is CourseOutlineUIState.CourseData -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = listBottomPadding
                            ) {
                                if (uiState.datesBannerInfo.isBannerAvailableForDashboard()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .padding(all = 8.dp)
                                        ) {
                                            if (windowSize.isTablet) {
                                                CourseDatesBannerTablet(
                                                    banner = uiState.datesBannerInfo,
                                                    resetDates = onResetDatesClick,
                                                )
                                            } else {
                                                CourseDatesBanner(
                                                    banner = uiState.datesBannerInfo,
                                                    resetDates = onResetDatesClick,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (uiState.resumeComponent != null) {
                                    item {
                                        Box(listPadding) {
                                            if (windowSize.isTablet) {
                                                ResumeCourseTablet(
                                                    modifier = Modifier.padding(vertical = 16.dp),
                                                    block = uiState.resumeComponent,
                                                    onResumeClick = onResumeClick
                                                )
                                            } else {
                                                ResumeCourse(
                                                    modifier = Modifier.padding(vertical = 16.dp),
                                                    block = uiState.resumeComponent,
                                                    onResumeClick = onResumeClick
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isCourseNestedListEnabled) {
                                    uiState.courseStructure.blockData.forEach { section ->
                                        val courseSubSections =
                                            uiState.courseSubSections[section.id]
                                        val courseSectionsState =
                                            uiState.courseSectionsState[section.id]

                                        item {
                                            Column {
                                                CourseExpandableChapterCard(
                                                    modifier = listPadding,
                                                    block = section,
                                                    onItemClick = onExpandClick,
                                                    arrowDegrees = if (courseSectionsState == true) -90f else 90f
                                                )
                                                Divider()
                                            }
                                        }

                                        courseSubSections?.forEach { subSectionBlock ->
                                            item {
                                                Column {
                                                    AnimatedVisibility(
                                                        visible = courseSectionsState == true
                                                    ) {
                                                        Column {
                                                            val downloadsCount =
                                                                uiState.subSectionsDownloadsCount[subSectionBlock.id]
                                                                    ?: 0

                                                            CourseSubSectionItem(
                                                                modifier = listPadding,
                                                                block = subSectionBlock,
                                                                downloadedState = uiState.downloadedState[subSectionBlock.id],
                                                                downloadsCount = downloadsCount,
                                                                onClick = onSubSectionClick,
                                                                onDownloadClick = onDownloadClick
                                                            )
                                                            Divider()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    return@LazyColumn
                                }

                                items(uiState.courseStructure.blockData) { block ->
                                    Column(listPadding) {
                                        if (block.type == BlockType.CHAPTER) {
                                            Text(
                                                modifier = Modifier.padding(
                                                    top = 36.dp,
                                                    bottom = 8.dp
                                                ),
                                                text = block.displayName,
                                                style = MaterialTheme.appTypography.titleMedium,
                                                color = MaterialTheme.appColors.textPrimaryVariant
                                            )
                                        } else {
                                            CourseSectionCard(
                                                block = block,
                                                downloadedState = uiState.downloadedState[block.id],
                                                onItemClick = onItemClick,
                                                onDownloadClick = onDownloadClick
                                            )
                                            Divider()
                                        }
                                    }
                                }
                            }
                        }

                        CourseOutlineUIState.Loading -> {}
                    }
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

                SnackbarHost(
                    modifier = Modifier.align(Alignment.BottomStart),
                    hostState = snackState
                ) { snackbarData: SnackbarData ->
                    DatesShiftedSnackBar(showAction = true,
                        onViewDates = onViewDates,
                        onClose = {
                            snackbarData.dismiss()
                        })
                }
            }
        }
    }
}

@Composable
private fun ResumeCourse(
    modifier: Modifier = Modifier,
    block: Block,
    onResumeClick: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = org.openedx.course.R.string.course_continue_with),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textPrimaryVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = getUnitBlockIcon(block)),
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary
            )
            Text(
                text = block.displayName,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(24.dp))
        OpenEdXButton(
            text = stringResource(id = org.openedx.course.R.string.course_resume),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                TextIcon(
                    text = stringResource(id = org.openedx.course.R.string.course_resume),
                    painter = painterResource(id = R.drawable.core_ic_forward),
                    color = MaterialTheme.appColors.buttonText,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        )
    }
}


@Composable
private fun ResumeCourseTablet(
    modifier: Modifier = Modifier,
    block: Block,
    onResumeClick: (String) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 35.dp)
        ) {
            Text(
                text = stringResource(id = org.openedx.course.R.string.course_continue_with),
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textPrimaryVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size((MaterialTheme.appTypography.titleMedium.fontSize.value + 4).dp),
                    painter = painterResource(id = getUnitBlockIcon(block)),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimary
                )
                Text(
                    text = block.displayName,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4
                )
            }
        }
        OpenEdXButton(
            modifier = Modifier.width(210.dp),
            text = stringResource(id = org.openedx.course.R.string.course_resume),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                TextIcon(
                    text = stringResource(id = org.openedx.course.R.string.course_resume),
                    painter = painterResource(id = R.drawable.core_ic_forward),
                    color = MaterialTheme.appColors.buttonText,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseOutlineScreenPreview() {
    OpenEdXTheme {
        CourseOutlineScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseOutlineUIState.CourseData(
                mockCourseStructure,
                mapOf(),
                mockChapterBlock,
                mapOf(),
                mapOf(),
                mapOf(),
                CourseDatesBannerInfo(
                    missedDeadlines = false,
                    missedGatedContent = false,
                    verifiedUpgradeLink = "",
                    contentTypeGatingEnabled = false,
                    hasEnded = false
                )
            ),
            isCourseNestedListEnabled = true,
            uiMessage = null,
            hasInternetConnection = true,
            onItemClick = {},
            onExpandClick = {},
            onSubSectionClick = {},
            onResumeClick = {},
            onReloadClick = {},
            onDownloadClick = {},
            onResetDatesClick = {},
            onViewDates = {},
            onCertificateClick = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseOutlineScreenTabletPreview() {
    OpenEdXTheme {
        CourseOutlineScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseOutlineUIState.CourseData(
                mockCourseStructure,
                mapOf(),
                mockChapterBlock,
                mapOf(),
                mapOf(),
                mapOf(),
                CourseDatesBannerInfo(
                    missedDeadlines = false,
                    missedGatedContent = false,
                    verifiedUpgradeLink = "",
                    contentTypeGatingEnabled = false,
                    hasEnded = false
                )
            ),
            isCourseNestedListEnabled = true,
            uiMessage = null,
            hasInternetConnection = true,
            onItemClick = {},
            onExpandClick = {},
            onSubSectionClick = {},
            onResumeClick = {},
            onReloadClick = {},
            onDownloadClick = {},
            onResetDatesClick = {},
            onViewDates = {},
            onCertificateClick = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ResumeCoursePreview() {
    OpenEdXTheme {
        ResumeCourse(block = mockChapterBlock) {}
    }
}

private val mockChapterBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.CHAPTER,
    displayName = "Chapter",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    descendantsType = BlockType.CHAPTER,
    completion = 0.0,
    containsGatedContent = false
)
private val mockSequentialBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.SEQUENTIAL,
    displayName = "Sequential",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    descendantsType = BlockType.CHAPTER,
    completion = 0.0,
    containsGatedContent = false
)

private val mockCourseStructure = CourseStructure(
    root = "",
    blockData = listOf(mockSequentialBlock, mockSequentialBlock),
    id = "id",
    name = "Course name",
    number = "",
    org = "Org",
    start = Date(),
    startDisplay = "",
    startType = "",
    end = Date(),
    coursewareAccess = CoursewareAccess(
        true,
        "",
        "",
        "",
        "",
        ""
    ),
    media = null,
    certificate = null,
    isSelfPaced = false
)
