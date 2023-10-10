package org.openedx.profile.presentation.settings.video

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.openedx.core.R
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.profile.R as profileR

class VideoQualityFragment : Fragment() {

    private val viewModel by viewModel<VideoQualityViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val videoQuality by viewModel.videoQuality.observeAsState(viewModel.currentVideoQuality)

                VideoQualityScreen(
                    windowSize = windowSize,
                    videoQuality = videoQuality,
                    onQualityChanged = {
                        viewModel.setVideoDownloadQuality(it)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    })
            }
        }
    }

}

@Composable
private fun VideoQualityScreen(
    windowSize: WindowSize,
    videoQuality: VideoQuality,
    onQualityChanged: (VideoQuality) -> Unit,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        scaffoldState = scaffoldState,
    ) { paddingValues ->

        val topBarWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier
                        .fillMaxWidth()
                )
            )
        }

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .statusBarsInset()
                    .displayCutoutForLandscape(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = topBarWidth,
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = profileR.string.profile_video_download_quality),
                        color = MaterialTheme.appColors.textPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    BackBtn(Modifier.padding(start = 8.dp)) {
                        onBackClick()
                    }
                }

                Column(
                    modifier = Modifier
                        .then(contentWidth)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val autoQuality =
                        stringResource(id = R.string.auto_recommended_text).split(Regex("\\s"), 2)
                    QualityOption(
                        title = autoQuality[0],
                        description = autoQuality[1],
                        selected = videoQuality == VideoQuality.AUTO,
                        onClick = {
                            onQualityChanged(VideoQuality.AUTO)
                        }
                    )
                    Divider()
                    val option360p =
                        stringResource(id = R.string.video_quality_p360).split(Regex("\\s"), 2)
                    QualityOption(
                        title = option360p[0],
                        description = option360p[1],
                        selected = videoQuality == VideoQuality.OPTION_360P,
                        onClick = {
                            onQualityChanged(VideoQuality.OPTION_360P)
                        }
                    )
                    Divider()
                    val option540p =
                        stringResource(id = R.string.video_quality_p540)
                    QualityOption(
                        title = option540p,
                        description = "",
                        selected = videoQuality == VideoQuality.OPTION_540P,
                        onClick = {
                            onQualityChanged(VideoQuality.OPTION_540P)
                        }
                    )
                    Divider()
                    val option720p =
                        stringResource(id = R.string.video_quality_p720).split(Regex("\\s"), 2)
                    QualityOption(
                        title = option720p[0],
                        description = option720p[1],
                        selected = videoQuality == VideoQuality.OPTION_720P,
                        onClick = {
                            onQualityChanged(VideoQuality.OPTION_720P)
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun QualityOption(
    title: String,
    description: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable {
                onClick()
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            if (!description.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description.replace(Regex("[(|)]"), ""),
                    color = MaterialTheme.appColors.textSecondary,
                    style = MaterialTheme.appTypography.labelMedium
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Done,
                tint = MaterialTheme.appColors.primary,
                contentDescription = null
            )
        }
    }

}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun VideoQualityScreenPreview() {
    OpenEdXTheme {
        VideoQualityScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            videoQuality = VideoQuality.OPTION_720P,
            onQualityChanged = {},
            onBackClick = {})
    }
}