package org.openedx.course.presentation.offline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.foundation.extension.toFileSize
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as coreR

@Composable
fun CourseOfflineScreen(
    windowSize: WindowSize,
    viewModel: CourseOfflineViewModel,
    fragmentManager: FragmentManager,
) {
    val uiState by viewModel.uiState.collectAsState()

    CourseOfflineUI(
        windowSize = windowSize,
        uiState = uiState,
        hasInternetConnection = viewModel.hasInternetConnection,
        onDownloadAllClick = {
            viewModel.downloadAllBlocks(fragmentManager)
        },
        onCancelDownloadClick = {
            viewModel.removeDownloadModel()
        },
        onDeleteClick = { downloadModel ->
            viewModel.removeDownloadModel(
                downloadModel,
                fragmentManager
            )
        },
        onDeleteAllClick = {
            viewModel.deleteAll(fragmentManager)
        },
    )
}

@Composable
private fun CourseOfflineUI(
    windowSize: WindowSize,
    uiState: CourseOfflineUIState,
    hasInternetConnection: Boolean,
    onDownloadAllClick: () -> Unit,
    onCancelDownloadClick: () -> Unit,
    onDeleteClick: (downloadModel: DownloadModel) -> Unit,
    onDeleteAllClick: () -> Unit
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

        val horizontalPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.padding(horizontal = 6.dp),
                    compact = Modifier.padding(horizontal = 24.dp)
                )
            )
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
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 24.dp)
                        .then(horizontalPadding)
                ) {
                    item {
                        if (uiState.isHaveDownloadableBlocks) {
                            DownloadProgress(
                                uiState = uiState,
                            )
                        } else {
                            NoDownloadableBlocksProgress()
                        }
                        if (uiState.progressBarValue != 1f && !uiState.isDownloading && hasInternetConnection) {
                            Spacer(modifier = Modifier.height(20.dp))
                            OpenEdXButton(
                                text = stringResource(R.string.course_download_all),
                                backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
                                onClick = onDownloadAllClick,
                                enabled = uiState.isHaveDownloadableBlocks,
                                content = {
                                    val textColor = if (uiState.isHaveDownloadableBlocks) {
                                        MaterialTheme.appColors.primaryButtonText
                                    } else {
                                        MaterialTheme.appColors.textPrimaryVariant
                                    }
                                    IconText(
                                        text = stringResource(R.string.course_download_all),
                                        icon = Icons.Outlined.CloudDownload,
                                        color = textColor,
                                        textStyle = MaterialTheme.appTypography.labelLarge
                                    )
                                }
                            )
                        } else if (uiState.isDownloading) {
                            Spacer(modifier = Modifier.height(20.dp))
                            OpenEdXOutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.course_cancel_course_download),
                                backgroundColor = MaterialTheme.appColors.background,
                                borderColor = MaterialTheme.appColors.error,
                                textColor = MaterialTheme.appColors.error,
                                onClick = onCancelDownloadClick,
                                content = {
                                    IconText(
                                        text = stringResource(R.string.course_cancel_course_download),
                                        icon = Icons.Rounded.Close,
                                        color = MaterialTheme.appColors.error,
                                        textStyle = MaterialTheme.appTypography.labelLarge
                                    )
                                }
                            )
                        }
                        if (uiState.largestDownloads.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            LargestDownloads(
                                largestDownloads = uiState.largestDownloads,
                                isDownloading = uiState.isDownloading,
                                onDeleteClick = onDeleteClick,
                                onDeleteAllClick = onDeleteAllClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LargestDownloads(
    largestDownloads: List<DownloadModel>,
    isDownloading: Boolean,
    onDeleteClick: (downloadModel: DownloadModel) -> Unit,
    onDeleteAllClick: () -> Unit,
) {
    var isEditingEnabled by rememberSaveable {
        mutableStateOf(false)
    }
    val text = if (!isEditingEnabled) {
        stringResource(coreR.string.core_edit)
    } else {
        stringResource(coreR.string.core_label_done)
    }

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            isEditingEnabled = false
        }
    }

    Column {
        Row {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.course_largest_downloads),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark
            )
            if (!isDownloading) {
                Text(
                    modifier = Modifier.clickable {
                        isEditingEnabled = !isEditingEnabled
                    },
                    text = text,
                    style = MaterialTheme.appTypography.bodyMedium,
                    color = MaterialTheme.appColors.textAccent,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        largestDownloads.forEach {
            DownloadItem(
                downloadModel = it,
                isEditingEnabled = isEditingEnabled,
                onDeleteClick = onDeleteClick
            )
        }
        if (!isDownloading) {
            OpenEdXOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.course_remove_all_downloads),
                backgroundColor = MaterialTheme.appColors.background,
                borderColor = MaterialTheme.appColors.error,
                textColor = MaterialTheme.appColors.error,
                onClick = onDeleteAllClick,
                content = {
                    IconText(
                        text = stringResource(R.string.course_remove_all_downloads),
                        icon = Icons.Rounded.Delete,
                        color = MaterialTheme.appColors.error,
                        textStyle = MaterialTheme.appTypography.labelLarge
                    )
                }
            )
        }
    }
}

