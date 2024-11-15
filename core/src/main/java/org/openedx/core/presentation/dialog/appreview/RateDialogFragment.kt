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

class RateDialogFragment : BaseAppReviewDialogFragment() {
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
                    onNotNowClick = { notNowClick(rating.intValue) },
                    onSubmitClick = { onSubmitClick(rating.intValue) }
                )
            }
        }
        onRatingDialogShowed()
    }

    private fun onSubmitClick(rating: Int) {
        onSubmitRatingClick(rating)
        if (rating > MIN_RATE) {
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

    override fun dismiss() {
        onDismiss()
    }

    companion object {
        private const val MIN_RATE = 3

        fun newInstance(): RateDialogFragment {
            return RateDialogFragment()
        }
    }
}
