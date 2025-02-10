package org.openedx.course.presentation.handouts

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.presentation.ui.CardArrow
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.course.R as courseR

@Composable
fun HandoutsScreen(
    windowSize: WindowSize,
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = screenWidth,
                color = MaterialTheme.appColors.background
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 24.dp)
                ) {
                    item {
                        HandoutsItem(
                            title = stringResource(id = courseR.string.course_handouts),
                            description = stringResource(id = courseR.string.course_find_important_info),
                            painter = painterResource(id = courseR.drawable.course_ic_handouts),
                            onClick = onHandoutsClick
                        )
                    }
                    item {
                        HandoutsItem(
                            title = stringResource(id = courseR.string.course_announcements),
                            description = stringResource(id = courseR.string.course_latest_news),
                            painter = painterResource(id = courseR.drawable.course_ic_announcements),
                            onClick = onAnnouncementsClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HandoutsItem(
    title: String,
    description: String,
    painter: Painter,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.appTypography.titleSmall,
                    color = MaterialTheme.appColors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textFieldHint
                )
            }
        }
        CardArrow(degrees = 0f)
    }
    Divider()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HandoutsScreenPreview() {
    OpenEdXTheme {
        HandoutsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            onHandoutsClick = {},
            onAnnouncementsClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun HandoutsScreenTabletPreview() {
    OpenEdXTheme {
        HandoutsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            onHandoutsClick = {},
            onAnnouncementsClick = {}
        )
    }
}
