package org.openedx.course.presentation.outline

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.AndroidUriHandler
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
import org.openedx.core.BlockType
import org.openedx.core.NoContentScreenType
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.OfflineDownload
import org.openedx.core.domain.model.Progress
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.NoContentScreen
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.presentation.ui.CourseDatesBanner
import org.openedx.course.presentation.ui.CourseDatesBannerTablet
import org.openedx.course.presentation.ui.CourseMessage
import org.openedx.course.presentation.ui.CourseSection
import org.openedx.foundation.extension.takeIfNotEmpty
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import java.util.Date

@Composable
fun CourseOutlineScreen(
    windowSize: WindowSize,
    viewModel: CourseOutlineViewModel,
    fragmentManager: FragmentManager,
    onResetDatesClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState(null)
    val resumeBlockId by viewModel.resumeBlockId.collectAsState("")
    val context = LocalContext.current

    LaunchedEffect(resumeBlockId) {
        if (resumeBlockId.isNotEmpty()) {
            viewModel.openBlock(fragmentManager, resumeBlockId)
        }
    }

    CourseOutlineUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = uiMessage,
        onExpandClick = { block ->
            if (viewModel.switchCourseSections(block.id)) {
                viewModel.sequentialClickedEvent(
                    block.blockId,
                    block.displayName
                )
            }
        },
        onSubSectionClick = { subSectionBlock ->
            if (viewModel.isCourseDropdownNavigationEnabled) {
                viewModel.courseSubSectionUnit[subSectionBlock.id]?.let { unit ->
                    viewModel.logUnitDetailViewedEvent(
                        unit.blockId,
                        unit.displayName
                    )
                    viewModel.courseRouter.navigateToCourseContainer(
                        fragmentManager,
                        courseId = viewModel.courseId,
                        unitId = unit.id,
                        mode = CourseViewMode.FULL
                    )
                }
            } else {
                viewModel.sequentialClickedEvent(
                    subSectionBlock.blockId,
                    subSectionBlock.displayName
                )
                viewModel.courseRouter.navigateToCourseSubsections(
                    fm = fragmentManager,
                    courseId = viewModel.courseId,
                    subSectionId = subSectionBlock.id,
                    mode = CourseViewMode.FULL
                )
            }
        },
        onResumeClick = { componentId ->
            viewModel.openBlock(
                fragmentManager,
                componentId
            )
        },
        onDownloadClick = { blocksIds ->
            viewModel.downloadBlocks(
                blocksIds = blocksIds,
                fragmentManager = fragmentManager,
            )
        },
        onResetDatesClick = {
            viewModel.resetCourseDatesBanner(
                onResetDates = {
                    onResetDatesClick()
                }
            )
        },
        onCertificateClick = {
            viewModel.viewCertificateTappedEvent()
            it.takeIfNotEmpty()
                ?.let { url -> AndroidUriHandler(context).openUri(url) }
        }
    )
}

