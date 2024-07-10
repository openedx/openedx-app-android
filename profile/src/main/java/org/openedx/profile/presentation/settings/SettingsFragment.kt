package org.openedx.profile.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.presentation.IAPAnalyticsEvent
import org.openedx.core.presentation.IAPAnalyticsKeys
import org.openedx.core.presentation.IAPAnalyticsScreen
import org.openedx.core.presentation.dialog.IAPDialogFragment
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPFlow
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme

class SettingsFragment : Fragment() {

    private val viewModel by viewModel<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val uiState by viewModel.uiState.collectAsState()
                val iapUiState by viewModel.iapUiState.collectAsState()
                val logoutSuccess by viewModel.successLogout.collectAsState(false)
                val appUpgradeEvent by viewModel.appUpgradeEvent.collectAsState(null)

                SettingsScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    iapUiState = iapUiState,
                    appUpgradeEvent = appUpgradeEvent,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onAction = { action ->
                        when (action) {
                            SettingsScreenAction.AppVersionClick -> {
                                viewModel.appVersionClickedEvent(requireContext())
                            }

                            SettingsScreenAction.LogoutClick -> {
                                viewModel.logout()
                            }

                            SettingsScreenAction.PrivacyPolicyClick -> {
                                viewModel.privacyPolicyClicked(
                                    requireActivity().supportFragmentManager
                                )
                            }

                            SettingsScreenAction.CookiePolicyClick -> {
                                viewModel.cookiePolicyClicked(
                                    requireActivity().supportFragmentManager
                                )
                            }

                            SettingsScreenAction.DataSellClick -> {
                                viewModel.dataSellClicked(
                                    requireActivity().supportFragmentManager
                                )
                            }

                            SettingsScreenAction.FaqClick -> viewModel.faqClicked()

                            SettingsScreenAction.SupportClick -> {
                                viewModel.emailSupportClicked(requireContext())
                            }

                            SettingsScreenAction.TermsClick -> {
                                viewModel.termsOfUseClicked(
                                    requireActivity().supportFragmentManager
                                )
                            }

                            SettingsScreenAction.VideoSettingsClick -> {
                                viewModel.videoSettingsClicked(
                                    requireActivity().supportFragmentManager
                                )
                            }

                            SettingsScreenAction.ManageAccountClick -> {
                                viewModel.manageAccountClicked(
                                    requireActivity().supportFragmentManager
                                )
                            }

                            SettingsScreenAction.CalendarSettingsClick -> {
                                viewModel.calendarSettingsClicked(
                                    requireActivity().supportFragmentManager
                                )
                            }

                            SettingsScreenAction.RestorePurchaseClick -> {
                                viewModel.restorePurchase()
                            }
                        }
                    },
                    onIAPAction = { action, iapException ->
                        when (action) {
                            IAPAction.ACTION_ERROR_CLOSE -> {
                                viewModel.logIAPCancelEvent()
                            }

                            IAPAction.ACTION_GET_HELP -> {
                                viewModel.clearIAPState()
                                val errorMessage = iapException?.getFormattedErrorMessage() ?: ""
                                viewModel.showFeedbackScreen(requireActivity(), errorMessage)
                            }

                            IAPAction.ACTION_RESTORE -> {
                                IAPDialogFragment.newInstance(
                                    IAPFlow.RESTORE,
                                    IAPAnalyticsScreen.PROFILE.screenName
                                ).show(
                                    requireActivity().supportFragmentManager,
                                    IAPDialogFragment.TAG
                                )
                            }

                            IAPAction.ACTION_RESTORE_PURCHASE_CANCEL -> {
                                viewModel.logIAPEvent(
                                    IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION,
                                    buildMap {
                                        put(
                                            IAPAnalyticsKeys.ACTION.key,
                                            IAPAction.ACTION_CLOSE.action
                                        )
                                    }.toMutableMap()
                                )
                                viewModel.clearIAPState()
                            }

                            else -> {}
                        }
                    }
                )

                LaunchedEffect(logoutSuccess) {
                    if (logoutSuccess) {
                        viewModel.restartApp(requireActivity().supportFragmentManager)
                    }
                }
            }
        }
    }
}

internal interface SettingsScreenAction {
    object AppVersionClick : SettingsScreenAction
    object LogoutClick : SettingsScreenAction
    object PrivacyPolicyClick : SettingsScreenAction
    object CookiePolicyClick : SettingsScreenAction
    object DataSellClick : SettingsScreenAction
    object FaqClick : SettingsScreenAction
    object TermsClick : SettingsScreenAction
    object SupportClick : SettingsScreenAction
    object VideoSettingsClick : SettingsScreenAction
    object ManageAccountClick : SettingsScreenAction
    object CalendarSettingsClick : SettingsScreenAction
    object RestorePurchaseClick : SettingsScreenAction
}

