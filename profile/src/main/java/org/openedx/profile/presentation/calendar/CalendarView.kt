package org.openedx.profile.presentation.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.profile.R
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
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
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(
                    modifier = Modifier
                        .padding(0.dp),
                    checked = isRelativeDatesEnabled,
                    onCheckedChange = onRelativeDateSwitchClick,
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
