package org.openedx.auth.presentation.learningsites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.foundation.presentation.rememberWindowSize

class LearningSitesFragment : Fragment() {

    private val viewModel: LearningSitesViewModel by viewModel {
        parametersOf(requireArguments().getBoolean(ARG_IS_SWITCHING))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val state by viewModel.uiState.collectAsState()
                LearningSitesScreen(
                    windowSize = rememberWindowSize(),
                    state = state,
                    isSwitching = viewModel.isSwitching,
                    onBackClick = { parentFragmentManager.popBackStack() },
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onSiteClick = { viewModel.select(parentFragmentManager, it) },
                    onLogOutClick = { viewModel.logOut(parentFragmentManager, it) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Back from a site's sign-in, or from signing in elsewhere, the lists may be stale.
        viewModel.refresh()
    }

    companion object {
        private const val ARG_IS_SWITCHING = "is_switching"

        fun newInstance(isSwitching: Boolean) = LearningSitesFragment().apply {
            arguments = bundleOf(ARG_IS_SWITCHING to isSwitching)
        }
    }
}
