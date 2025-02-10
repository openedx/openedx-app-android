package org.openedx.profile.presentation.calendar

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.koin.androidx.compose.koinViewModel
import org.openedx.core.domain.model.CalendarData
import org.openedx.core.presentation.dialog.DefaultDialogBox
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.parcelable
import org.openedx.profile.R
import androidx.compose.ui.graphics.Color as ComposeColor
import org.openedx.core.R as coreR

class DisableCalendarSyncDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val viewModel: DisableCalendarSyncDialogViewModel = koinViewModel()
                DisableCalendarSyncDialogView(
                    calendarData = requireArguments().parcelable<CalendarData>(ARG_CALENDAR_DATA),
                    onCancelClick = {
                        dismiss()
                    },
                    onDisableSyncingClick = {
                        viewModel.disableSyncingClick()
                        dismiss()
                    }
                )
            }
        }
    }

    companion object {
        const val DIALOG_TAG = "DisableCalendarSyncDialogFragment"
        const val ARG_CALENDAR_DATA = "ARG_CALENDAR_DATA"

        fun newInstance(
            calendarData: CalendarData
        ): DisableCalendarSyncDialogFragment {
            val fragment = DisableCalendarSyncDialogFragment()
            fragment.arguments = bundleOf(
                ARG_CALENDAR_DATA to calendarData
            )
            return fragment
        }
    }
}

@Composable
private fun DisableCalendarSyncDialogView(
    modifier: Modifier = Modifier,
    calendarData: CalendarData?,
    onCancelClick: () -> Unit,
    onDisableSyncingClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    DefaultDialogBox(
        modifier = modifier,
        onDismissClick = onCancelClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = coreR.drawable.core_ic_warning),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.profile_disable_calendar_dialog_title),
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textDark
                )
            }
            calendarData?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.appShapes.cardShape)
                        .background(MaterialTheme.appColors.cardViewBackground)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(ComposeColor(calendarData.color))
                    )
                    Text(
                        text = calendarData.title,
                        style = MaterialTheme.appTypography.bodyMedium.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = MaterialTheme.appColors.textDark
                    )
                }
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    id = R.string.profile_disable_calendar_dialog_description,
                    calendarData?.title ?: ""
                ),
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )
            OpenEdXOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.profile_disable_syncing),
                backgroundColor = MaterialTheme.appColors.background,
                borderColor = MaterialTheme.appColors.primaryButtonBackground,
                textColor = MaterialTheme.appColors.primaryButtonBackground,
                onClick = {
                    onDisableSyncingClick()
                }
            )
            OpenEdXButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = coreR.string.core_cancel),
                onClick = {
                    onCancelClick()
                }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DisableCalendarSyncDialogPreview() {
    OpenEdXTheme {
        DisableCalendarSyncDialogView(
            calendarData = CalendarData("calendar", Color.GREEN),
            onCancelClick = { },
            onDisableSyncingClick = { }
        )
    }
}
