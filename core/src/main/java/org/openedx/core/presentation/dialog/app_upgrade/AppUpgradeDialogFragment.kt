package org.openedx.core.presentation.dialog.app_upgrade

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.presentation.global.app_upgrade.AppUpgradeDialogContent
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes

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
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
                )
            )
        }
        dismiss()
    }

    companion object {
        fun newInstance(): AppUpgradeDialogFragment {
            return AppUpgradeDialogFragment()
        }
    }

}
