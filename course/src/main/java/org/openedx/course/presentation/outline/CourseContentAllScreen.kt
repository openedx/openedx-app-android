package org.openedx.course.presentation.outline

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.BlockType
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.OfflineDownload
import org.openedx.core.domain.model.Progress
import org.openedx.core.extension.getChapterBlocks
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.presentation.contenttab.CourseContentAllEmptyState
import org.openedx.course.presentation.ui.CourseDatesBanner
import org.openedx.course.presentation.ui.CourseDatesBannerTablet
import org.openedx.course.presentation.ui.CourseMessage
import org.openedx.course.presentation.ui.CourseProgress
import org.openedx.course.presentation.ui.CourseSection
import org.openedx.foundation.extension.takeIfNotEmpty
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import java.util.Date

@Composable
fun CourseContentAllScreen(
    windowSize: WindowSize,
    viewModel: CourseContentAllViewModel,
    fragmentManager: FragmentManager,
    onNavigateToHome: () -> Unit = {},
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

    CourseContentAllUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = uiMessage,
        onNavigateToHome = onNavigateToHome,
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
            viewModel.resetCourseDatesBanner()
        },
        onCertificateClick = {
            viewModel.viewCertificateTappedEvent()
            it.takeIfNotEmpty()
                ?.let { url -> AndroidUriHandler(context).openUri(url) }
        }
    )
}

@Composable
private fun CourseContentAllUI(
    windowSize: WindowSize,
    uiState: CourseContentAllUIState,
    uiMessage: UIMessage?,
    onNavigateToHome: () -> Unit,
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
                        is CourseContentAllUIState.CourseData -> {
                            if (uiState.courseStructure.blockData.isEmpty()) {
                                CourseContentAllEmptyState(
                                    onReturnToCourseClick = onNavigateToHome
                                )
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

                                    val sections =
                                        uiState.courseStructure.blockData.getChapterBlocks()
                                    val progress = Progress(
                                        total = sections.size,
                                        completed = sections.filter { it.isCompleted() }.size
                                    )
                                    item {
                                        CourseProgress(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 24.dp,
                                                    end = 24.dp
                                                ),
                                            progress = progress,
                                            description = pluralStringResource(
                                                R.plurals.course_sections_complete,
                                                progress.completed,
                                                progress.completed,
                                                progress.total
                                            )
                                        )
                                    }

                                    if (uiState.resumeComponent != null) {
                                        item {
                                            Box(listPadding) {
                                                ResumeCourse(
                                                    modifier = Modifier.padding(vertical = 16.dp),
                                                    block = uiState.resumeComponent,
                                                    displayName = uiState.resumeUnitTitle,
                                                    onResumeClick = onResumeClick
                                                )
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
                                                isSectionVisible = courseSectionsState,
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

                        CourseContentAllUIState.Error -> {
                            CourseContentAllEmptyState(
                                onReturnToCourseClick = onNavigateToHome
                            )
                        }

                        CourseContentAllUIState.Loading -> {
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
    OpenEdXButton(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp),
        onClick = {
            onResumeClick(block.id)
        },
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = displayName,
                    color = MaterialTheme.appColors.primaryButtonText,
                    style = MaterialTheme.appTypography.titleMedium,
                    fontWeight = FontWeight.W600
                )
                TextIcon(
                    text = stringResource(id = R.string.course_continue),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    color = MaterialTheme.appColors.primaryButtonText,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        }
    )
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
        CourseContentAllUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseContentAllUIState.CourseData(
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
            onNavigateToHome = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseContentAllScreenTabletPreview() {
    OpenEdXTheme {
        CourseContentAllUI(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseContentAllUIState.CourseData(
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
            onNavigateToHome = {},
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
    numPointsPossible = 3f,
    shortLabel = "HM1"
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
