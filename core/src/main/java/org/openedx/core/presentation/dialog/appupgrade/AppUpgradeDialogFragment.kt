package org.openedx.core.presentation.dialog.appupgrade

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import org.openedx.core.AppUpdateState
import org.openedx.core.presentation.global.appupgrade.AppUpgradeRecommendDialog
import org.openedx.core.ui.theme.OpenEdXTheme

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
        AppUpdateState.wasUpdateDialogClosed.value = true
        dismiss()
    }

    private fun onUpdateClick() {
        AppUpdateState.wasUpdateDialogClosed.value = true
        dismiss()
        AppUpdateState.openPlayMarket(requireContext())
    }

    companion object {
        fun newInstance(): AppUpgradeDialogFragment {
            return AppUpgradeDialogFragment()
        }
    }
}
