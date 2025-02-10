package org.openedx.profile.presentation.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.settingsHeaderBackground
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.profile.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalendarSetUpView(
    windowSize: WindowSize,
    useRelativeDates: Boolean,
    setUpCalendarSync: () -> Unit,
    onRelativeDateSwitchClick: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()

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
                        modifier = contentWidth
                            .verticalScroll(scrollState)
                            .padding(vertical = 28.dp),
                    ) {
                        Text(
                            modifier = Modifier.testTag("txt_calendar_sync"),
                            text = stringResource(id = R.string.profile_calendar_sync),
                            style = MaterialTheme.appTypography.labelLarge,
                            color = MaterialTheme.appColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            shape = MaterialTheme.appShapes.cardShape,
                            elevation = 0.dp,
                            backgroundColor = MaterialTheme.appColors.cardViewBackground
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(148.dp),
                                        tint = MaterialTheme.appColors.textDark,
                                        imageVector = Icons.Rounded.CalendarToday,
                                        contentDescription = null
                                    )
                                    Icon(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 30.dp)
                                            .height(60.dp),
                                        tint = MaterialTheme.appColors.textDark,
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    text = stringResource(id = R.string.profile_calendar_sync),
                                    style = MaterialTheme.appTypography.titleMedium,
                                    color = MaterialTheme.appColors.textDark
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    text = stringResource(id = R.string.profile_calendar_sync_description),
                                    style = MaterialTheme.appTypography.labelLarge,
                                    color = MaterialTheme.appColors.textDark
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OpenEdXButton(
                                    modifier = Modifier.fillMaxWidth(fraction = 0.75f),
                                    text = stringResource(id = R.string.profile_set_up_calendar_sync),
                                    onClick = {
                                        setUpCalendarSync()
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                        OptionsSection(
                            isRelativeDatesEnabled = useRelativeDates,
                            onRelativeDateSwitchClick = onRelativeDateSwitchClick
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
private fun CalendarScreenPreview() {
    OpenEdXTheme {
        CalendarSetUpView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            useRelativeDates = true,
            setUpCalendarSync = {},
            onRelativeDateSwitchClick = { _ -> },
            onBackClick = {}
        )
    }
}
