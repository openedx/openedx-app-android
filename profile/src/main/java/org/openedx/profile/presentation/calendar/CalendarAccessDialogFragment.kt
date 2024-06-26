package org.openedx.profile.presentation.calendar

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.config.Config
import org.openedx.core.presentation.dialog.DefaultDialogBox
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.profile.R
import org.openedx.core.R as CoreR

class CalendarAccessDialogFragment : DialogFragment() {

    private val config by inject<Config>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                CalendarAccessDialog(
                    onCancelClick = {
                        dismiss()
                    },
                    onGrantCalendarAccessClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + config.getAppId())
                        )
                        startActivity(intent)
                        dismiss()
                    }
                )
            }
        }
    }

    companion object {
        const val DIALOG_TAG = "CalendarAccessDialogFragment"

        fun newInstance(): CalendarAccessDialogFragment {
            return CalendarAccessDialogFragment()
        }
    }
}

@Composable
private fun CalendarAccessDialog(
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onGrantCalendarAccessClick: () -> Unit
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
                    painter = painterResource(id = CoreR.drawable.core_ic_warning),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.profile_calendar_access_dialog_title),
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textDark
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.profile_calendar_access_dialog_description),
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )
            OpenEdXButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onGrantCalendarAccessClick()
                },
                content = {
                    TextIcon(
                        text = stringResource(id = R.string.profile_grant_access_calendar),
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        color = MaterialTheme.appColors.primaryButtonText,
                        textStyle = MaterialTheme.appTypography.labelLarge,
                        iconModifier = Modifier.padding(start = 4.dp)
                    )
                }
            )
            OpenEdXOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = CoreR.string.core_cancel),
                backgroundColor = MaterialTheme.appColors.background,
                borderColor = MaterialTheme.appColors.primaryButtonBackground,
                textColor = MaterialTheme.appColors.primaryButtonBackground,
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
private fun CalendarAccessDialogPreview() {
    OpenEdXTheme {
        CalendarAccessDialog(
            onCancelClick = { },
            onGrantCalendarAccessClick = { }
        )
    }
}
