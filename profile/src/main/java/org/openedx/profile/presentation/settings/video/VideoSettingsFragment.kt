package org.openedx.profile.presentation.settings.video

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.profile.presentation.ProfileRouter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.profile.R as profileR

class VideoSettingsFragment : Fragment() {

    private val viewModel by viewModel<VideoSettingsViewModel>()
    private val router by inject<ProfileRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
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

                val videoSettings by viewModel.videoSettings.observeAsState(viewModel.currentSettings)

                VideoSettingsScreen(
                    videoSettings = videoSettings,
                    windowSize = windowSize,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    wifiDownloadChanged = {
                        viewModel.setWifiDownloadOnly(it)
                    },
                    videoDownloadQualityClick = {
                        router.navigateToVideoQuality(
                            requireActivity().supportFragmentManager
                        )
                    }
                )
            }
        }
    }

}

@Composable
private fun VideoSettingsScreen(
    windowSize: WindowSize,
    videoSettings: VideoSettings,
    wifiDownloadChanged: (Boolean) -> Unit,
    videoDownloadQualityClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    var wifiDownloadOnly by rememberSaveable {
        mutableStateOf(videoSettings.wifiDownloadOnly)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState
    ) { paddingValues ->

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

        val topBarWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier
                        .fillMaxWidth()
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
                    .statusBarsInset(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = topBarWidth,
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = org.openedx.profile.R.string.profile_video_settings),
                        color = MaterialTheme.appColors.textPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.appTypography.titleMedium
                    )

                    BackBtn(modifier = Modifier.padding(start = 8.dp)) {
                        onBackClick()
                    }
                }

                Column(
                    modifier = Modifier.then(contentWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                            .noRippleClickable {
                                wifiDownloadOnly = !wifiDownloadOnly
                                wifiDownloadChanged(wifiDownloadOnly)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = profileR.string.profile_wifi_only_download),
                                color = MaterialTheme.appColors.textPrimary,
                                style = MaterialTheme.appTypography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = profileR.string.profile_only_download_when_wifi_turned_on),
                                color = MaterialTheme.appColors.textSecondary,
                                style = MaterialTheme.appTypography.labelMedium
                            )
                        }
                        Switch(
                            checked = wifiDownloadOnly,
                            onCheckedChange = {
                                wifiDownloadOnly = !wifiDownloadOnly
                                wifiDownloadChanged(wifiDownloadOnly)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.appColors.primary,
                                checkedTrackColor = MaterialTheme.appColors.primary
                            )
                        )
                    }
                    Divider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                            .clickable {
                                videoDownloadQualityClick()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = profileR.string.profile_video_download_quality),
                                color = MaterialTheme.appColors.textPrimary,
                                style = MaterialTheme.appTypography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = videoSettings.videoQuality.titleResId),
                                color = MaterialTheme.appColors.textSecondary,
                                style = MaterialTheme.appTypography.labelMedium
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            tint = MaterialTheme.appColors.onSurface,
                            contentDescription = "Expandable Arrow"
                        )
                    }
                    Divider()
                }
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun VideoSettingsScreenPreview() {
    OpenEdXTheme {
        VideoSettingsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            wifiDownloadChanged = {},
            videoDownloadQualityClick = {},
            onBackClick = {},
            videoSettings = VideoSettings.default
        )
    }
}