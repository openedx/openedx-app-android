package org.openedx.core.presentation.settings.calendarsync

import androidx.annotation.StringRes
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FreeCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors

enum class CalendarSyncState(
    @StringRes val title: Int,
    @StringRes val longTitle: Int,
    val icon: ImageVector
) {
    OFFLINE(
        R.string.core_offline,
        R.string.core_offline,
        Icons.Default.SyncDisabled
    ),
    SYNC_FAILED(
        R.string.core_syncing_failed,
        R.string.core_calendar_sync_failed,
        Icons.Rounded.FreeCancellation
    ),
    SYNCED(
        R.string.core_to_sync,
        R.string.core_synced_to_calendar,
        Icons.Rounded.EventRepeat
    ),
    SYNCHRONIZATION(
        R.string.core_syncing_to_calendar,
        R.string.core_syncing_to_calendar,
        Icons.Default.CloudSync
    );

    val tint: Color
        @Composable
        @ReadOnlyComposable
        get() = when (this) {
            OFFLINE -> MaterialTheme.appColors.textFieldHint
            SYNC_FAILED -> MaterialTheme.appColors.error
            SYNCED -> MaterialTheme.appColors.successGreen
            SYNCHRONIZATION -> MaterialTheme.appColors.primary
        }
}
