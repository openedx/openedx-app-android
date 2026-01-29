package org.openedx.course.presentation.videos

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.CoreMocks
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.Progress
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.R
import org.openedx.course.presentation.contenttab.CourseContentVideoEmptyState
import org.openedx.course.presentation.ui.CourseProgress
import org.openedx.course.presentation.ui.CourseVideoSection
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue

@Composable
fun CourseContentVideoScreen(
    windowSize: WindowSize,
    viewModel: CourseVideoViewModel,
    fragmentManager: FragmentManager,
    onNavigateToHome: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState(CourseVideoUIState.Loading)
    val uiMessage by viewModel.uiMessage.collectAsState(null)

    CourseVideosUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = uiMessage,
        onNavigateToHome = onNavigateToHome,
        onVideoClick = { videoBlock ->
            viewModel.courseRouter.navigateToCourseContainer(
                fragmentManager,
                courseId = viewModel.courseId,
                unitId = viewModel.getBlockParent(videoBlock.id)?.id ?: return@CourseVideosUI,
                mode = CourseViewMode.VIDEOS
            )
            viewModel.logVideoClick(videoBlock.id)
        },
        onDownloadClick = { blocksIds ->
            viewModel.downloadBlocks(
                blocksIds = blocksIds,
                fragmentManager = fragmentManager,
            )
        },
        onCompletedSectionVisibilityChange = {
            viewModel.onCompletedSectionVisibilityChange()
        },
    )
}

@Composable
private fun CourseVideosUI(
    windowSize: WindowSize,
    uiState: CourseVideoUIState,
    uiMessage: UIMessage?,
    onNavigateToHome: () -> Unit,
    onVideoClick: (Block) -> Unit,
    onDownloadClick: (blocksIds: List<String>) -> Unit,
    onCompletedSectionVisibilityChange: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.appColors.background
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

        HandleUIMessage(uiMessage = uiMessage, snackbarHostState = snackbarHostState)

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
                            is CourseVideoUIState.Empty -> {
                                CourseContentVideoEmptyState(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    onReturnToCourseClick = onNavigateToHome
                                )
                            }

                            is CourseVideoUIState.CourseData -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = listBottomPadding
                                ) {
                                    val allVideos = uiState.courseVideos.values.flatten()
                                    val hasCompletedSection =
                                        uiState.courseVideos.values.any { sectionVideos ->
                                            sectionVideos.all { video ->
                                                video.isCompleted()
                                            }
                                        }
                                    val progress = Progress(
                                        completed = allVideos.filter { it.isCompleted() }.size,
                                        total = allVideos.size,
                                    )
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
                                            isCompletedShown = uiState.isCompletedSectionsShown,
                                            onVisibilityChanged = if (hasCompletedSection) {
                                                { onCompletedSectionVisibilityChange() }
                                            } else {
                                                null
                                            },
                                            description = stringResource(
                                                R.string.course_completed_of,
                                                progress.completed,
                                                progress.total
                                            )
                                        )
                                    }
                                    item {
                                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                                    }

                                    uiState.courseStructure.blockData
                                        .let { list ->
                                            if (uiState.isCompletedSectionsShown) {
                                                list.sortedBy { section ->
                                                    uiState.courseVideos[section.id]?.any { !it.isCompleted() }
                                                }
                                            } else {
                                                list
                                            }
                                        }
                                        .forEach { section ->
                                            val sectionVideos =
                                                uiState.courseVideos[section.id] ?: emptyList()

                                            val shouldShowSection =
                                                sectionVideos.any { !it.isCompleted() } ||
                                                        uiState.isCompletedSectionsShown
                                            if (shouldShowSection) {
                                                item {
                                                    CourseVideoSection(
                                                        block = section,
                                                        videoBlocks = sectionVideos,
                                                        downloadedStateMap = uiState.downloadedState,
                                                        onVideoClick = onVideoClick,
                                                        onDownloadClick = onDownloadClick,
                                                        preview = uiState.videoPreview,
                                                        progress = uiState.videoProgress,
                                                    )
                                                }
                                            }
                                        }
                                }
                            }

                            CourseVideoUIState.Loading -> {
                                CircularProgress()
                            }
                        }
                    }
                }
            }
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
            uiState = CourseVideoUIState.CourseData(
                CoreMocks.mockCourseStructure,
                emptyMap(),
                mapOf(),
                mapOf(),
                CoreMocks.mockDownloadModelsSize,
                isCompletedSectionsShown = false,
                videoPreview = mapOf(),
                videoProgress = mapOf(),
            ),
            onVideoClick = { },
            onDownloadClick = {},
            onCompletedSectionVisibilityChange = {},
            onNavigateToHome = {},
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
            uiState = CourseVideoUIState.Empty,
            onVideoClick = { },
            onDownloadClick = {},
            onCompletedSectionVisibilityChange = {},
            onNavigateToHome = {},
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
            uiState = CourseVideoUIState.CourseData(
                CoreMocks.mockCourseStructure,
                emptyMap(),
                mapOf(),
                mapOf(),
                CoreMocks.mockDownloadModelsSize.copy(
                    allCount = 0
                ),
                isCompletedSectionsShown = true,
                videoPreview = mapOf(),
                videoProgress = mapOf(),
            ),
            onVideoClick = { },
            onDownloadClick = {},
            onCompletedSectionVisibilityChange = {},
            onNavigateToHome = {},
        )
    }
}
