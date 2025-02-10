package org.openedx.core.presentation.settings.calendarsync

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.openedx.core.R
import org.openedx.core.presentation.global.appupgrade.TransparentTextButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.takeIfNotEmpty
import androidx.compose.ui.window.DialogProperties as AlertDialogProperties
import org.openedx.core.R as CoreR

@Composable
fun CalendarSyncDialog(
    syncDialogType: CalendarSyncDialogType,
    calendarTitle: String,
    syncDialogPosAction: (CalendarSyncDialogType) -> Unit,
    syncDialogNegAction: (CalendarSyncDialogType) -> Unit,
    dismissSyncDialog: (CalendarSyncDialogType) -> Unit,
) {
    when (syncDialogType) {
        CalendarSyncDialogType.SYNC_DIALOG,
        CalendarSyncDialogType.UN_SYNC_DIALOG,
        -> {
            CalendarAlertDialog(
                dialogProperties = DialogProperties(
                    title = stringResource(syncDialogType.titleResId),
                    message = stringResource(syncDialogType.messageResId, calendarTitle),
                    positiveButton = stringResource(syncDialogType.positiveButtonResId),
                    negativeButton = stringResource(syncDialogType.negativeButtonResId),
                    positiveAction = { syncDialogPosAction(syncDialogType) },
                    negativeAction = { syncDialogNegAction(syncDialogType) },
                ),
                onDismiss = { dismissSyncDialog(syncDialogType) },
            )
        }

        CalendarSyncDialogType.PERMISSION_DIALOG -> {
            CalendarAlertDialog(
                dialogProperties = DialogProperties(
                    title = stringResource(
                        syncDialogType.titleResId,
                        stringResource(CoreR.string.platform_name)
                    ),
                    message = stringResource(
                        syncDialogType.messageResId,
                        stringResource(CoreR.string.platform_name),
                        stringResource(CoreR.string.platform_name)
                    ),
                    positiveButton = stringResource(syncDialogType.positiveButtonResId),
                    negativeButton = stringResource(syncDialogType.negativeButtonResId),
                    positiveAction = { syncDialogPosAction(syncDialogType) },
                    negativeAction = { syncDialogNegAction(syncDialogType) },
                ),
                onDismiss = { dismissSyncDialog(syncDialogType) }
            )
        }

        CalendarSyncDialogType.EVENTS_DIALOG -> {
            CalendarAlertDialog(
                dialogProperties = DialogProperties(
                    title = "",
                    message = stringResource(syncDialogType.messageResId, calendarTitle),
                    positiveButton = stringResource(syncDialogType.positiveButtonResId),
                    negativeButton = stringResource(syncDialogType.negativeButtonResId),
                    positiveAction = { syncDialogPosAction(syncDialogType) },
                    negativeAction = { syncDialogNegAction(syncDialogType) },
                ),
                onDismiss = { dismissSyncDialog(syncDialogType) }
            )
        }

        CalendarSyncDialogType.OUT_OF_SYNC_DIALOG -> {
            CalendarAlertDialog(
                dialogProperties = DialogProperties(
                    title = stringResource(syncDialogType.titleResId, calendarTitle),
                    message = stringResource(syncDialogType.messageResId),
                    positiveButton = stringResource(syncDialogType.positiveButtonResId),
                    negativeButton = stringResource(syncDialogType.negativeButtonResId),
                    positiveAction = { syncDialogPosAction(syncDialogType) },
                    negativeAction = { syncDialogNegAction(syncDialogType) },
                ),
                onDismiss = { dismissSyncDialog(syncDialogType) }
            )
        }

        CalendarSyncDialogType.LOADING_DIALOG -> {
            SyncDialog()
        }

        CalendarSyncDialogType.NONE -> {
        }
    }
}

@Composable
private fun CalendarAlertDialog(dialogProperties: DialogProperties, onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.background(
            color = MaterialTheme.appColors.background,
            shape = MaterialTheme.appShapes.cardShape
        ),
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.background,

        properties = AlertDialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        onDismissRequest = onDismiss,

        title = dialogProperties.title.takeIfNotEmpty()?.let {
            @Composable {
                Text(
                    text = dialogProperties.title,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            Text(
                text = dialogProperties.message,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        confirmButton = {
            TransparentTextButton(
                text = dialogProperties.positiveButton
            ) {
                onDismiss()
                dialogProperties.positiveAction.invoke()
            }
        },
        dismissButton = {
            TransparentTextButton(
                text = dialogProperties.negativeButton
            ) {
                onDismiss()
                dialogProperties.negativeAction.invoke()
            }
        },
    )
}

@Composable
private fun SyncDialog() {
    Dialog(
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.appShapes.cardShape),
                shape = MaterialTheme.appShapes.cardShape,
                color = MaterialTheme.appColors.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.core_title_syncing_calendar),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                    }
                }
            }
        }
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun CalendarSyncDialogsPreview(
    @PreviewParameter(CalendarSyncDialogTypeProvider::class) dialogType: CalendarSyncDialogType,
) {
    OpenEdXTheme {
        CalendarSyncDialog(
            syncDialogType = dialogType,
            calendarTitle = "Hello to OpenEdx",
            syncDialogPosAction = {},
            syncDialogNegAction = {},
            dismissSyncDialog = {},
        )
    }
}

private class CalendarSyncDialogTypeProvider : PreviewParameterProvider<CalendarSyncDialogType> {
    override val values = CalendarSyncDialogType.entries.dropLast(1).asSequence()
}
