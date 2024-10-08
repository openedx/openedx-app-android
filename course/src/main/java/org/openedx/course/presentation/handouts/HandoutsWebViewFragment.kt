package org.openedx.course.presentation.handouts

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.ui.WebContentScreen
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.R
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize

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
                val windowSize = rememberWindowSize()

                val htmlBody by viewModel.htmlContent.observeAsState("")
                val colorBackgroundValue = MaterialTheme.appColors.background.value
                val colorTextValue = MaterialTheme.appColors.textPrimary.value

                WebContentScreen(
                    windowSize = windowSize,
                    apiHostUrl = viewModel.apiHostUrl,
                    title = title,
                    htmlBody = viewModel.injectDarkMode(
                        htmlBody,
                        colorBackgroundValue,
                        colorTextValue
                    ),
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    })
            }
        }
    }

    companion object {
        private val ARG_TYPE = "argType"
        private val ARG_COURSE_ID = "argCourse"

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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WebContentScreenPreview() {
    WebContentScreen(
        windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
        apiHostUrl = "http://localhost:8000",
        title = "Handouts", onBackClick = { }, htmlBody = ""
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
fun WebContentScreenTabletPreview() {
    WebContentScreen(
        windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
        apiHostUrl = "http://localhost:8000",
        title = "Handouts", onBackClick = { }, htmlBody = ""
    )
}
