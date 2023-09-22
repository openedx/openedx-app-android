package org.openedx.course.presentation.container

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
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
import org.openedx.core.extension.parcelable
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import java.util.*
import org.openedx.course.R as courseR

class NoAccessCourseContainerFragment : Fragment() {

    private var courseTitle = ""
    private var auditAccessExpires: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(requireArguments()) {
            courseTitle = getString(ARG_TITLE, "")
            auditAccessExpires = parcelable(ARG_AUDIT_ACCESS_EXPIRES)
        }
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
                val auditAccessExpired =
                    auditAccessExpires != null && Date().after(auditAccessExpires)
                NoAccessCourseContainerScreen(
                    windowSize = windowSize,
                    title = courseTitle,
                    auditAccessExpired = auditAccessExpired,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_AUDIT_ACCESS_EXPIRES = "auditAccessExpires"

        fun newInstance(
            title: String,
            auditAccessExpires: Date?
        ): NoAccessCourseContainerFragment {
            val fragment = NoAccessCourseContainerFragment()
            fragment.arguments = bundleOf(
                ARG_TITLE to title,
                ARG_AUDIT_ACCESS_EXPIRES to auditAccessExpires
            )
            return fragment
        }
    }

}


@Composable
private fun NoAccessCourseContainerScreen(
    windowSize: WindowSize,
    title: String,
    auditAccessExpired: Boolean,
    onBackClick: () -> Unit
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                screenWidth
            ) {
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
                        text = title,
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
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            modifier = Modifier.size(100.dp),
                            imageVector = Icons.Filled.Error,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (auditAccessExpired) {
                                stringResource(id = courseR.string.course_access_expired)
                            } else {
                                stringResource(id = courseR.string.course_not_started)
                            },
                            color = MaterialTheme.appColors.textPrimary,
                            style = MaterialTheme.appTypography.bodyLarge
                        )
                    }
                }

            }
        }
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NoAccessCourseContainerScreenPreview() {
    OpenEdXTheme {
        NoAccessCourseContainerScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            title = "Example title",
            auditAccessExpired = false,
            onBackClick = {}
        )
    }
}