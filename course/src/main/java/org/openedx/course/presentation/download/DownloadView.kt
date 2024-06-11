package org.openedx.course.presentation.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

@Composable
fun DownloadDialogItem(
    modifier: Modifier = Modifier,
    title: String,
    size: String
) {
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_core_chapter_icon),
            tint = MaterialTheme.appColors.textDark,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.appTypography.titleSmall,
            color = MaterialTheme.appColors.textDark
        )
        Text(
            text = size,
            style = MaterialTheme.appTypography.bodySmall,
            color = MaterialTheme.appColors.textFieldHint
        )
    }
}