package org.openedx.core.presentation.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.domain.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.extension.parcelable
import org.openedx.core.extension.serializable
import org.openedx.core.extension.setFullScreen
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.presentation.iap.IAPViewModel
import org.openedx.core.ui.UnlockingAccessView
import org.openedx.core.ui.UpgradeErrorDialog
import org.openedx.core.ui.ValuePropModal
import org.openedx.core.ui.theme.OpenEdXTheme

class IAPDialogFragment : DialogFragment() {

    private val iapViewModel by viewModel<IAPViewModel> {
        parametersOf(
            requireArguments().serializable<IAPAction>(ARG_IAP_ACTION),
            PurchaseFlowData(
                screenName = requireArguments().getString(ARG_SCREEN_NAME, ""),
                courseId = requireArguments().getString(ARG_COURSE_ID, ""),
                courseName = requireArguments().getString(ARG_COURSE_NAME, ""),
                isSelfPaced = requireArguments().getBoolean(ARG_SELF_PACES, false),
                componentId = requireArguments().getString(ARG_COMPONENT_ID, ""),
                productInfo = requireArguments().parcelable<ProductInfo>(ARG_PRODUCT_INFO)
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val iapState by iapViewModel.uiState.collectAsState()

                ValuePropModal(
                    courseTitle = iapViewModel.purchaseFlowData.courseName!!,
                    isLoading = true,
                    onDismiss = { onDismiss() },
                )
                when {
                    (iapState is IAPUIState.ProductData && TextUtils.isEmpty((iapState as IAPUIState.ProductData).formattedPrice)
                        .not()) -> {
                        ValuePropModal(
                            courseTitle = iapViewModel.purchaseFlowData.courseName!!,
                            formattedPrice = iapViewModel.purchaseFlowData.formattedPrice,
                            onDismiss = { onDismiss() },
                            onUpgradeNow = {
                                iapViewModel.startPurchaseFlow()
                            }
                        )
                    }

                    iapState is IAPUIState.PurchaseProduct -> {
                        iapViewModel.purchaseItem(requireActivity())
                    }

                    iapState is IAPUIState.Loading && (iapState as IAPUIState.Loading).loaderType == IAPLoaderType.FULL_SCREEN -> {
                        UnlockingAccessView()
                    }

                    iapState is IAPUIState.CourseDataUpdated -> {
                        onDismiss()
                    }

                    iapState is IAPUIState.Error -> {
                        UpgradeErrorDialog(onDismiss = {
                            onDismiss()
                        }, onGetHelp = {
                            iapViewModel.showFeedbackScreen(
                                requireActivity(),
                                (iapState as IAPUIState.Error).feedbackErrorMessage
                            )
                            onDismiss()
                        })
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setFullScreen(100)
    }

    private fun onDismiss() {
        iapViewModel.clearIAPFLow()
        dismiss()
    }

    companion object {
        private const val ARG_IAP_ACTION = "iap_action"
        private const val ARG_SCREEN_NAME = "SCREEN_NAME"
        private const val ARG_COURSE_ID = "course_id"
        private const val ARG_COURSE_NAME = "course_name"
        private const val ARG_SELF_PACES = "self_paces"
        private const val ARG_COMPONENT_ID = "component_id"
        private const val ARG_PRODUCT_INFO = "product_info"

        fun newInstance(
            iapAction: IAPAction,
            screenName: String,
            courseId: String,
            courseName: String,
            isSelfPaced: Boolean,
            componentId: String? = null,
            productInfo: ProductInfo
        ): IAPDialogFragment {
            val fragment = IAPDialogFragment()
            fragment.arguments = bundleOf(
                ARG_IAP_ACTION to iapAction,
                ARG_SCREEN_NAME to screenName,
                ARG_COURSE_ID to courseId,
                ARG_COURSE_NAME to courseName,
                ARG_SELF_PACES to isSelfPaced,
                ARG_COMPONENT_ID to componentId,
                ARG_PRODUCT_INFO to productInfo
            )
            return fragment
        }
    }
}
