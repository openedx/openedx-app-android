package org.openedx.core.presentation.dialog.alert

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.CoreAnalyticsEvent
import org.openedx.core.presentation.CoreAnalyticsKey
import org.openedx.core.presentation.global.appupgrade.DefaultTextButton
import org.openedx.core.presentation.global.appupgrade.TransparentTextButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.utils.UrlUtils

class ActionDialogFragment : DialogFragment() {

    private val config by inject<Config>()
    private val analytics: CoreAnalytics by inject()
    private lateinit var url: String
    private lateinit var screen: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                url = requireArguments().getString(ARG_URL, "")
                screen = requireArguments().getString(ARG_SCREEN, "")
                ActionDialog(
                    title = requireArguments().getString(ARG_TITLE, ""),
                    message = requireArguments().getString(ARG_MESSAGE, ""),
                    onPositiveClick = {
                        logDialogActionEvent(CoreAnalyticsKey.CANCEL.key)
                        dismiss()
                    },
                    onNegativeClick = {
                        UrlUtils.openInBrowser(
                            activity = context,
                            apiHostUrl = config.getApiHostURL(),
                            url = url,
                        )
                        logDialogActionEvent(CoreAnalyticsKey.CONTINUE.key)
                        dismiss()
                    }
                )
                logDialogEvent(event = CoreAnalyticsEvent.EXTERNAL_LINK_OPENING_ALERT)
            }
        }
    }

    private fun logDialogActionEvent(action: String) {
        logDialogEvent(
            event = CoreAnalyticsEvent.EXTERNAL_LINK_OPENING_ALERT_ACTION,
            action = action
        )
    }

    private fun logDialogEvent(
        event: CoreAnalyticsEvent,
        action: String? = null,
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CoreAnalyticsKey.NAME.key, event.biValue)
                put(CoreAnalyticsKey.CATEGORY.key, CoreAnalyticsKey.DISCOVERY.key)
                put(CoreAnalyticsKey.URL.key, url)
                put(CoreAnalyticsKey.SCREEN_NAME.key, screen)
                action?.let { put(CoreAnalyticsKey.ACTION.key, action) }
            }
        )
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_URL = "url"
        private const val ARG_SCREEN = "screen"

        fun newInstance(
            title: String,
            message: String,
            url: String,
            source: String,
        ): ActionDialogFragment {
            val fragment = ActionDialogFragment()
            fragment.arguments = bundleOf(
                ARG_TITLE to title,
                ARG_MESSAGE to message,
                ARG_URL to url,
                ARG_SCREEN to source
            )
            return fragment
        }
    }
}

@Composable
private fun ActionDialog(
    title: String,
    message: String,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.cardShape)
            .background(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.cardShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            Text(
                text = message,
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.appTypography.bodyMedium
            )
            Row {
                TransparentTextButton(
                    text = stringResource(R.string.core_cancel),
                    onClick = onPositiveClick
                )
                DefaultTextButton(
                    text = stringResource(R.string.core_continue),
                    onClick = onNegativeClick
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActionDialogPreview() {
    ActionDialog(
        title = "Leaving the app",
        message = "You are now leaving the app and opening a browser.",
        onPositiveClick = {},
        onNegativeClick = {},
    )
}
