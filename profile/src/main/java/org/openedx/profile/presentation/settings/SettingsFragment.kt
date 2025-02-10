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
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.foundation.presentation.rememberWindowSize

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
                val logoutSuccess by viewModel.successLogout.collectAsState(false)
                val appUpgradeEvent by viewModel.appUpgradeEvent.collectAsState(null)

                SettingsScreen(
                    windowSize = windowSize,
                    uiState = uiState,
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
}
