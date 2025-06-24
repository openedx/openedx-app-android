package org.openedx.course.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.koin.compose.koinInject
import org.openedx.core.BlockType
import org.openedx.core.NoContentScreenType
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.Progress
import org.openedx.core.module.download.DownloadModelsSize
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.NoContentScreen
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.presentation.videos.CourseVideoViewModel
import org.openedx.course.presentation.videos.CourseVideosUIState
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.foundation.utils.FileUtil
import java.util.Date
import org.openedx.core.R as coreR

@Composable
fun CourseContentVideoScreen(
    windowSize: WindowSize,
    viewModel: CourseVideoViewModel,
    fragmentManager: FragmentManager
) {
    val uiState by viewModel.uiState.collectAsState(CourseVideosUIState.Loading)
    val uiMessage by viewModel.uiMessage.collectAsState(null)
    val fileUtil: FileUtil = koinInject()

    CourseVideosUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = uiMessage,
        courseTitle = viewModel.courseTitle,
        onExpandClick = { block ->
            viewModel.switchCourseSections(block.id)
        },
        onSubSectionClick = { subSectionBlock ->
            if (viewModel.isCourseDropdownNavigationEnabled) {
                viewModel.courseSubSectionUnit[subSectionBlock.id]?.let { unit ->
                    viewModel.courseRouter.navigateToCourseContainer(
                        fragmentManager,
                        courseId = viewModel.courseId,
                        unitId = unit.id,
                        mode = CourseViewMode.VIDEOS
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
                    mode = CourseViewMode.VIDEOS
                )
            }
        },
        onDownloadClick = { blocksIds ->
            viewModel.downloadBlocks(
                blocksIds = blocksIds,
                fragmentManager = fragmentManager,
            )
        },
        onDownloadAllClick = { isAllBlocksDownloadedOrDownloading ->
            viewModel.logBulkDownloadToggleEvent(
                !isAllBlocksDownloadedOrDownloading,
                viewModel.courseId
            )
            if (isAllBlocksDownloadedOrDownloading) {
                viewModel.removeAllDownloadModels()
            } else {
                viewModel.saveAllDownloadModels(
                    fileUtil.getExternalAppDir().path,
                    viewModel.courseId
                )
            }
        },
        onCompletedSectionVisibilityChange = {
            viewModel.onCompletedSectionVisibilityChange(it)
        }
    )
}

