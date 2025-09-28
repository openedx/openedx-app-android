package org.openedx.downloads.presentation.download

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.domain.model.DownloadCoursePreview
import org.openedx.core.extension.safeDivBy
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.DownloadedState.LOADING_COURSE_STRUCTURE
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IconText
import org.openedx.core.ui.MainToolbar
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXDropdownMenuItem
import org.openedx.core.ui.crop
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.downloads.R
import org.openedx.foundation.extension.toFileSize
import org.openedx.foundation.extension.toImageLink
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DownloadsScreen(
    uiState: DownloadsUIState,
    uiMessage: UIMessage?,
    apiHostUrl: String,
    hasInternetConnection: Boolean,
    onAction: (DownloadsViewActions) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val windowSize = rememberWindowSize()
    val configuration = LocalConfiguration.current
    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxWidth(),
            )
        )
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { onAction(DownloadsViewActions.SwipeRefresh) }
    )
    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background,
        topBar = {
            MainToolbar(
                modifier = Modifier
                    .statusBarsInset()
                    .displayCutoutForLandscape(),
                label = stringResource(id = R.string.downloads),
                onSettingsClick = {
                    onAction(DownloadsViewActions.OpenSettings)
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                    }
                } else if (uiState.downloadCoursePreviews.isEmpty()) {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .displayCutoutForLandscape()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (configuration.orientation == ORIENTATION_LANDSCAPE || windowSize.isTablet) {
                            LazyVerticalGrid(
                                modifier = contentWidth.fillMaxHeight(),
                                state = rememberLazyGridState(),
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(bottom = 46.dp, top = 12.dp),
                                content = {
                                    items(uiState.downloadCoursePreviews) { item ->
                                        val downloadModels =
                                            uiState.downloadModels.filter { it.courseId == item.id }
                                        val downloadState = uiState.courseDownloadState[item.id]
                                            ?: DownloadedState.NOT_DOWNLOADED
                                        CourseItem(
                                            modifier = Modifier.height(314.dp),
                                            downloadCoursePreview = item,
                                            downloadModels = downloadModels,
                                            downloadedState = downloadState,
                                            apiHostUrl = apiHostUrl,
                                            onCourseClick = {
                                                onAction(DownloadsViewActions.OpenCourse(item.id))
                                            },
                                            onDownloadClick = {
                                                onAction(DownloadsViewActions.DownloadCourse(item.id))
                                            },
                                            onCancelClick = {
                                                onAction(DownloadsViewActions.CancelDownloading(item.id))
                                            },
                                            onRemoveClick = {
                                                onAction(DownloadsViewActions.RemoveDownloads(item.id))
                                            }
                                        )
                                    }
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = contentWidth,
                                contentPadding = PaddingValues(bottom = 46.dp, top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                items(uiState.downloadCoursePreviews) { item ->
                                    val downloadModels =
                                        uiState.downloadModels.filter { it.courseId == item.id }
                                    val downloadState = uiState.courseDownloadState[item.id]
                                        ?: DownloadedState.NOT_DOWNLOADED
                                    CourseItem(
                                        downloadCoursePreview = item,
                                        downloadModels = downloadModels,
                                        downloadedState = downloadState,
                                        apiHostUrl = apiHostUrl,
                                        onCourseClick = {
                                            onAction(DownloadsViewActions.OpenCourse(item.id))
                                        },
                                        onDownloadClick = {
                                            onAction(DownloadsViewActions.DownloadCourse(item.id))
                                        },
                                        onCancelClick = {
                                            onAction(DownloadsViewActions.CancelDownloading(item.id))
                                        },
                                        onRemoveClick = {
                                            onAction(DownloadsViewActions.RemoveDownloads(item.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

                PullRefreshIndicator(
                    uiState.isRefreshing,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
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
                            onAction(DownloadsViewActions.SwipeRefresh)
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CourseItem(
    modifier: Modifier = Modifier,
    downloadCoursePreview: DownloadCoursePreview,
    downloadModels: List<DownloadModel>,
    downloadedState: DownloadedState,
    apiHostUrl: String,
    onCourseClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val windowSize = rememberWindowSize()
    val configuration = LocalConfiguration.current
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val downloadedSize = downloadModels
        .filter { it.downloadedState == DownloadedState.DOWNLOADED }
        .sumOf { it.size }
    val availableSize = downloadCoursePreview.totalSize - downloadedSize
    val availableSizeString = availableSize.toFileSize(space = false, round = 1)
    val progress = downloadedSize.toFloat().safeDivBy(downloadCoursePreview.totalSize.toFloat())
    Card(
        modifier = modifier
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 4.dp,
        onClick = onCourseClick
    ) {
        Box {
            Column(
                modifier = Modifier.animateContentSize()
            ) {
                val imageModifier =
                    if (configuration.orientation == ORIENTATION_LANDSCAPE || windowSize.isTablet) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.height(120.dp)
                    }
                AsyncImage(
                    modifier = imageModifier.fillMaxWidth(),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(downloadCoursePreview.image.toImageLink(apiHostUrl))
                        .error(org.openedx.core.R.drawable.core_no_image_course)
                        .placeholder(org.openedx.core.R.drawable.core_no_image_course)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 12.dp),
                ) {
                    Text(
                        text = downloadCoursePreview.name,
                        style = MaterialTheme.appTypography.titleLarge,
                        color = MaterialTheme.appColors.textDark,
                        overflow = TextOverflow.Ellipsis,
                        minLines = 1,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (downloadedState != DownloadedState.DOWNLOADED && downloadedSize != 0L) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            progress = progress,
                            color = MaterialTheme.appColors.successGreen,
                            backgroundColor = MaterialTheme.appColors.divider
                        )
                    }
                    if (downloadedSize != 0L) {
                        Spacer(modifier = Modifier.height(4.dp))
                        IconText(
                            icon = Icons.Filled.CloudDone,
                            color = MaterialTheme.appColors.successGreen,
                            text = stringResource(
                                R.string.downloaded_downloaded_size,
                                downloadedSize.toFileSize(space = false, round = 1)
                            )
                        )
                    }
                    if (downloadedState != DownloadedState.DOWNLOADED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        IconText(
                            icon = Icons.Outlined.CloudDownload,
                            color = MaterialTheme.appColors.textPrimaryVariant,
                            text = stringResource(
                                R.string.downloaded_available_size,
                                availableSizeString
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (downloadedState.isWaitingOrDownloading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    backgroundColor = Color.LightGray,
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.appColors.primary
                                )
                                IconButton(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(2.dp),
                                    onClick = onCancelClick
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(
                                            id = R.string.downloads_accessibility_stop_downloading_course
                                        ),
                                        tint = MaterialTheme.appColors.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val text = if (downloadedState == LOADING_COURSE_STRUCTURE) {
                                stringResource(R.string.downloads_loading_course_structure)
                            } else {
                                stringResource(org.openedx.core.R.string.core_downloading)
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.appTypography.titleSmall,
                                color = MaterialTheme.appColors.textPrimary
                            )
                        }
                    } else if (downloadedState == DownloadedState.NOT_DOWNLOADED) {
                        OpenEdXButton(
                            onClick = {
                                onDownloadClick()
                            },
                            content = {
                                IconText(
                                    text = stringResource(R.string.downloads_download_course),
                                    icon = Icons.Outlined.CloudDownload,
                                    color = MaterialTheme.appColors.primaryButtonText,
                                    textStyle = MaterialTheme.appTypography.labelLarge
                                )
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd),
            ) {
                if (downloadedSize != 0L || downloadedState.isWaitingOrDownloading) {
                    MoreButton(
                        onClick = {
                            isDropdownExpanded = true
                        }
                    )
                }
                DropdownMenu(
                    modifier = Modifier
                        .crop(vertical = 8.dp)
                        .defaultMinSize(minWidth = 269.dp)
                        .background(MaterialTheme.appColors.background),
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                ) {
                    Column {
                        if (downloadedSize != 0L) {
                            OpenEdXDropdownMenuItem(
                                text = stringResource(R.string.downloads_remove_course_downloads),
                                onClick = {
                                    isDropdownExpanded = false
                                    onRemoveClick()
                                }
                            )
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.appColors.divider
                            )
                        }
                        if (downloadedState.isWaitingOrDownloading) {
                            OpenEdXDropdownMenuItem(
                                text = stringResource(R.string.downloads_cancel_download),
                                onClick = {
                                    isDropdownExpanded = false
                                    onCancelClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = MaterialTheme.appColors.onPrimary.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .padding(4.dp),
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = null,
            tint = MaterialTheme.appColors.onSurface
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = org.openedx.core.R.drawable.core_ic_book),
                tint = MaterialTheme.appColors.textFieldBorder,
                contentDescription = null
            )
            Spacer(Modifier.height(4.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_title")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.downloads_empty_state_title),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_description")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.downloads_empty_state_description),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun DownloadsScreenPreview() {
    OpenEdXTheme {
        DownloadsScreen(
            uiState = DownloadsUIState(isLoading = false),
            uiMessage = null,
            apiHostUrl = "",
            hasInternetConnection = true,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun CourseItemPreview() {
    OpenEdXTheme {
        CourseItem(
            downloadCoursePreview = DownloadCoursePreview("", "name", "", 100),
            downloadModels = emptyList(),
            apiHostUrl = "",
            downloadedState = DownloadedState.NOT_DOWNLOADED,
            onCourseClick = {},
            onDownloadClick = {},
            onCancelClick = {},
            onRemoveClick = {},
        )
    }
}
