package org.openedx.core.presentation.global.app_upgrade

import androidx.compose.foundation.Image
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.IconText
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
fun AppUpgradeRequiredScreen(
    modifier: Modifier = Modifier,
    onAccountSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsInset(),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            modifier = Modifier
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
                onAccountSettingsClick = onAccountSettingsClick,
                onUpdateClick = onUpdateClick
            )
        }
    }
}

@Composable
fun AppUpgradeDialogContent(
    modifier: Modifier = Modifier,
    onNotNowClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.core_ic_icon_upgrade),
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.core_app_upgrade_title),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleMedium
        )
        Text(
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

@Composable
fun AppUpgradeRequiredContent(
    modifier: Modifier = Modifier,
    onAccountSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Column(
        modifier = modifier,
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
                text = stringResource(id = R.string.core_app_update_required_title),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.core_app_update_required_description),
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.appTypography.bodyMedium
            )
            IconText(
                text = stringResource(R.string.core_why_do_you_need_update),
                painter = painterResource(id = R.drawable.core_ic_question),
                textStyle = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.primary,
                onClick = {
                    //TODO
                }
            )
        }
        AppUpgradeRequiredButtons(
            onAccountSettingsClick = onAccountSettingsClick,
            onUpdateClick = onUpdateClick
        )
    }
}

@Composable
fun AppUpgradeRequiredButtons(
    onAccountSettingsClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AppUpgradeTransparentTextButton(
            text = stringResource(id = R.string.core_account_settings),
            onNotNowClick = onAccountSettingsClick
        )
        UpdateButton(
            onUpdateClick = onUpdateClick
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
        AppUpgradeTransparentTextButton(
            text = stringResource(id = R.string.core_not_now),
            onNotNowClick = onNotNowClick
        )
        UpdateButton(
            onUpdateClick = onUpdateClick
        )
    }
}

@Composable
fun AppUpgradeTransparentTextButton(
    text: String,
    onNotNowClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        onClick = onNotNowClick
    ) {
        Text(
            color = MaterialTheme.appColors.textAccent,
            style = MaterialTheme.appTypography.labelLarge,
            text = text
        )
    }
}

@Composable
fun UpdateButton(
    onUpdateClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.appColors.buttonBackground
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        onClick = onUpdateClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.core_update),
                color = MaterialTheme.appColors.buttonText,
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
                    text = stringResource(id = R.string.core_app_upgrade_title),
                    color = Color.White,
                    style = MaterialTheme.appTypography.titleMedium
                )
                Text(
                    text = stringResource(id = R.string.core_app_upgrade_box_description),
                    color = Color.White,
                    style = MaterialTheme.appTypography.bodyMedium
                )
            }
        }
    }
}

@Preview
@Composable
fun AppUpgradeRecommendedBoxPreview() {
    AppUpgradeRecommendedBox(
        onClick = {}
    )
}

@Preview
@Composable
fun AppUpgradeDialogButtonsPreview() {
    AppUpgradeDialogButtons(
        onNotNowClick = {},
        onUpdateClick = {}
    )
}

@Preview
@Composable
fun AppUpgradeDialogContentPreview() {
    AppUpgradeDialogContent(
        onNotNowClick = {},
        onUpdateClick = {}
    )
}