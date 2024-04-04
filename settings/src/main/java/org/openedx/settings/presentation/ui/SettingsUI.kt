package org.openedx.settings.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openedx.core.extension.tagId
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

@Composable
fun SettingsItem(
    text: String,
    external: Boolean = false,
    onClick: () -> Unit
) {
    val icon = if (external) {
        Icons.AutoMirrored.Filled.OpenInNew
    } else {
        Icons.AutoMirrored.Filled.ArrowForwardIos
    }
    Row(
        Modifier
            .testTag("btn_${text.tagId()}")
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .testTag("txt_${text.tagId()}")
                .weight(1f),
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary
        )
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null
        )
    }
}

@Composable
fun SettingsTitle(
    modifier: Modifier = Modifier,
    title: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsInset()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            text = title,
            textAlign = TextAlign.Center,
            color = MaterialTheme.appColors.surface,
            style = MaterialTheme.appTypography.titleMedium
        )
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart),
            onClick = { onBackClick() }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                tint = MaterialTheme.appColors.surface,
                contentDescription = stringResource(id = org.openedx.core.R.string.core_accessibility_settings)
            )
        }
    }
}