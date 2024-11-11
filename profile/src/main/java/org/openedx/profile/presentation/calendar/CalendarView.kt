package org.openedx.profile.presentation.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.profile.R
import java.util.Date

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OptionsSection(
    isRelativeDatesEnabled: Boolean,
    onRelativeDateSwitchClick: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val textDescription = if (isRelativeDatesEnabled) {
        stringResource(R.string.profile_show_relative_dates)
    } else {
        stringResource(
            R.string.profile_show_full_dates,
            TimeUtils.formatToString(context, Date(), false)
        )
    }
    Column {
        SectionTitle(stringResource(R.string.profile_options))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.profile_use_relative_dates),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Switch(
                    modifier = Modifier
                        .padding(0.dp),
                    checked = isRelativeDatesEnabled,
                    onCheckedChange = onRelativeDateSwitchClick,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.appColors.textAccent
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = textDescription,
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textPrimaryVariant
        )
    }
}
