package org.openedx.core.presentation.dialog.app_upgrade

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.R
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

class AppUpgradeDialogFragment : DialogFragment() {

    private val preferencesManager by inject<CorePreferences>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        if (dialog != null && dialog!!.window != null) {
            dialog!!.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                Surface(
                    modifier = Modifier,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .noRippleClickable {
                                onNotNowClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .clip(MaterialTheme.appShapes.cardShape)
                                .noRippleClickable {}
                                .background(
                                    color = MaterialTheme.appColors.background,
                                    shape = MaterialTheme.appShapes.cardShape
                                )
                        ) {
                            AppUpgradeDialogContent(
                                modifier = Modifier.padding(32.dp),
                                onNotNowClick = this@AppUpgradeDialogFragment::onNotNowClick,
                                onUpdateClick = this@AppUpgradeDialogFragment::onUpdateClick
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onNotNowClick() {
        preferencesManager.wasUpdateDialogDisplayed = true
        dismiss()
    }

    private fun onUpdateClick() {
        preferencesManager.wasUpdateDialogDisplayed = true
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")))
        }
        dismiss()
    }

    companion object {
        fun newInstance(): AppUpgradeDialogFragment {
            return AppUpgradeDialogFragment()
        }
    }

}

@Composable
fun AppUpgradeDialogContent(
    modifier: Modifier = Modifier,
    onNotNowClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.core_icon_upgrade),
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.core_app_upgrade_title),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.core_app_upgrade_description),
            color = MaterialTheme.appColors.textPrimary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.appTypography.bodyMedium
        )
        AppUpgradeDialogButtons(
            onNotNowClick = onNotNowClick,
            onUpdateClick = onUpdateClick
        )
    }
}

@Composable
fun AppUpgradeDialogButtons(
    onNotNowClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Button(
            modifier = Modifier
                .height(42.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent
            ),
            elevation = null,
            shape = MaterialTheme.appShapes.navigationButtonShape,
            onClick = onNotNowClick
        ) {
            Text(
                color = MaterialTheme.appColors.textAccent,
                style = MaterialTheme.appTypography.labelLarge,
                text = stringResource(id = R.string.core_not_now)
            )
        }
        Button(
            modifier = Modifier
                .height(42.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.appColors.buttonBackground
            ),
            elevation = null,
            shape = MaterialTheme.appShapes.navigationButtonShape,
            onClick = onUpdateClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.core_update),
                    color = MaterialTheme.appColors.buttonText,
                    style = MaterialTheme.appTypography.labelLarge
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AppUpgradeDialogButtonsPreview() {
    AppUpgradeDialogButtons(
        onNotNowClick = { },
        onUpdateClick = { }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AppUpgradeDialogContentPreview() {
    AppUpgradeDialogContent(
        onNotNowClick = { },
        onUpdateClick = { }
    )
}