@Composable
private fun CourseOutlineUI(
    windowSize: WindowSize,
    uiState: CourseOutlineUIState,
    uiMessage: UIMessage?,
    onExpandClick: (Block) -> Unit,
    onSubSectionClick: (Block) -> Unit,
    onResumeClick: (String) -> Unit,
    onDownloadClick: (blockIds: List<String>) -> Unit,
    onResetDatesClick: () -> Unit,
    onCertificateClick: (String) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

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
                            if (uiState.courseStructure.blockData.isEmpty()) {
                                NoContentScreen(noContentScreenType = NoContentScreenType.COURSE_OUTLINE)
                            } else {
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

                                    val certificate = uiState.courseStructure.certificate
                                    if (certificate?.isCertificateEarned() == true) {
                                        item {
                                            CourseMessage(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp)
                                                    .then(listPadding),
                                                icon = painterResource(R.drawable.course_ic_certificate),
                                                message = stringResource(
                                                    R.string.course_you_earned_certificate,
                                                    uiState.courseStructure.name
                                                ),
                                                action = stringResource(R.string.course_view_certificate),
                                                onActionClick = {
                                                    onCertificateClick(
                                                        certificate.certificateURL ?: ""
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    val progress = uiState.courseStructure.progress
                                    if (progress != null && progress.totalAssignmentsCount > 0) {
                                        item {
                                            CourseProgress(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        top = 16.dp,
                                                        start = 24.dp,
                                                        end = 24.dp
                                                    ),
                                                progress = progress
                                            )
                                        }
                                    }

                                    if (uiState.resumeComponent != null) {
                                        item {
                                            Box(listPadding) {
                                                if (windowSize.isTablet) {
                                                    ResumeCourseTablet(
                                                        modifier = Modifier.padding(vertical = 16.dp),
                                                        block = uiState.resumeComponent,
                                                        displayName = uiState.resumeUnitTitle,
                                                        onResumeClick = onResumeClick
                                                    )
                                                } else {
                                                    ResumeCourse(
                                                        modifier = Modifier.padding(vertical = 16.dp),
                                                        block = uiState.resumeComponent,
                                                        displayName = uiState.resumeUnitTitle,
                                                        onResumeClick = onResumeClick
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    uiState.courseStructure.blockData.forEach { section ->
                                        val courseSubSections =
                                            uiState.courseSubSections[section.id]
                                        val courseSectionsState =
                                            uiState.courseSectionsState[section.id]

                                        item {
                                            CourseSection(
                                                modifier = listPadding.padding(vertical = 4.dp),
                                                block = section,
                                                onItemClick = onExpandClick,
                                                useRelativeDates = uiState.useRelativeDates,
                                                courseSectionsState = courseSectionsState,
                                                courseSubSections = courseSubSections,
                                                downloadedStateMap = uiState.downloadedState,
                                                onSubSectionClick = onSubSectionClick,
                                                onDownloadClick = onDownloadClick
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CourseOutlineUIState.Error -> {
                            NoContentScreen(noContentScreenType = NoContentScreenType.COURSE_OUTLINE)
                        }

                        CourseOutlineUIState.Loading -> {
                            CircularProgress()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumeCourse(
    modifier: Modifier = Modifier,
    block: Block,
    displayName: String,
    onResumeClick: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.course_continue_with),
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
                text = displayName,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(24.dp))
        OpenEdXButton(
            text = stringResource(id = R.string.course_resume),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                TextIcon(
                    text = stringResource(id = R.string.course_resume),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    color = MaterialTheme.appColors.primaryButtonText,
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
    displayName: String,
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
                text = stringResource(id = R.string.course_continue_with),
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textPrimaryVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(size = (MaterialTheme.appTypography.titleMedium.fontSize.value + 4).dp),
                    painter = painterResource(id = getUnitBlockIcon(block)),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimary
                )
                Text(
                    text = displayName,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4
                )
            }
        }
        OpenEdXButton(
            modifier = Modifier.width(210.dp),
            text = stringResource(id = R.string.course_resume),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                TextIcon(
                    text = stringResource(id = R.string.course_resume),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    color = MaterialTheme.appColors.primaryButtonText,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        )
    }
}

@Composable
private fun CourseProgress(
    modifier: Modifier = Modifier,
    progress: Progress,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            progress = progress.value,
            color = MaterialTheme.appColors.progressBarColor,
            backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
        )
        Text(
            text = pluralStringResource(
                R.plurals.course_assignments_complete,
                progress.assignmentsCompleted,
                progress.assignmentsCompleted,
                progress.totalAssignmentsCount
            ),
            color = MaterialTheme.appColors.textDark,
            style = MaterialTheme.appTypography.labelSmall
        )
    }
}

fun getUnitBlockIcon(block: Block): Int {
    return when (block.type) {
        BlockType.VIDEO -> R.drawable.course_ic_video
        BlockType.PROBLEM -> R.drawable.course_ic_pen
        BlockType.DISCUSSION -> R.drawable.course_ic_discussion
        else -> R.drawable.course_ic_block
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseOutlineScreenPreview() {
    OpenEdXTheme {
        CourseOutlineUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseOutlineUIState.CourseData(
                mockCourseStructure,
                mapOf(),
                mockChapterBlock,
                "Resumed Unit",
                mapOf(),
                mapOf(),
                mapOf(),
                CourseDatesBannerInfo(
                    missedDeadlines = false,
                    missedGatedContent = false,
                    verifiedUpgradeLink = "",
                    contentTypeGatingEnabled = false,
                    hasEnded = false
                ),
                true
            ),
            uiMessage = null,
            onExpandClick = {},
            onSubSectionClick = {},
            onResumeClick = {},
            onDownloadClick = {},
            onResetDatesClick = {},
            onCertificateClick = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseOutlineScreenTabletPreview() {
    OpenEdXTheme {
        CourseOutlineUI(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseOutlineUIState.CourseData(
                mockCourseStructure,
                mapOf(),
                mockChapterBlock,
                "Resumed Unit",
                mapOf(),
                mapOf(),
                mapOf(),
                CourseDatesBannerInfo(
                    missedDeadlines = false,
                    missedGatedContent = false,
                    verifiedUpgradeLink = "",
                    contentTypeGatingEnabled = false,
                    hasEnded = false
                ),
                true
            ),
            uiMessage = null,
            onExpandClick = {},
            onSubSectionClick = {},
            onResumeClick = {},
            onDownloadClick = {},
            onResetDatesClick = {},
            onCertificateClick = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ResumeCoursePreview() {
    OpenEdXTheme {
        ResumeCourse(block = mockChapterBlock, displayName = "Resumed Unit") {}
    }
}

private val mockAssignmentProgress = AssignmentProgress(
    assignmentType = "Home",
    numPointsEarned = 1f,
    numPointsPossible = 3f
)
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
    containsGatedContent = false,
    assignmentProgress = mockAssignmentProgress,
    due = Date(),
    offlineDownload = null
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
    containsGatedContent = false,
    assignmentProgress = mockAssignmentProgress,
    due = Date(),
    offlineDownload = OfflineDownload("fileUrl", "", 1),
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
    isSelfPaced = false,
    progress = Progress(1, 3),
)