@Composable
private fun DownloadItem(
    modifier: Modifier = Modifier,
    downloadModel: DownloadModel,
    isEditingEnabled: Boolean,
    onDeleteClick: (downloadModel: DownloadModel) -> Unit
) {
    val fileIcon = if (downloadModel.type == FileType.VIDEO) {
        Icons.Outlined.SmartDisplay
    } else {
        Icons.AutoMirrored.Outlined.InsertDriveFile
    }
    val downloadIcon: ImageVector
    val downloadIconTint: Color
    val downloadIconClick: Modifier
    if (isEditingEnabled) {
        downloadIcon = Icons.Rounded.Delete
        downloadIconTint = MaterialTheme.appColors.error
        downloadIconClick = Modifier.clickable {
            onDeleteClick(downloadModel)
        }
    } else {
        downloadIcon = Icons.Default.CloudDone
        downloadIconTint = MaterialTheme.appColors.successGreen
        downloadIconClick = Modifier
    }

    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = fileIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = downloadModel.title,
                    style = MaterialTheme.appTypography.labelLarge,
                    color = MaterialTheme.appColors.textDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = downloadModel.size.toFileSize(1, false),
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textDark
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .then(downloadIconClick),
                imageVector = downloadIcon,
                tint = downloadIconTint,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider()
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DownloadProgress(
    modifier: Modifier = Modifier,
    uiState: CourseOfflineUIState,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = uiState.downloadedSize,
                style = MaterialTheme.appTypography.titleLarge,
                color = MaterialTheme.appColors.successGreen
            )
            Text(
                text = uiState.readyToDownloadSize,
                style = MaterialTheme.appTypography.titleLarge,
                color = MaterialTheme.appColors.textDark
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconText(
                text = stringResource(R.string.course_downloaded),
                icon = Icons.Default.CloudDone,
                color = MaterialTheme.appColors.successGreen,
                textStyle = MaterialTheme.appTypography.labelLarge
            )
            if (!uiState.isDownloading) {
                IconText(
                    text = stringResource(R.string.course_ready_to_download),
                    icon = Icons.Outlined.CloudDownload,
                    color = MaterialTheme.appColors.textDark,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            } else {
                IconText(
                    text = stringResource(R.string.course_downloading),
                    icon = Icons.Outlined.CloudDownload,
                    color = MaterialTheme.appColors.textDark,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        }
        if (uiState.progressBarValue != 0f) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                progress = uiState.progressBarValue,
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.appColors.successGreen,
                backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
            )
        } else {
            Text(
                text = stringResource(R.string.course_you_can_download_course_content_offline),
                style = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.textDark
            )
        }
    }
}

@Composable
private fun NoDownloadableBlocksProgress(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.course_0mb),
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textFieldHint
        )
        Spacer(modifier = Modifier.height(4.dp))
        IconText(
            text = stringResource(R.string.course_available_to_download),
            icon = Icons.Outlined.CloudDownload,
            color = MaterialTheme.appColors.textFieldHint,
            textStyle = MaterialTheme.appTypography.labelLarge
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.course_no_available_to_download_offline),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textDark
        )
    }
}

@Preview
@Composable
private fun CourseOfflineUIPreview() {
    OpenEdXTheme {
        CourseOfflineUI(
            windowSize = rememberWindowSize(),
            hasInternetConnection = true,
            uiState = CourseOfflineUIState(
                isHaveDownloadableBlocks = true,
                readyToDownloadSize = "159MB",
                downloadedSize = "0MB",
                progressBarValue = 0f,
                isDownloading = true,
                largestDownloads = listOf(
                    DownloadModel(
                        "",
                        "",
                        "",
                        0,
                        "",
                        "",
                        FileType.X_BLOCK,
                        DownloadedState.DOWNLOADED,
                        null
                    )
                ),
            ),
            onDownloadAllClick = {},
            onCancelDownloadClick = {},
            onDeleteClick = {},
            onDeleteAllClick = {}
        )
    }
}
