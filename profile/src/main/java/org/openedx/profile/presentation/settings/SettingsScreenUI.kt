package org.openedx.profile.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.openedx.core.R
import org.openedx.core.domain.model.AgreementUrls
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.notifier.app.AppUpgradeEvent
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
import org.openedx.profile.domain.model.Configuration
import org.openedx.profile.presentation.ui.SettingsDivider
import org.openedx.profile.presentation.ui.SettingsItem
import org.openedx.profile.R as profileR

@Composable
internal fun SettingsScreen(
    windowSize: WindowSize,
    uiState: SettingsUIState,
    appUpgradeEvent: AppUpgradeEvent?,
    onBackClick: () -> Unit,
    onAction: (SettingsScreenAction) -> Unit,
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .settingsHeaderBackground()
            .statusBarsInset()
    ) {
        Toolbar(
            modifier = topBarWidth
                .align(Alignment.CenterHorizontally)
                .displayCutoutForLandscape(),
            label = stringResource(id = R.string.core_settings),
            canShowBackBtn = true,
            labelTint = MaterialTheme.appColors.settingsTitleContent,
            iconTint = MaterialTheme.appColors.settingsTitleContent,
            onBackClick = onBackClick
        )

        if (showLogoutDialog) {
            LogoutDialog(
                onDismissRequest = {
                    showLogoutDialog = false
                },
                onLogoutClick = {
                    showLogoutDialog = false
                    onAction(SettingsScreenAction.LogoutClick)
                }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                shape = MaterialTheme.appShapes.screenBackgroundShape,
                color = MaterialTheme.appColors.background
            ) {
                Box(
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .displayCutoutForLandscape(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (uiState) {
                            is SettingsUIState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                }
                            }

                            is SettingsUIState.Data -> {
                                Column(
                                    Modifier
                                        .fillMaxHeight()
                                        .then(contentWidth)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(Modifier.height(30.dp))

                                    ManageAccountSection(onManageAccountClick = {
                                        onAction(SettingsScreenAction.ManageAccountClick)
                                    })

                                    Spacer(modifier = Modifier.height(24.dp))

                                    SettingsSection(
                                        onVideoSettingsClick = {
                                            onAction(SettingsScreenAction.VideoSettingsClick)
                                        },
                                        onCalendarSettingsClick = {
                                            onAction(SettingsScreenAction.CalendarSettingsClick)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    SupportInfoSection(
                                        uiState = uiState,
                                        onAction = onAction,
                                        appUpgradeEvent = appUpgradeEvent,
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    LogoutButton(
                                        onClick = { showLogoutDialog = true }
                                    )

                                    Spacer(Modifier.height(30.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    onVideoSettingsClick: () -> Unit,
    onCalendarSettingsClick: () -> Unit
) {
    Column {
        Text(
            modifier = Modifier.testTag("txt_settings"),
            text = stringResource(id = R.string.core_settings),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier,
            shape = MaterialTheme.appShapes.cardShape,
            elevation = 0.dp,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            Column(Modifier.fillMaxWidth()) {
                SettingsItem(
                    text = stringResource(id = profileR.string.profile_video),
                    onClick = onVideoSettingsClick
                )
                SettingsDivider()
                SettingsItem(
                    text = stringResource(id = profileR.string.profile_dates_and_calendar),
                    onClick = onCalendarSettingsClick
                )
            }
        }
    }
}

@Composable
private fun ManageAccountSection(onManageAccountClick: () -> Unit) {
    Column {
        Card(
            shape = MaterialTheme.appShapes.cardShape,
            elevation = 0.dp,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            Column(Modifier.fillMaxWidth()) {
                SettingsItem(
                    text = stringResource(id = R.string.core_manage_account),
                    onClick = onManageAccountClick
                )
            }
        }
    }
}

@Composable
private fun SupportInfoSection(
    uiState: SettingsUIState.Data,
    appUpgradeEvent: AppUpgradeEvent?,
    onAction: (SettingsScreenAction) -> Unit
) {
    Column {
        Text(
            modifier = Modifier.testTag("txt_support_info"),
            text = stringResource(id = profileR.string.profile_support_info),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier,
            shape = MaterialTheme.appShapes.cardShape,
            elevation = 0.dp,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            Column(Modifier.fillMaxWidth()) {
                if (uiState.configuration.supportEmail.isNotBlank()) {
                    SettingsItem(text = stringResource(id = profileR.string.profile_contact_support)) {
                        onAction(SettingsScreenAction.SupportClick)
                    }
                    SettingsDivider()
                }
                if (uiState.configuration.agreementUrls.tosUrl.isNotBlank()) {
                    SettingsItem(text = stringResource(id = R.string.core_terms_of_use)) {
                        onAction(SettingsScreenAction.TermsClick)
                    }
                    SettingsDivider()
                }
                if (uiState.configuration.agreementUrls.privacyPolicyUrl.isNotBlank()) {
                    SettingsItem(text = stringResource(id = R.string.core_privacy_policy)) {
                        onAction(SettingsScreenAction.PrivacyPolicyClick)
                    }
                    SettingsDivider()
                }
                if (uiState.configuration.agreementUrls.cookiePolicyUrl.isNotBlank()) {
                    SettingsItem(text = stringResource(id = R.string.core_cookie_policy)) {
                        onAction(SettingsScreenAction.CookiePolicyClick)
                    }
                    SettingsDivider()
                }
                if (uiState.configuration.agreementUrls.dataSellConsentUrl.isNotBlank()) {
                    SettingsItem(text = stringResource(id = R.string.core_data_sell)) {
                        onAction(SettingsScreenAction.DataSellClick)
                    }
                    SettingsDivider()
                }
                if (uiState.configuration.faqUrl.isNotBlank()) {
                    val uriHandler = LocalUriHandler.current
                    SettingsItem(
                        text = stringResource(id = R.string.core_faq),
                        external = true,
                    ) {
                        uriHandler.openUri(uiState.configuration.faqUrl)
                        onAction(SettingsScreenAction.FaqClick)
                    }
                    SettingsDivider()
                }
                AppVersionItem(
                    versionName = uiState.configuration.versionName,
                    appUpgradeEvent = appUpgradeEvent,
                ) {
                    onAction(SettingsScreenAction.AppVersionClick)
                }
            }
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .testTag("btn_logout")
            .fillMaxWidth(),
        shape = MaterialTheme.appShapes.cardShape,
        elevation = 0.dp,
        backgroundColor = MaterialTheme.appColors.cardViewBackground
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    onClick()
                }
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.testTag("txt_logout"),
                text = stringResource(id = profileR.string.profile_logout),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.error
            )
            Icon(
                painterResource(id = profileR.drawable.profile_ic_logout),
                contentDescription = null,
                tint = MaterialTheme.appColors.error
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LogoutDialog(
    onDismissRequest: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.appColors.background,
                        MaterialTheme.appShapes.cardShape
                    )
                    .clip(MaterialTheme.appShapes.cardShape)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.cardViewBorder,
                        MaterialTheme.appShapes.cardShape
                    )
                    .padding(horizontal = 40.dp, vertical = 36.dp)
                    .semantics { testTagsAsResourceId = true },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        modifier = Modifier
                            .testTag("ib_close")
                            .size(24.dp),
                        onClick = onDismissRequest
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.core_cancel),
                            tint = MaterialTheme.appColors.primary
                        )
                    }
                }
                Icon(
                    modifier = Modifier
                        .width(88.dp)
                        .height(85.dp),
                    painter = painterResource(profileR.drawable.profile_ic_exit),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.onBackground
                )
                Spacer(Modifier.size(36.dp))
                Text(
                    modifier = Modifier.testTag("txt_logout_dialog_title"),
                    text = stringResource(id = profileR.string.profile_logout_dialog_body),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(36.dp))
                OpenEdXButton(
                    text = stringResource(id = profileR.string.profile_logout),
                    backgroundColor = MaterialTheme.appColors.warning,
                    onClick = onLogoutClick,
                    content = {
                        Box(
                            Modifier
                                .testTag("btn_logout")
                                .fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                modifier = Modifier
                                    .testTag("txt_logout")
                                    .fillMaxWidth(),
                                text = stringResource(id = profileR.string.profile_logout),
                                color = MaterialTheme.appColors.textWarning,
                                style = MaterialTheme.appTypography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                            Icon(
                                modifier = Modifier
                                    .testTag("ic_logout"),
                                painter = painterResource(id = profileR.drawable.profile_ic_logout),
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    }
                )
            }
        }
    )
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
                    size = (MaterialTheme.appTypography.labelLarge.fontSize.value + 4).dp
                ),
                painter = painterResource(id = R.drawable.core_ic_check),
                contentDescription = null,
                tint = MaterialTheme.appColors.successGreen
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
                        .size(size = (MaterialTheme.appTypography.labelLarge.fontSize.value + 8).dp),
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

private val mockAppData = AppData(
    appName = "openedx",
    versionName = "1.0.0",
    applicationId = "org.example.com"
)

private val mockConfiguration = Configuration(
    agreementUrls = AgreementUrls(),
    faqUrl = "https://example.com/faq",
    supportEmail = "test@example.com",
    versionName = mockAppData.versionName,
)

private val mockUiState = SettingsUIState.Data(
    configuration = mockConfiguration
)

@Preview
@Composable
private fun AppVersionItemAppToDatePreview() {
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
private fun AppVersionItemUpgradeRecommendedPreview() {
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
private fun AppVersionItemUpgradeRequiredPreview() {
    OpenEdXTheme {
        AppVersionItem(
            versionName = mockAppData.versionName,
            appUpgradeEvent = AppUpgradeEvent.UpgradeRequiredEvent,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun LogoutDialogPreview() {
    LogoutDialog({}, {})
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    OpenEdXTheme {
        SettingsScreen(
            onBackClick = {},
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = mockUiState,
            onAction = {},
            appUpgradeEvent = null,
        )
    }
}
