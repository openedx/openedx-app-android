package org.openedx.core.presentation.global.appupgrade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import org.openedx.core.AppUpdateState
import org.openedx.core.ui.theme.OpenEdXTheme

class UpgradeRequiredFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                AppUpgradeRequiredScreen(
                    showAccountSettingsButton = true,
                    onAccountSettingsClick = {
                        setFragmentResult(REQUEST_KEY, bundleOf(OPEN_ACCOUNT_SETTINGS_KEY to ""))
                        parentFragmentManager.popBackStack()
                    },
                    onUpdateClick = {
                        AppUpdateState.openPlayMarket(requireContext())
                    }
                )
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "UpgradeRequiredFragmentRequestKey"
        const val OPEN_ACCOUNT_SETTINGS_KEY = "openAccountSettings"
    }
}
