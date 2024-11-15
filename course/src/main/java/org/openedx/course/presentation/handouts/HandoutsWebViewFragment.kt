package org.openedx.course.presentation.handouts

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.NoContentScreenType
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.NoContentScreen
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.WebContentScreen
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.R
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue

class HandoutsWebViewFragment : Fragment() {

    private val viewModel by viewModel<HandoutsViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TYPE, "")
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val title = if (HandoutsType.valueOf(viewModel.handoutsType) == HandoutsType.Handouts) {
            viewModel.logEvent(CourseAnalyticsEvent.HANDOUTS)
            getString(R.string.course_handouts)
        } else {
            viewModel.logEvent(CourseAnalyticsEvent.ANNOUNCEMENTS)
            getString(R.string.course_announcements)
        }

        setContent {
            OpenEdXTheme {
                val colorBackgroundValue = MaterialTheme.appColors.background.value
                val colorTextValue = MaterialTheme.appColors.textPrimary.value
                val uiState by viewModel.uiState.collectAsState()
                HandoutsScreens(
                    handoutType = HandoutsType.valueOf(viewModel.handoutsType),
                    uiState = uiState,
                    title = title,
                    apiHostUrl = viewModel.apiHostUrl,
                    onInjectDarkMode = {
                        viewModel.injectDarkMode(
                            (uiState as HandoutsUIState.HTMLContent).htmlContent,
                            colorBackgroundValue,
                            colorTextValue
                        )
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_TYPE = "argType"
        private const val ARG_COURSE_ID = "argCourse"

        fun newInstance(
            type: String,
            courseId: String,
        ): HandoutsWebViewFragment {
            val fragment = HandoutsWebViewFragment()
            fragment.arguments = bundleOf(
                ARG_TYPE to type,
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }
}

@Composable
fun HandoutsScreens(
    handoutType: HandoutsType,
    uiState: HandoutsUIState,
    title: String,
    apiHostUrl: String,
    onInjectDarkMode: () -> String,
    onBackClick: () -> Unit
) {
    val windowSize = rememberWindowSize()
    when (uiState) {
        is HandoutsUIState.Loading -> {
            CircularProgress()
        }

        is HandoutsUIState.HTMLContent -> {
            WebContentScreen(
                windowSize = windowSize,
                apiHostUrl = apiHostUrl,
                title = title,
                htmlBody = onInjectDarkMode(),
                onBackClick = onBackClick
            )
        }

        HandoutsUIState.Error -> {
            HandoutsEmptyScreen(
                windowSize = windowSize,
                handoutType = handoutType,
                title = title,
                onBackClick = onBackClick
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HandoutsEmptyScreen(
    windowSize: WindowSize,
    handoutType: HandoutsType,
    title: String,
    onBackClick: () -> Unit
) {
    val handoutScreenType =
        if (handoutType == HandoutsType.Handouts) {
            NoContentScreenType.COURSE_HANDOUTS
        } else {
            NoContentScreenType.COURSE_ANNOUNCEMENTS
        }

    val scaffoldState = rememberScaffoldState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
            .semantics {
                testTagsAsResourceId = true
            },
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(screenWidth) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Toolbar(
                        label = title,
                        canShowBackBtn = true,
                        onBackClick = onBackClick
                    )
                }
                Surface(
                    Modifier.fillMaxSize(),
                    color = MaterialTheme.appColors.background
                ) {
                    NoContentScreen(noContentScreenType = handoutScreenType)
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HandoutsScreensPreview() {
    HandoutsScreens(
        handoutType = HandoutsType.Handouts,
        uiState = HandoutsUIState.HTMLContent(htmlContent = ""),
        title = "Handouts",
        apiHostUrl = "http://localhost:8000",
        onInjectDarkMode = { "" },
        onBackClick = { }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
fun HandoutsScreensTabletPreview() {
    HandoutsScreens(
        handoutType = HandoutsType.Handouts,
        uiState = HandoutsUIState.HTMLContent(htmlContent = ""),
        title = "Handouts",
        apiHostUrl = "http://localhost:8000",
        onInjectDarkMode = { "" },
        onBackClick = { }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyHandoutsScreensPreview() {
    OpenEdXTheme(darkTheme = true) {
        HandoutsScreens(
            handoutType = HandoutsType.Handouts,
            uiState = HandoutsUIState.Error,
            title = "Handouts",
            apiHostUrl = "http://localhost:8000",
            onInjectDarkMode = { "" },
            onBackClick = { }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyAnnouncementsScreensPreview() {
    OpenEdXTheme(darkTheme = true) {
        HandoutsScreens(
            handoutType = HandoutsType.Announcements,
            uiState = HandoutsUIState.Error,
            title = "Handouts",
            apiHostUrl = "http://localhost:8000",
            onInjectDarkMode = { "" },
            onBackClick = { }
        )
    }
}
