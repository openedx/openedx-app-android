package org.openedx.course.settings.download

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.CoreMocks
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.presentation.ui.OfflineQueueCard
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as coreR

class DownloadQueueFragment : Fragment() {

    private val viewModel by viewModel<DownloadQueueViewModel> {
        parametersOf(requireArguments().getStringArrayList(ARG_DESCENDANTS))
    }

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
                val uiState by viewModel.uiState.collectAsStateWithLifecycle(DownloadQueueUIState.Loading)

                DownloadQueueScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onDownloadClick = {
                        viewModel.removeDownloadModels(it.id, "")
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_DESCENDANTS = "descendants"
        fun newInstance(descendants: List<String>): DownloadQueueFragment {
            val fragment = DownloadQueueFragment()
            fragment.arguments = bundleOf(
                ARG_DESCENDANTS to descendants
            )
            return fragment
        }
    }
}

@Composable
private fun DownloadQueueScreen(
    windowSize: WindowSize,
    uiState: DownloadQueueUIState,
    onBackClick: () -> Unit,
    onDownloadClick: (DownloadModel) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.appColors.background,
        contentWindowInsets = WindowInsets()
    ) { paddingValues ->

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(contentWidth) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsInset()
                        .zIndex(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BackBtn {
                        onBackClick()
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        text = stringResource(id = coreR.string.core_download_queue_title),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    when (uiState) {
                        is DownloadQueueUIState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is DownloadQueueUIState.Models -> {
                            Column(Modifier.fillMaxSize()) {
                                LazyColumn {
                                    items(uiState.downloadingModels) { model ->
                                        val progressValue =
                                            if (model.id == uiState.currentProgressId) {
                                                uiState.currentProgressValue
                                            } else {
                                                0
                                            }
                                        val progressSize =
                                            if (model.id == uiState.currentProgressId) {
                                                uiState.currentProgressSize
                                            } else {
                                                0
                                            }

                                        OfflineQueueCard(
                                            downloadModel = model,
                                            progressValue = progressValue,
                                            progressSize = progressSize,
                                            onDownloadClick = onDownloadClick
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }

                        else -> {
                            onBackClick()
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
private fun DownloadQueueScreenPreview() {
    OpenEdXTheme {
        DownloadQueueScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DownloadQueueUIState.Models(
                listOf(
                    CoreMocks.mockDownloadModel.copy(
                        title = "Video 1",
                        downloadedState = DownloadedState.DOWNLOADING
                    ),
                    CoreMocks.mockDownloadModel.copy(
                        title = "Video 2",
                        downloadedState = DownloadedState.DOWNLOADING
                    )
                ),
                currentProgressId = CoreMocks.mockDownloadModel.id,
                currentProgressValue = 50,
                currentProgressSize = 100
            ),
            onBackClick = {},
            onDownloadClick = {}
        )
    }
}
