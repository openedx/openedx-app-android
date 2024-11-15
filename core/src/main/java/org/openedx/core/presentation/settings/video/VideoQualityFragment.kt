package org.openedx.core.presentation.settings.video

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.R
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.nonZero
import org.openedx.foundation.extension.tagId
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue

class VideoQualityFragment : Fragment() {

    private val viewModel by viewModel<VideoQualityViewModel> {
        parametersOf(
            requireArguments().getString(ARG_QUALITY_TYPE, "")
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val title = stringResource(
                    id = if (viewModel.getQualityType() == VideoQualityType.Streaming) {
                        R.string.core_video_streaming_quality
                    } else {
                        R.string.core_video_download_quality
                    }
                )
                val videoQuality by viewModel.videoQuality.observeAsState(viewModel.getCurrentVideoQuality())

                VideoQualityScreen(
                    windowSize = windowSize,
                    title = title,
                    selectedVideoQuality = videoQuality,
                    onQualityChanged = {
                        viewModel.setVideoQuality(it)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    companion object {

        private const val ARG_QUALITY_TYPE = "quality_type"

        fun newInstance(
            type: String,
        ): VideoQualityFragment {
            val fragment = VideoQualityFragment()
            fragment.arguments = bundleOf(
                ARG_QUALITY_TYPE to type
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun VideoQualityScreen(
    windowSize: WindowSize,
    title: String,
    selectedVideoQuality: VideoQuality,
    onQualityChanged: (VideoQuality) -> Unit,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .semantics {
                testTagsAsResourceId = true
            },
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
                Toolbar(
                    modifier = topBarWidth,
                    label = title,
                    canShowBackBtn = true,
                    onBackClick = onBackClick
                )

                Column(
                    modifier = Modifier
                        .then(contentWidth)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VideoQuality.entries.forEach { videoQuality ->
                        QualityOption(
                            title = stringResource(id = videoQuality.titleResId),
                            description = videoQuality.desResId.nonZero()
                                ?.let { stringResource(id = videoQuality.desResId) } ?: "",
                            selected = selectedVideoQuality == videoQuality,
                            onClick = {
                                onQualityChanged(videoQuality)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .testTag("btn_video_quality_${title.tagId()}")
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
                modifier = Modifier.testTag("txt_video_quality_title_${title.tagId()}"),
                text = title,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    modifier = Modifier.testTag("txt_video_quality_description_${title.tagId()}"),
                    text = description,
                    color = MaterialTheme.appColors.textSecondary,
                    style = MaterialTheme.appTypography.labelMedium
                )
            }
        }
        if (selected) {
            Icon(
                modifier = Modifier.testTag("ic_video_quality_selected_${title.tagId()}"),
                imageVector = Icons.Filled.Done,
                tint = MaterialTheme.appColors.primary,
                contentDescription = null
            )
        }
    }
    Divider()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VideoQualityScreenPreview() {
    OpenEdXTheme {
        VideoQualityScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            title = "",
            selectedVideoQuality = VideoQuality.OPTION_720P,
            onQualityChanged = {},
            onBackClick = {}
        )
    }
}
