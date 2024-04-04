package org.openedx.profile.presentation.profile.compose

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.profile.ProfileUIState
import org.openedx.profile.presentation.ui.ProfileInfoSection
import org.openedx.profile.presentation.ui.ProfileTopic

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun ProfileView(
    windowSize: WindowSize,
    uiState: ProfileUIState,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    onAction: (ProfileViewAction) -> Unit,
    onSettingsClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { onAction(ProfileViewAction.SwipeRefresh) })

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

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = topBarWidth,
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    modifier = Modifier
                        .testTag("txt_profile_title")
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.core_profile),
                    color = MaterialTheme.appColors.textPrimary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.appTypography.titleMedium
                )

                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    onClick = {
                        onSettingsClick()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.core_ic_settings),
                        tint = MaterialTheme.appColors.primary,
                        contentDescription = stringResource(id = R.string.core_accessibility_settings)
                    )
                }
            }
            Surface(
                color = MaterialTheme.appColors.background
            ) {
                Box(
                    modifier = Modifier.pullRefresh(pullRefreshState),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (uiState) {
                            is ProfileUIState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                }
                            }

                            is ProfileUIState.Data -> {
                                Column(
                                    Modifier
                                        .fillMaxHeight()
                                        .then(contentWidth)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    ProfileTopic(uiState.account)

                                    Spacer(modifier = Modifier.height(36.dp))

                                    ProfileInfoSection(uiState.account)


                                    OpenEdXOutlinedButton(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        text = stringResource(id = org.openedx.profile.R.string.profile_edit_profile),
                                        onClick = {
                                            onAction(ProfileViewAction.EditAccountClick)
                                        },
                                        borderColor = MaterialTheme.appColors.buttonBackground,
                                        textColor = MaterialTheme.appColors.textAccent
                                    )

                                    Spacer(Modifier.height(30.dp))
                                }
                            }
                        }
                    }
                    PullRefreshIndicator(
                        refreshing,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppVersionItem(
    versionName: String,
    appUpgradeEvent: AppUpgradeEvent?,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(20.dp)) {
        when (appUpgradeEvent) {
            is AppUpgradeEvent.UpgradeRecommendedEvent -> {
                AppVersionItemUpgradeRecommended(
                    versionName = versionName,
                    appUpgradeEvent = appUpgradeEvent,
                    onClick = onClick
                )
            }

            is AppUpgradeEvent.UpgradeRequiredEvent -> {
                AppVersionItemUpgradeRequired(
                    versionName = versionName,
                    onClick = onClick
                )
            }

            else -> {
                AppVersionItemAppToDate(
                    versionName = versionName
                )
            }
        }
    }
}

@Composable
private fun AppVersionItemAppToDate(versionName: String) {
    Column(
        Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            modifier = Modifier.testTag("txt_app_version_code"),
            text = stringResource(id = R.string.core_version, versionName),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                modifier = Modifier.size(
                    (MaterialTheme.appTypography.labelLarge.fontSize.value + 4).dp
                ),
                painter = painterResource(id = R.drawable.core_ic_check),
                contentDescription = null,
                tint = MaterialTheme.appColors.accessGreen
            )
            Text(
                modifier = Modifier.testTag("txt_up_to_date"),
                text = stringResource(id = R.string.core_up_to_date),
                color = MaterialTheme.appColors.textSecondary,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
    }
}

@Composable
private fun AppVersionItemUpgradeRecommended(
    versionName: String,
    appUpgradeEvent: AppUpgradeEvent.UpgradeRecommendedEvent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag("btn_upgrade_recommended")
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                modifier = Modifier.testTag("txt_app_version_code"),
                text = stringResource(id = R.string.core_version, versionName),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textPrimary
            )
            Text(
                modifier = Modifier.testTag("txt_upgrade_recommended"),
                text = stringResource(
                    id = R.string.core_tap_to_update_to_version,
                    appUpgradeEvent.newVersionName
                ),
                color = MaterialTheme.appColors.textAccent,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(id = R.drawable.core_ic_icon_upgrade),
            tint = MaterialTheme.appColors.primary,
            contentDescription = null
        )
    }
}

@Composable
fun AppVersionItemUpgradeRequired(
    versionName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag("btn_upgrade_required")
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(
                    modifier = Modifier
                        .size((MaterialTheme.appTypography.labelLarge.fontSize.value + 8).dp),
                    painter = painterResource(id = R.drawable.core_ic_warning),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.testTag("txt_app_version_code"),
                    text = stringResource(id = R.string.core_version, versionName),
                    style = MaterialTheme.appTypography.titleMedium,
                    color = MaterialTheme.appColors.textPrimary
                )
            }
            Text(
                modifier = Modifier.testTag("txt_upgrade_required"),
                text = stringResource(id = R.string.core_tap_to_install_required_app_update),
                color = MaterialTheme.appColors.textAccent,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(id = R.drawable.core_ic_icon_upgrade),
            tint = MaterialTheme.appColors.primary,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun AppVersionItemAppToDatePreview() {
    OpenEdXTheme {
        AppVersionItem(
            versionName = mockAppData.versionName,
            appUpgradeEvent = null,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun AppVersionItemUpgradeRecommendedPreview() {
    OpenEdXTheme {
        AppVersionItem(
            versionName = mockAppData.versionName,
            appUpgradeEvent = AppUpgradeEvent.UpgradeRecommendedEvent("1.0.1"),
            onClick = {}
        )
    }
}

@Preview
@Composable
fun AppVersionItemUpgradeRequiredPreview() {
    OpenEdXTheme {
        AppVersionItem(
            versionName = mockAppData.versionName,
            appUpgradeEvent = AppUpgradeEvent.UpgradeRequiredEvent,
            onClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() {
    OpenEdXTheme {
        ProfileView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = mockUiState,
            uiMessage = null,
            refreshing = false,
            onAction = {},
            onSettingsClick = {},
        )
    }
}


@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenTabletPreview() {
    OpenEdXTheme {
        ProfileView(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = mockUiState,
            uiMessage = null,
            refreshing = false,
            onAction = {},
            onSettingsClick = {},
        )
    }
}

private val mockAppData = AppData(
    versionName = "1.0.0",
)

val mockAccount = Account(
    username = "thom84",
    bio = "He as compliment unreserved projecting. Between had observe pretend delight for believe. Do newspaper questions consulted sweetness do. Our sportsman his unwilling fulfilled departure law.",
    requiresParentalConsent = true,
    name = "Thomas",
    country = "Ukraine",
    isActive = true,
    profileImage = ProfileImage("", "", "", "", false),
    yearOfBirth = 2000,
    levelOfEducation = "Bachelor",
    goals = "130",
    languageProficiencies = emptyList(),
    gender = "male",
    mailingAddress = "",
    "",
    null,
    accountPrivacy = Account.Privacy.ALL_USERS
)

private val mockUiState = ProfileUIState.Data(
    account = mockAccount
)

internal interface ProfileViewAction {
    object EditAccountClick : ProfileViewAction
    object SwipeRefresh : ProfileViewAction
}
