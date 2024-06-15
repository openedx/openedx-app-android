package org.openedx.course.presentation.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.R

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
        onDownloadAllClick = {

        }
    )
}

@Composable
private fun CourseOfflineUI(
    windowSize: WindowSize,
    uiState: CourseOfflineUIState,
    onDownloadAllClick: () -> Unit
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
                .displayCutoutForLandscape(), contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = modifierScreenWidth,
                color = MaterialTheme.appColors.background,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .then(horizontalPadding)
                ) {
                    if (uiState.isHaveDownloadableBlocks) {
                        DownloadProgress(uiState = uiState)
                    } else {
                        NoDownloadableBlocksProgress()
                    }
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
                }
            }
        }
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
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconText(
                text = stringResource(R.string.course_downloaded),
                icon = Icons.Default.CloudDone,
                color = MaterialTheme.appColors.successGreen,
                textStyle = MaterialTheme.appTypography.labelLarge
            )
            IconText(
                text = stringResource(R.string.course_ready_to_download),
                icon = Icons.Outlined.CloudDownload,
                color = MaterialTheme.appColors.textDark,
                textStyle = MaterialTheme.appTypography.labelLarge
            )
        }
        if (uiState.progressBarValue != 0f) {
            Spacer(modifier = Modifier.height(20.dp))
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
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.course_you_can_download_course_content_offline),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textDark
        )
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
            uiState = CourseOfflineUIState(
                isHaveDownloadableBlocks = true,
                readyToDownloadSize = "159MB",
                downloadedSize = "0MB",
                progressBarValue = 0f
            ),
            onDownloadAllClick = {}
        )
    }
}
