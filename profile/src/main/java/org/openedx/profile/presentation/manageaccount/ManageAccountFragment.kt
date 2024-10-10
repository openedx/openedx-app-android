package org.openedx.profile.presentation.manageaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.profile.presentation.manageaccount.compose.ManageAccountView
import org.openedx.profile.presentation.manageaccount.compose.ManageAccountViewAction

class ManageAccountFragment : Fragment() {

    private val viewModel: ManageAccountViewModel by viewModel()

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
                val uiMessage by viewModel.uiMessage.collectAsState(null)
                val refreshing by viewModel.isUpdating.collectAsState(false)

                ManageAccountView(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    onAction = { action ->
                        when (action) {
                            ManageAccountViewAction.EditAccountClick -> {
                                viewModel.profileEditClicked(
                                    requireActivity().supportFragmentManager
                                )
                            }
                            ManageAccountViewAction.SwipeRefresh -> {
                                viewModel.updateAccount()
                            }
                            ManageAccountViewAction.BackClick -> {
                                requireActivity().supportFragmentManager.popBackStack()
                            }
                            ManageAccountViewAction.DeleteAccount -> {
                                viewModel.profileDeleteAccountClickedEvent()
                                viewModel.profileRouter.navigateToDeleteAccount(
                                    requireActivity().supportFragmentManager
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
