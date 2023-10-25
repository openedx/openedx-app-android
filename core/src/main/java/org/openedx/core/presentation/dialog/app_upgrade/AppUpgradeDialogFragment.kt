package org.openedx.core.presentation.dialog.app_upgrade

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import org.openedx.core.presentation.global.app_upgrade.AppUpgradeRecommendDialog
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.utils.AppUpdateState

class AppUpgradeDialogFragment : DialogFragment() {

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
                AppUpgradeRecommendDialog(
                    onNotNowClick = this@AppUpgradeDialogFragment::onNotNowClick,
                    onUpdateClick = this@AppUpgradeDialogFragment::onUpdateClick
                )
            }
        }
    }

    private fun onNotNowClick() {
        dismiss()
    }

    private fun onUpdateClick() {
        AppUpdateState.openPlayMarket(requireContext())
        dismiss()
    }

    companion object {
        fun newInstance(): AppUpgradeDialogFragment {
            return AppUpgradeDialogFragment()
        }
    }

}
