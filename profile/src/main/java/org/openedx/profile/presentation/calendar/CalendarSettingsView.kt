package org.openedx.profile.presentation.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.domain.model.CalendarData
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncState
import org.openedx.core.ui.OpenEdXOutlinedButton
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
import org.openedx.profile.presentation.ui.SettingsItem

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalendarSettingsView(
    windowSize: WindowSize,
    uiState: CalendarUIState,
    onCalendarSyncSwitchClick: (Boolean) -> Unit,
    onRelativeDateSwitchClick: (Boolean) -> Unit,
    onChangeSyncOptionClick: () -> Unit,
    onCourseToSyncClick: () -> Unit,
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
                        if (uiState.calendarData != null) {
                            CalendarSyncSection(
                                isCourseCalendarSyncEnabled = uiState.isCalendarSyncEnabled,
                                calendarData = uiState.calendarData,
                                calendarSyncState = uiState.calendarSyncState,
                                onCalendarSyncSwitchClick = onCalendarSyncSwitchClick,
                                onChangeSyncOptionClick = onChangeSyncOptionClick
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        if (uiState.coursesSynced != null) {
                            CoursesToSyncSection(
                                coursesSynced = uiState.coursesSynced,
                                onCourseToSyncClick = onCourseToSyncClick
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        OptionsSection(
                            isRelativeDatesEnabled = uiState.isRelativeDateEnabled,
                            onRelativeDateSwitchClick = onRelativeDateSwitchClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CalendarSyncSection(
    isCourseCalendarSyncEnabled: Boolean,
    calendarData: CalendarData,
    calendarSyncState: CalendarSyncState,
    onCalendarSyncSwitchClick: (Boolean) -> Unit,
    onChangeSyncOptionClick: () -> Unit
) {
    Column {
        SectionTitle(stringResource(id = R.string.profile_calendar_sync))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.appShapes.cardShape)
                .background(MaterialTheme.appColors.cardViewBackground)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(calendarData.color))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = calendarData.title,
                    style = MaterialTheme.appTypography.labelLarge,
                    color = MaterialTheme.appColors.textDark
                )
                Text(
                    text = stringResource(id = calendarSyncState.title),
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textFieldHint
                )
            }
            if (calendarSyncState == CalendarSyncState.SYNCHRONIZATION) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.appColors.primary
                )
            } else {
                Icon(
                    imageVector = calendarSyncState.icon,
                    tint = calendarSyncState.tint,
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.profile_course_calendar_sync),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Switch(
                    modifier = Modifier
                        .padding(0.dp),
                    checked = isCourseCalendarSyncEnabled,
                    onCheckedChange = onCalendarSyncSwitchClick,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.appColors.textAccent
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.profile_currently_syncing_events),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textPrimaryVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        SyncOptionsButton(
            onChangeSyncOptionClick = onChangeSyncOptionClick
        )
    }
}

@Composable
fun SyncOptionsButton(
    onChangeSyncOptionClick: () -> Unit
) {
    OpenEdXOutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.profile_change_sync_options),
        backgroundColor = MaterialTheme.appColors.background,
        borderColor = MaterialTheme.appColors.primaryButtonBackground,
        textColor = MaterialTheme.appColors.primaryButtonBackground,
        onClick = {
            onChangeSyncOptionClick()
        }
    )
}

@Composable
fun CoursesToSyncSection(
    coursesSynced: Int,
    onCourseToSyncClick: () -> Unit
) {
    Column {
        SectionTitle(stringResource(R.string.profile_courses_to_sync))
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier,
            shape = MaterialTheme.appShapes.cardShape,
            elevation = 0.dp,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            SettingsItem(
                text = stringResource(R.string.profile_syncing_courses, coursesSynced),
                onClick = onCourseToSyncClick
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.appTypography.labelLarge,
        color = MaterialTheme.appColors.textSecondary
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalendarSettingsViewPreview() {
    OpenEdXTheme {
        CalendarSettingsView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CalendarUIState(
                isCalendarExist = true,
                calendarData = CalendarData("calendar", Color.Red.toArgb()),
                calendarSyncState = CalendarSyncState.SYNCED,
                isCalendarSyncEnabled = false,
                isRelativeDateEnabled = true,
                coursesSynced = 5
            ),
            onBackClick = {},
            onCalendarSyncSwitchClick = {},
            onRelativeDateSwitchClick = {},
            onChangeSyncOptionClick = {},
            onCourseToSyncClick = {}
        )
    }
}
