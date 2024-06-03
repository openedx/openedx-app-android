package org.openedx.profile.presentation.calendar

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
import org.openedx.core.ui.theme.appColors
import org.openedx.profile.R

enum class CalendarSyncState(
    @StringRes val title: Int,
    val icon: ImageVector
) {
    OFFLINE(R.string.profile_offline, Icons.Default.SyncDisabled),
    SYNC_FAILED(R.string.profile_syncing_failed, Icons.Rounded.FreeCancellation),
    SYNCED(R.string.profile_synced, Icons.Rounded.EventRepeat),
    SYNCHRONIZATION(R.string.profile_synchronization, Icons.Default.CloudSync);

    val tint: Color
        @Composable
        @ReadOnlyComposable
        get() = when (this) {
            OFFLINE -> MaterialTheme.appColors.textFieldHint
            SYNC_FAILED -> MaterialTheme.appColors.error
            SYNCED -> MaterialTheme.appColors.accessGreen
            SYNCHRONIZATION -> MaterialTheme.appColors.primary
        }
}