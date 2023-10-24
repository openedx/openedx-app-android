package org.openedx.core.presentation.global.app_upgrade

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.openedx.core.ui.theme.OpenEdXTheme

class UpgradeRequiredFragment : Fragment() {

    private val router: AppUpgradeRouter by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                AppUpgradeRequiredScreen(
                    onAccountSettingsClick = {
                        router.navigateToUserProfile(parentFragmentManager)
                    },
                    onUpdateClick = {
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
                    }
                )
            }
        }
    }
}