package org.openedx.core.presentation.dialog.app_review

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.openedx.core.presentation.global.AppDataHolder
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.utils.EmailUtil

class FeedbackDialogFragment : BaseAppReviewDialogFragment() {

    private var wasShareClicked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        if (dialog != null && dialog!!.window != null) {
            dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val feedback = rememberSaveable { mutableStateOf("") }
                FeedbackDialog(
                    feedback = feedback,
                    onNotNowClick = this@FeedbackDialogFragment::notNowClick,
                    onShareClick = {
                        onShareClick(feedback.value)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (wasShareClicked) {
            openThankYouDialog()
            dismiss()
        }
    }

    private fun onShareClick(feedback: String) {
        saveVersionName()
        wasShareClicked = true
        sendEmail(feedback)
    }

    private fun sendEmail(feedback: String) {
        EmailUtil.showFeedbackScreen(
            context = requireContext(),
            subject = feedback,
            appVersion = (requireActivity() as AppDataHolder).appData.versionName
        )
    }

    private fun openThankYouDialog() {
        val dialog = ThankYouDialogFragment.newInstance(
            isFeedbackPositive = false
        )
        dialog.show(
            requireActivity().supportFragmentManager,
            ThankYouDialogFragment::class.simpleName
        )
    }

    companion object {
        fun newInstance(): FeedbackDialogFragment {
            return FeedbackDialogFragment()
        }
    }
}