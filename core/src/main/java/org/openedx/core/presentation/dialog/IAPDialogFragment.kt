package org.openedx.core.presentation.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.R
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.extension.parcelable
import org.openedx.core.extension.serializable
import org.openedx.core.extension.setFullScreen
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPFlow
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.presentation.iap.IAPViewModel
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IAPErrorDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.UnlockingAccessView
import org.openedx.core.ui.ValuePropUpgradeFeatures
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors

class IAPDialogFragment : DialogFragment() {

    private val iapViewModel by viewModel<IAPViewModel> {
        parametersOf(
            requireArguments().serializable<IAPFlow>(ARG_IAP_FLOW),
            requireArguments().parcelable<PurchaseFlowData>(ARG_PURCHASE_FLOW_DATA)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val iapState by iapViewModel.uiState.collectAsState()
                val uiMessage by iapViewModel.uiMessage.collectAsState(null)
                val scaffoldState = rememberScaffoldState()

                val isFullScreenLoader =
                    (iapState as? IAPUIState.Loading)?.loaderType == IAPLoaderType.FULL_SCREEN

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = MaterialTheme.appColors.background,
                    topBar = {
                        if (isFullScreenLoader.not()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    modifier = Modifier.clickable { onDismiss() },
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    bottomBar = {
                        if (isFullScreenLoader.not()) {
                            Box(modifier = Modifier.padding(all = 16.dp)) {
                                when {
                                    (iapState is IAPUIState.Loading ||
                                            iapState is IAPUIState.PurchaseProduct ||
                                            iapState is IAPUIState.Error) -> {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                        }
                                    }

                                    iapState is IAPUIState.ProductData &&
                                            iapViewModel.purchaseData.formattedPrice.isNullOrEmpty()
                                                .not() -> {
                                        OpenEdXButton(modifier = Modifier.fillMaxWidth(),
                                            text = stringResource(
                                                id = R.string.iap_upgrade_price,
                                                iapViewModel.purchaseData.formattedPrice!!,
                                            ),
                                            onClick = {
                                                iapViewModel.startPurchaseFlow()
                                            })
                                    }
                                }
                            }
                        }
                    }
                ) { contentPadding ->

                    HandleUIMessage(
                        uiMessage = uiMessage,
                        scaffoldState = scaffoldState,
                        onDisplayed = {
                            if (iapState is IAPUIState.CourseDataUpdated) {
                                onDismiss()
                            }
                        }
                    )

                    when (iapState) {
                        is IAPUIState.PurchaseProduct -> {
                            iapViewModel.purchaseItem(requireActivity())
                        }

                        is IAPUIState.Error -> {
                            val iapException = (iapState as IAPUIState.Error).iapException
                            IAPErrorDialog(iapException = iapException, onIAPAction = { iapAction ->
                                when (iapAction) {
                                    IAPAction.ACTION_RELOAD_PRICE -> {
                                        iapViewModel.logIAPErrorActionEvent(
                                            iapException.requestType.request,
                                            IAPAction.ACTION_RELOAD_PRICE.action
                                        )
                                        iapViewModel.loadPrice()
                                    }

                                    IAPAction.ACTION_CLOSE -> {
                                        iapViewModel.logIAPErrorActionEvent(
                                            iapException.requestType.request,
                                            IAPAction.ACTION_CLOSE.action
                                        )
                                        onDismiss()
                                    }

                                    IAPAction.ACTION_OK -> {
                                        iapViewModel.logIAPErrorActionEvent(
                                            iapException.requestType.request,
                                            IAPAction.ACTION_OK.action
                                        )
                                        onDismiss()
                                    }

                                    IAPAction.ACTION_REFRESH -> {
                                        iapViewModel.logIAPErrorActionEvent(
                                            iapException.requestType.request,
                                            IAPAction.ACTION_REFRESH.action
                                        )
                                        iapViewModel.refreshCourse()
                                    }

                                    IAPAction.ACTION_GET_HELP -> {
                                        iapViewModel.showFeedbackScreen(
                                            requireActivity(),
                                            iapException.requestType.request,
                                            iapException.getFormattedErrorMessage()
                                        )
                                        onDismiss()
                                    }

                                    IAPAction.ACTION_RETRY -> {
                                        iapViewModel.logIAPErrorActionEvent(
                                            iapException.requestType.request,
                                            IAPAction.ACTION_RETRY.action
                                        )
                                        if (iapException.requestType == IAPRequestType.CONSUME_CODE) {
                                            iapViewModel.retryToConsumeOrder()
                                        } else if (iapException.requestType == IAPRequestType.EXECUTE_ORDER_CODE) {
                                            iapViewModel.retryExecuteOrder()
                                        }
                                    }

                                    else -> {
                                        // ignore
                                    }
                                }
                            })
                        }

                        is IAPUIState.Clear -> {
                            onDismiss()
                        }

                        else -> {}
                    }

                    if (isFullScreenLoader) {
                        UnlockingAccessView()
                    } else if (TextUtils.isEmpty(iapViewModel.purchaseData.courseName).not()) {
                        ValuePropUpgradeFeatures(
                            Modifier.padding(contentPadding),
                            iapViewModel.purchaseData.courseName!!
                        )
                    }
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
        const val TAG = "IAPDialogFragment"

        private const val ARG_IAP_FLOW = "iap_flow"
        private const val ARG_PURCHASE_FLOW_DATA = "purchase_flow_data"

        fun newInstance(
            iapFlow: IAPFlow,
            screenName: String = "",
            courseId: String = "",
            courseName: String = "",
            isSelfPaced: Boolean = false,
            componentId: String? = null,
            productInfo: ProductInfo? = null
        ): IAPDialogFragment {
            val fragment = IAPDialogFragment()
            val purchaseFlowData = PurchaseFlowData().apply {
                this.screenName = screenName
                this.courseId = courseId
                this.courseName = courseName
                this.isSelfPaced = isSelfPaced
                this.componentId = componentId
                this.productInfo = productInfo
            }

            fragment.arguments = bundleOf(
                ARG_IAP_FLOW to iapFlow,
                ARG_PURCHASE_FLOW_DATA to purchaseFlowData
            )
            return fragment
        }
    }
}
