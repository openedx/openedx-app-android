package org.openedx.profile.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.profile.presentation.profile.compose.ProfileView
import org.openedx.profile.presentation.profile.compose.ProfileViewAction

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

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
                val logoutSuccess by viewModel.successLogout.observeAsState(false)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.isUpdating.observeAsState(false)
                val appUpgradeEvent by viewModel.appUpgradeEvent.observeAsState(null)

                ProfileView(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    appUpgradeEvent = appUpgradeEvent,
                    onAction = { action ->
                        when (action) {
                            ProfileViewAction.AppVersionClick -> {
                                viewModel.appVersionClickedEvent(requireContext())
                            }

                            ProfileViewAction.EditAccountClick -> {
                                viewModel.profileEditClicked(
                                    requireParentFragment().parentFragmentManager
                                )
                            }

                            ProfileViewAction.LogoutClick -> {
                                viewModel.logout()
                            }

                            ProfileViewAction.PrivacyPolicyClick -> {
                                viewModel.privacyPolicyClicked(
                                    requireParentFragment().parentFragmentManager
                                )
                            }

                            ProfileViewAction.CookiePolicyClick -> {
                                viewModel.cookiePolicyClicked(
                                    requireParentFragment().parentFragmentManager
                                )
                            }

                            ProfileViewAction.DataSellClick -> {
                                viewModel.dataSellClicked(
                                    requireParentFragment().parentFragmentManager
                                )
                            }

                            ProfileViewAction.FaqClick -> viewModel.faqClicked()

                            ProfileViewAction.SupportClick -> {
                                viewModel.emailSupportClicked(requireContext())
                            }

                            ProfileViewAction.TermsClick -> {
                                viewModel.termsOfUseClicked(
                                    requireParentFragment().parentFragmentManager
                                )
                            }

                            ProfileViewAction.VideoSettingsClick -> {
                                viewModel.profileVideoSettingsClicked(
                                    requireParentFragment().parentFragmentManager
                                )
                            }

                            ProfileViewAction.SwipeRefresh -> {
                                viewModel.updateAccount()
                            }
                        }
                    },
                )

                LaunchedEffect(logoutSuccess) {
                    if (logoutSuccess) {
                        viewModel.restartApp(requireParentFragment().parentFragmentManager)
                    }
                }
            }
        }
    }
}
