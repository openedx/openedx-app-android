package org.openedx.core.presentation.dialog.appreview

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.openedx.core.ui.theme.OpenEdXTheme

class RateDialogFragment: BaseAppReviewDialogFragment() {
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
                val rating = rememberSaveable { mutableIntStateOf(0) }
                RateDialog(
                    rating = rating,
                    onNotNowClick = this@RateDialogFragment::notNowClick,
                    onSubmitClick = {
                        onSubmitClick(rating.intValue)
                    }
                )
            }
        }
    }

    private fun onSubmitClick(rating: Int) {
        dismiss()
        if (rating > 3) {
            openThankYouDialog()
        } else {
            openFeedbackDialog()
        }
    }

    private fun openFeedbackDialog() {
        val dialog = FeedbackDialogFragment.newInstance()
        dialog.show(
            requireActivity().supportFragmentManager,
            FeedbackDialogFragment::class.simpleName
        )
    }

    private fun openThankYouDialog() {
        val dialog = ThankYouDialogFragment.newInstance(
            isFeedbackPositive = true
        )
        dialog.show(
            requireActivity().supportFragmentManager,
            ThankYouDialogFragment::class.simpleName
        )
    }

    companion object {
        fun newInstance(): RateDialogFragment {
            return RateDialogFragment()
        }
    }
}