package org.openedx.course.presentation.outline

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.BlockType
import org.openedx.core.Mock
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.Progress
import org.openedx.core.extension.getChapterBlocks
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.R
import org.openedx.course.presentation.contenttab.CourseContentAllEmptyState
import org.openedx.course.presentation.ui.CourseDatesBanner
import org.openedx.course.presentation.ui.CourseDatesBannerTablet
import org.openedx.course.presentation.ui.CourseMessage
import org.openedx.course.presentation.ui.CourseProgress
import org.openedx.course.presentation.ui.CourseSection
import org.openedx.course.presentation.ui.ResumeCourseButton
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.foundation.extension.takeIfNotEmpty
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue

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
                                                ResumeCourseButton(
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
                                                section = section,
                                                onItemClick = onExpandClick,
                                                useRelativeDates = uiState.useRelativeDates,
                                                isSectionVisible = courseSectionsState,
                                                subSections = courseSubSections,
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
                                modifier = Modifier.verticalScroll(rememberScrollState()),
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
                Mock.mockCourseStructure,
                mapOf(),
                Mock.mockChapterBlock,
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
                Mock.mockCourseStructure,
                mapOf(),
                Mock.mockChapterBlock,
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
        ResumeCourseButton(block = Mock.mockChapterBlock, displayName = "Resumed Unit") {}
    }
}
