package org.openedx.core.presentation.global.appupgrade

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
fun AppUpgradeRequiredScreen(
    modifier: Modifier = Modifier,
    onUpdateClick: () -> Unit
) {
    AppUpgradeRequiredScreen(
        modifier = modifier,
        showAccountSettingsButton = false,
        onAccountSettingsClick = {},
        onUpdateClick = onUpdateClick
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppUpgradeRequiredScreen(
    modifier: Modifier = Modifier,
    showAccountSettingsButton: Boolean,
    onAccountSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.appColors.background)
            .statusBarsInset()
            .semantics { testTagsAsResourceId = true },
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            modifier = Modifier
                .testTag("txt_app_upgrade_deprecated")
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp),
            text = stringResource(id = R.string.core_deprecated_app_version),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleMedium,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AppUpgradeRequiredContent(
                modifier = Modifier.padding(horizontal = 32.dp),
                showAccountSettingsButton = showAccountSettingsButton,
                onAccountSettingsClick = onAccountSettingsClick,
                onUpdateClick = onUpdateClick
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppUpgradeRecommendDialog(
    modifier: Modifier = Modifier,
    onNotNowClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    val orientation = LocalConfiguration.current.orientation
    val imageModifier = if (orientation == ORIENTATION_LANDSCAPE) {
        Modifier.size(60.dp)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        color = Color.Transparent
    ) {
        Box(
            modifier = modifier
                .testTag("btn_upgrade_dialog_not_now")
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .noRippleClickable {
                    onNotNowClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.appShapes.cardShape)
                    .noRippleClickable {}
                    .background(
                        color = MaterialTheme.appColors.background,
                        shape = MaterialTheme.appShapes.cardShape
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Image(
                        modifier = imageModifier,
                        painter = painterResource(id = R.drawable.core_ic_icon_upgrade),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.testTag("txt_app_upgrade_title"),
                        text = stringResource(id = R.string.core_app_upgrade_title),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    Text(
                        modifier = Modifier.testTag("txt_app_upgrade_description"),
                        text = stringResource(id = R.string.core_app_upgrade_dialog_description),
                        color = MaterialTheme.appColors.textPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.appTypography.bodyMedium
                    )
                    AppUpgradeDialogButtons(
                        onNotNowClick = onNotNowClick,
                        onUpdateClick = onUpdateClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppUpgradeRequiredContent(
    modifier: Modifier = Modifier,
    showAccountSettingsButton: Boolean,
    onAccountSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Column(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.core_ic_warning),
            contentDescription = null
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.testTag("txt_app_upgrade_required_title"),
                text = stringResource(id = R.string.core_app_update_required_title),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            Text(
                modifier = Modifier.testTag("txt_app_upgrade_required_description"),
                text = stringResource(id = R.string.core_app_update_required_description),
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.appTypography.bodyMedium
            )
        }
        AppUpgradeRequiredButtons(
            showAccountSettingsButton = showAccountSettingsButton,
            onAccountSettingsClick = onAccountSettingsClick,
            onUpdateClick = onUpdateClick
        )
    }
}

@Composable
fun AppUpgradeRequiredButtons(
    showAccountSettingsButton: Boolean,
    onAccountSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (showAccountSettingsButton) {
            TransparentTextButton(
                text = stringResource(id = R.string.core_account_settings),
                onClick = onAccountSettingsClick
            )
        }
        DefaultTextButton(
            text = stringResource(id = R.string.core_update),
            onClick = onUpdateClick
        )
    }
}

@Composable
fun AppUpgradeDialogButtons(
    onNotNowClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TransparentTextButton(
            text = stringResource(id = R.string.core_not_now),
            onClick = onNotNowClick
        )
        DefaultTextButton(
            text = stringResource(id = R.string.core_update),
            onClick = onUpdateClick
        )
    }
}

@Composable
fun TransparentTextButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .testTag("btn_secondary")
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier.testTag("txt_secondary"),
            color = MaterialTheme.appColors.textAccent,
            style = MaterialTheme.appTypography.labelLarge,
            text = text
        )
    }
}

@Composable
fun DefaultTextButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .testTag("btn_primary")
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.appColors.primaryButtonBackground
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.testTag("txt_primary"),
                text = text,
                color = MaterialTheme.appColors.primaryButtonText,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
    }
}

@Composable
fun AppUpgradeRecommendedBox(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag("btn_upgrade_box")
            .fillMaxWidth()
            .padding(20.dp)
            .clickable {
                onClick()
            },
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(id = R.drawable.core_ic_icon_upgrade),
                contentDescription = null,
                tint = Color.White
            )
            Column {
                Text(
                    modifier = Modifier.testTag("txt_app_upgrade_title"),
                    text = stringResource(id = R.string.core_app_upgrade_title),
                    color = Color.White,
                    style = MaterialTheme.appTypography.titleMedium
                )
                Text(
                    modifier = Modifier.testTag("txt_app_upgrade_description"),
                    text = stringResource(id = R.string.core_app_upgrade_box_description),
                    color = Color.White,
                    style = MaterialTheme.appTypography.bodyMedium
                )
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppUpgradeRequiredScreenPreview() {
    OpenEdXTheme {
        AppUpgradeRequiredScreen(
            showAccountSettingsButton = true,
            onAccountSettingsClick = {},
            onUpdateClick = {}
        )
    }
}

@Preview
@Composable
private fun AppUpgradeRecommendedBoxPreview() {
    OpenEdXTheme {
        AppUpgradeRecommendedBox(
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun AppUpgradeDialogButtonsPreview() {
    OpenEdXTheme {
        AppUpgradeDialogButtons(
            onNotNowClick = {},
            onUpdateClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppUpgradeRecommendDialogPreview() {
    OpenEdXTheme {
        AppUpgradeRecommendDialog(
            onNotNowClick = {},
            onUpdateClick = {}
        )
    }
}
