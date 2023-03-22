package com.raccoongang.course.presentation.handouts

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.course.presentation.CourseRouter
import org.koin.android.ext.android.inject
import com.raccoongang.course.R as courseR

class HandoutsFragment : Fragment() {

    private val router by inject<CourseRouter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NewEdxTheme {
                val windowSize = rememberWindowSize()
                HandoutsScreen(
                    windowSize = windowSize,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onHandoutsClick = {
                        router.navigateToHandoutsWebView(
                            requireActivity().supportFragmentManager,
                            requireArguments().getString(ARG_COURSE_ID, ""),
                            getString(courseR.string.course_handouts),
                            HandoutsType.Handouts
                        )
                    },
                    onAnnouncementsClick = {
                        router.navigateToHandoutsWebView(
                            requireActivity().supportFragmentManager,
                            requireArguments().getString(ARG_COURSE_ID, ""),
                            getString(courseR.string.course_announcements),
                            HandoutsType.Announcements
                        )
                    })
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "argCourseId"
        fun newInstance(courseId: String): HandoutsFragment {
            val fragment = HandoutsFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }

}

@Composable
private fun HandoutsScreen(
    windowSize: WindowSize,
    onBackClick: () -> Unit,
    onHandoutsClick: () -> Unit,
    onAnnouncementsClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
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

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(it)
            .statusBarsInset(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(screenWidth) {
                Box(
                    Modifier
                        .fillMaxWidth()
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
                        text = stringResource(id = courseR.string.course_handouts),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.appColors.background
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onHandoutsClick() }
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(id = courseR.string.course_handouts),
                                        style = MaterialTheme.appTypography.titleLarge,
                                        color = MaterialTheme.appColors.textPrimary
                                    )
                                    Text(
                                        text = stringResource(id = courseR.string.course_find_important_info),
                                        style = MaterialTheme.appTypography.bodySmall,
                                        color = MaterialTheme.appColors.textPrimary
                                    )
                                }
                            }
                            Divider()
                        }
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onAnnouncementsClick() }
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Filled.Campaign, contentDescription = null)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(id = courseR.string.course_announcements),
                                        style = MaterialTheme.appTypography.titleLarge,
                                        color = MaterialTheme.appColors.textPrimary
                                    )
                                    Text(
                                        text = stringResource(id = courseR.string.course_latest_news),
                                        style = MaterialTheme.appTypography.bodySmall,
                                        color = MaterialTheme.appColors.textPrimary
                                    )
                                }
                            }
                            Divider()
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
private fun HandoutsScreenPreview() {
    NewEdxTheme {
        HandoutsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            onBackClick = {}, onHandoutsClick = {}, onAnnouncementsClick = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun HandoutsScreenTabletPreview() {
    NewEdxTheme {
        HandoutsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            onBackClick = {}, onHandoutsClick = {}, onAnnouncementsClick = {})
    }
}