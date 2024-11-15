package org.openedx.core.presentation.dialog.appreview

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.openedx.core.R
import org.openedx.core.ui.theme.OpenEdXTheme

class ThankYouDialogFragment : BaseAppReviewDialogFragment() {

    private val reviewManager: ReviewManager by inject()

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
                val isFeedbackPositive = rememberSaveable {
                    mutableStateOf(requireArguments().getBoolean(ARG_IS_FEEDBACK_POSITIVE))
                }
                val description = if (isFeedbackPositive.value) {
                    stringResource(id = R.string.core_thank_you_dialog_positive_description)
                } else {
                    stringResource(id = R.string.core_thank_you_dialog_negative_description)
                }

                ThankYouDialog(
                    description = description,
                    showButtons = isFeedbackPositive.value,
                    onNotNowClick = {
                        this@ThankYouDialogFragment.notNowClick()
                    },
                    onRateUsClick = this@ThankYouDialogFragment::openInAppReview
                )

                closeDialogDelay(isFeedbackPositive.value)
            }
        }
    }

    private fun closeDialogDelay(isFeedbackPositive: Boolean) {
        if (!isFeedbackPositive) {
            lifecycleScope.launch {
                delay(timeMillis = 3000)
                dismiss()
            }
        }
    }

    private fun openInAppReview() {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            try {
                if (request.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = reviewManager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        onPositiveRate()
                    }
                    onRateAppClick()
                }
            } catch (e: ReviewException) {
                e.printStackTrace()
            }
        }
    }

    override fun dismiss() {
        onDismiss()
    }

    companion object {

        private const val ARG_IS_FEEDBACK_POSITIVE = "is_feedback_positive"

        fun newInstance(
            isFeedbackPositive: Boolean
        ): ThankYouDialogFragment {
            val fragment = ThankYouDialogFragment()
            fragment.arguments = bundleOf(
                ARG_IS_FEEDBACK_POSITIVE to isFeedbackPositive
            )
            return fragment
        }
    }
}