@Composable
private fun CourseVideosUI(
    windowSize: WindowSize,
    uiState: CourseVideosUIState,
    uiMessage: UIMessage?,
    courseTitle: String,
    onExpandClick: (Block) -> Unit,
    onSubSectionClick: (Block) -> Unit,
    onDownloadClick: (blocksIds: List<String>) -> Unit,
    onDownloadAllClick: (Boolean) -> Unit,
    onCompletedSectionVisibilityChange: (Boolean) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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

        var isDownloadConfirmationShowed by rememberSaveable {
            mutableStateOf(false)
        }

        var isDeleteDownloadsConfirmationShowed by rememberSaveable {
            mutableStateOf(false)
        }

        var deleteDownloadBlock by rememberSaveable {
            mutableStateOf<Block?>(null)
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
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (uiState) {
                            is CourseVideosUIState.Empty -> {
                                NoContentScreen(noContentScreenType = NoContentScreenType.COURSE_VIDEOS)
                            }

                            is CourseVideosUIState.CourseData -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = listBottomPadding
                                ) {
                                    val progress = uiState.courseStructure.progress
                                    if (progress != null && progress.totalAssignmentsCount > 0) {
                                        item {
                                            CourseProgress(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        bottom = 8.dp,
                                                        start = 24.dp,
                                                        end = 24.dp,
                                                    ),
                                                progress = progress,
                                                onVisibilityChanged = {
                                                    onCompletedSectionVisibilityChange(it)
                                                }
                                            )
                                        }
                                    }

                                    uiState.courseStructure.blockData.forEach { section ->
                                        val courseSubSections =
                                            uiState.courseSubSections[section.id]
                                        val courseSectionsState =
                                            uiState.courseSectionsState[section.id]

                                        item {
                                            CourseSection(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                block = section,
                                                onItemClick = onExpandClick,
                                                courseSectionsState = courseSectionsState,
                                                courseSubSections = courseSubSections,
                                                downloadedStateMap = uiState.downloadedState,
                                                useRelativeDates = uiState.useRelativeDates,
                                                onSubSectionClick = onSubSectionClick,
                                                onDownloadClick = onDownloadClick
                                            )
                                        }
                                    }
                                }
                            }

                            CourseVideosUIState.Loading -> {
                                CircularProgress()
                            }
                        }
                    }
                }
            }
        }

        if (isDownloadConfirmationShowed) {
            AlertDialog(
                title = {
                    Text(
                        text = stringResource(id = coreR.string.core_download_big_files_confirmation_title)
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = coreR.string.core_download_big_files_confirmation_text)
                    )
                },
                onDismissRequest = {
                    isDownloadConfirmationShowed = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isDownloadConfirmationShowed = false
                            onDownloadAllClick(false)
                        }
                    ) {
                        Text(
                            text = stringResource(id = coreR.string.core_confirm)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            isDownloadConfirmationShowed = false
                        }
                    ) {
                        Text(text = stringResource(id = coreR.string.core_dismiss))
                    }
                }
            )
        }

        if (isDeleteDownloadsConfirmationShowed) {
            val downloadModelsSize =
                (uiState as? CourseVideosUIState.CourseData)?.downloadModelsSize
            val isDownloadedAllVideos =
                downloadModelsSize?.isAllBlocksDownloadedOrDownloading == true &&
                        downloadModelsSize.remainingCount == 0
            val dialogTextId = if (isDownloadedAllVideos) {
                coreR.string.core_delete_confirmation
            } else {
                coreR.string.core_delete_in_process_confirmation
            }

            AlertDialog(
                title = {
                    Text(
                        text = stringResource(id = coreR.string.core_warning)
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = dialogTextId, courseTitle)
                    )
                },
                onDismissRequest = {
                    isDeleteDownloadsConfirmationShowed = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isDeleteDownloadsConfirmationShowed = false
                            onDownloadAllClick(true)
                        }
                    ) {
                        Text(
                            text = stringResource(id = coreR.string.core_delete)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            isDeleteDownloadsConfirmationShowed = false
                        }
                    ) {
                        Text(text = stringResource(id = coreR.string.core_cancel))
                    }
                }
            )
        }

        if (deleteDownloadBlock != null) {
            AlertDialog(
                title = {
                    Text(
                        text = stringResource(id = coreR.string.core_warning)
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            id = coreR.string.core_delete_download_confirmation_text,
                            deleteDownloadBlock?.displayName ?: ""
                        )
                    )
                },
                onDismissRequest = {
                    deleteDownloadBlock = null
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteDownloadBlock?.let { block ->
                                onDownloadClick(listOf(block.id))
                            }
                            deleteDownloadBlock = null
                        }
                    ) {
                        Text(
                            text = stringResource(id = coreR.string.core_delete)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            deleteDownloadBlock = null
                        }
                    ) {
                        Text(text = stringResource(id = coreR.string.core_cancel))
                    }
                }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseVideosScreenPreview() {
    OpenEdXTheme {
        CourseVideosUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiMessage = null,
            uiState = CourseVideosUIState.CourseData(
                mockCourseStructure,
                emptyMap(),
                mapOf(),
                mapOf(),
                mapOf(),
                DownloadModelsSize(
                    isAllBlocksDownloadedOrDownloading = false,
                    remainingCount = 0,
                    remainingSize = 0,
                    allCount = 1,
                    allSize = 0
                ),
                useRelativeDates = true,
                isCompletedSectionsShown = false
            ),
            courseTitle = "",
            onExpandClick = { },
            onSubSectionClick = { },
            onDownloadClick = {},
            onDownloadAllClick = {},
            onCompletedSectionVisibilityChange = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseVideosScreenEmptyPreview() {
    OpenEdXTheme {
        CourseVideosUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiMessage = null,
            uiState = CourseVideosUIState.Empty,
            courseTitle = "",
            onExpandClick = { },
            onSubSectionClick = { },
            onDownloadClick = {},
            onDownloadAllClick = {},
            onCompletedSectionVisibilityChange = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseVideosScreenTabletPreview() {
    OpenEdXTheme {
        CourseVideosUI(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiMessage = null,
            uiState = CourseVideosUIState.CourseData(
                mockCourseStructure,
                emptyMap(),
                mapOf(),
                mapOf(),
                mapOf(),
                DownloadModelsSize(
                    isAllBlocksDownloadedOrDownloading = false,
                    remainingCount = 0,
                    remainingSize = 0,
                    allCount = 0,
                    allSize = 0
                ),
                useRelativeDates = true,
                isCompletedSectionsShown = true
            ),
            courseTitle = "",
            onExpandClick = { },
            onSubSectionClick = { },
            onDownloadClick = {},
            onDownloadAllClick = {},
            onCompletedSectionVisibilityChange = {}
        )
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
    descendantsType = BlockType.SEQUENTIAL,
    completion = 0.0,
    containsGatedContent = false,
    assignmentProgress = mockAssignmentProgress,
    due = Date(),
    offlineDownload = null
)

private val mockCourseStructure = CourseStructure(
    root = "",
    blockData = listOf(mockSequentialBlock, mockChapterBlock),
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
