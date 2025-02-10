package org.openedx.profile.presentation.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import org.koin.androidx.compose.koinViewModel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.settingsHeaderBackground
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.theme.fontFamily
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.profile.R
import org.openedx.core.R as coreR

class CoursesToSyncFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val viewModel: CoursesToSyncViewModel = koinViewModel()
                val uiState by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.collectAsState(initial = null)

                CoursesToSyncView(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    onHideInactiveCoursesSwitchClick = {
                        viewModel.setHideInactiveCoursesEnabled(it)
                    },
                    onCourseSyncCheckChange = { isEnabled, courseId ->
                        viewModel.setCourseSyncEnabled(isEnabled, courseId)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun CoursesToSyncView(
    windowSize: WindowSize,
    onBackClick: () -> Unit,
    uiState: CoursesToSyncUIState,
    uiMessage: UIMessage?,
    onHideInactiveCoursesSwitchClick: (Boolean) -> Unit,
    onCourseSyncCheckChange: (Boolean, String) -> Unit
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
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

        HandleUIMessage(
            uiMessage = uiMessage,
            scaffoldState = scaffoldState
        )

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
                    label = stringResource(id = R.string.profile_courses_to_sync),
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
                            .padding(vertical = 28.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_courses_to_sync_title),
                            style = MaterialTheme.appTypography.labelMedium,
                            color = MaterialTheme.appColors.textPrimaryVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        HideInactiveCoursesView(
                            isHideInactiveCourses = uiState.isHideInactiveCourses,
                            onHideInactiveCoursesSwitchClick = onHideInactiveCoursesSwitchClick
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        SyncCourseTabRow(
                            uiState = uiState,
                            onCourseSyncCheckChange = onCourseSyncCheckChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncCourseTabRow(
    uiState: CoursesToSyncUIState,
    onCourseSyncCheckChange: (Boolean, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(SyncCourseTab.SYNCED) }
    val selectedTabIndex = SyncCourseTab.entries.indexOf(selectedTab)

    Column {
        TabRow(
            modifier = Modifier
                .clip(MaterialTheme.appShapes.buttonShape)
                .border(
                    1.dp,
                    MaterialTheme.appColors.textAccent,
                    MaterialTheme.appShapes.buttonShape
                ),
            selectedTabIndex = selectedTabIndex,
            backgroundColor = MaterialTheme.appColors.background,
            indicator = {}
        ) {
            SyncCourseTab.entries.forEachIndexed { index, tab ->
                val backgroundColor = if (selectedTabIndex == index) {
                    MaterialTheme.appColors.textAccent
                } else {
                    MaterialTheme.appColors.background
                }
                Tab(
                    modifier = Modifier
                        .background(backgroundColor),
                    text = { Text(stringResource(id = tab.title)) },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTab = SyncCourseTab.entries[index] },
                    unselectedContentColor = MaterialTheme.appColors.textAccent,
                    selectedContentColor = MaterialTheme.appColors.background
                )
            }
        }

        CourseCheckboxList(
            selectedTab = selectedTab,
            uiState = uiState,
            onCourseSyncCheckChange = onCourseSyncCheckChange
        )
    }
}

@Composable
private fun CourseCheckboxList(
    selectedTab: SyncCourseTab,
    uiState: CoursesToSyncUIState,
    onCourseSyncCheckChange: (Boolean, String) -> Unit
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(8.dp),
        ) {
            val courseIds = uiState.coursesCalendarState
                .filter { it.isCourseSyncEnabled == (selectedTab == SyncCourseTab.SYNCED) }
                .map { it.courseId }
            val filteredEnrollments = uiState.enrollmentsStatus
                .filter { it.courseId in courseIds }
                .let { enrollments ->
                    if (uiState.isHideInactiveCourses) {
                        enrollments.filter { it.recentlyActive }
                    } else {
                        enrollments
                    }
                }
            if (filteredEnrollments.isEmpty()) {
                item {
                    EmptyListState(
                        selectedTab = selectedTab
                    )
                }
            } else {
                items(filteredEnrollments) { course ->
                    val isCourseSyncEnabled =
                        uiState.coursesCalendarState.find { it.courseId == course.courseId }?.isCourseSyncEnabled
                            ?: false
                    val annotatedString = buildAnnotatedString {
                        append(course.courseName)
                        if (!course.recentlyActive) {
                            append(" ")
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.sp,
                                    fontFamily = fontFamily,
                                    color = MaterialTheme.appColors.textFieldHint,
                                )
                            ) {
                                append(stringResource(R.string.profile_inactive))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            modifier = Modifier.size(24.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.appColors.primary,
                                uncheckedColor = MaterialTheme.appColors.textFieldText
                            ),
                            checked = isCourseSyncEnabled,
                            enabled = course.recentlyActive,
                            onCheckedChange = { isEnabled ->
                                onCourseSyncCheckChange(isEnabled, course.courseId)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.appTypography.labelLarge,
                            color = MaterialTheme.appColors.textDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyListState(
    modifier: Modifier = Modifier,
    selectedTab: SyncCourseTab,
) {
    val description = if (selectedTab == SyncCourseTab.SYNCED) {
        stringResource(id = R.string.profile_no_sync_courses)
    } else {
        stringResource(id = R.string.profile_no_courses_with_current_filter)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(96.dp),
            painter = painterResource(id = coreR.drawable.core_ic_book),
            tint = MaterialTheme.appColors.divider,
            contentDescription = null
        )
        Text(
            text = stringResource(
                id = R.string.profile_no_courses,
                stringResource(id = selectedTab.title)
            ),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textDark
        )
        Text(
            text = description,
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textDark,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun HideInactiveCoursesView(
    isHideInactiveCourses: Boolean,
    onHideInactiveCoursesSwitchClick: (Boolean) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.profile_hide_inactive_courses),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Switch(
                    modifier = Modifier
                        .padding(0.dp),
                    checked = isHideInactiveCourses,
                    onCheckedChange = onHideInactiveCoursesSwitchClick,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.appColors.textAccent
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_automatically_remove_events),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textPrimaryVariant
        )
    }
}

@Preview
@Composable
private fun CoursesToSyncViewPreview() {
    OpenEdXTheme {
        CoursesToSyncView(
            windowSize = rememberWindowSize(),
            uiState = CoursesToSyncUIState(
                enrollmentsStatus = emptyList(),
                coursesCalendarState = emptyList(),
                isHideInactiveCourses = true,
                isLoading = false
            ),
            uiMessage = null,
            onHideInactiveCoursesSwitchClick = {},
            onCourseSyncCheckChange = { _, _ -> },
            onBackClick = {}
        )
    }
}
