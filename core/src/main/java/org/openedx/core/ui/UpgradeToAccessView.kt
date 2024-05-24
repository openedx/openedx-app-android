package org.openedx.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
fun UpgradeToAccessView(
    modifier: Modifier = Modifier,
    type: UpgradeToAccessViewType = UpgradeToAccessViewType.DASHBOARD,
    onClick: () -> Unit,
) {
    val shape = when (type) {
        UpgradeToAccessViewType.DASHBOARD -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        UpgradeToAccessViewType.COURSE -> MaterialTheme.appShapes.buttonShape
    }
    Row(
        modifier = modifier
            .clip(shape = shape)
            .fillMaxWidth()
            .background(color = MaterialTheme.appColors.primaryButtonBackground)
            .clickable {
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(end = 16.dp),
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.appColors.primaryButtonText
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.iap_upgrade_access_course),
            color = MaterialTheme.appColors.primaryButtonText,
            style = MaterialTheme.appTypography.labelLarge
        )
        Icon(
            modifier = Modifier.padding(start = 16.dp),
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.appColors.primaryButtonText
        )
    }
}

enum class UpgradeToAccessViewType {
    DASHBOARD,
    COURSE,
}

@Preview
@Composable
private fun UpgradeToAccessViewPreview(
    @PreviewParameter(UpgradeToAccessViewTypeParameterProvider::class) type: UpgradeToAccessViewType
) {
    OpenEdXTheme {
        UpgradeToAccessView(type = type) {}
    }
}

private class UpgradeToAccessViewTypeParameterProvider : PreviewParameterProvider<UpgradeToAccessViewType> {
    override val values = sequenceOf(
        UpgradeToAccessViewType.DASHBOARD,
        UpgradeToAccessViewType.COURSE,
    )
}
