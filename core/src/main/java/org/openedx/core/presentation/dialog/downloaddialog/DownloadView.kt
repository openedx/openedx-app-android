package org.openedx.core.presentation.dialog.downloaddialog

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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.toFileSize

@Composable
fun DownloadDialogItem(
    modifier: Modifier = Modifier,
    downloadDialogItem: DownloadDialogItem,
) {
    val icon = if (downloadDialogItem.icon != null) {
        rememberVectorPainter(downloadDialogItem.icon)
    } else {
        painterResource(id = R.drawable.core_ic_chapter_icon)
    }
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Top),
            painter = icon,
            tint = MaterialTheme.appColors.textDark,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = downloadDialogItem.title,
            style = MaterialTheme.appTypography.titleSmall,
            color = MaterialTheme.appColors.textDark,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
        Text(
            text = downloadDialogItem.size.toFileSize(1, false),
            style = MaterialTheme.appTypography.bodySmall,
            color = MaterialTheme.appColors.textFieldHint
        )
    }
}
