package org.openedx.profile.presentation.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.settingsHeaderBackground
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.windowSizeValue
import org.openedx.profile.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalendarSettingsView(
    windowSize: WindowSize,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
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
                    .fillMaxSize()
                    .padding(paddingValues)
                    .settingsHeaderBackground()
                    .statusBarsInset(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Toolbar(
                    modifier = topBarWidth
                        .displayCutoutForLandscape(),
                    label = stringResource(id = R.string.profile_dates_and_calendar),
                    canShowBackBtn = true,
                    labelTint = MaterialTheme.appColors.settingsTitleContent,
                    iconTint = MaterialTheme.appColors.settingsTitleContent,
                    onBackClick = onBackClick
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.appShapes.screenBackgroundShape)
                        .background(MaterialTheme.appColors.background)
                        .displayCutoutForLandscape(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = contentWidth.padding(vertical = 28.dp),
                    ) {
                        //TODO
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalendarSettingsViewPreview() {
    OpenEdXTheme {
        CalendarSettingsView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            onBackClick = {}
        )
    }
}
