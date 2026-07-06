package org.openedx.auth.presentation.lmsselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.auth.presentation.AuthRouter
import org.openedx.core.config.Config
import org.openedx.core.ui.theme.OpenEdXTheme

/**
 * "Find my LMS" — browse or search the registry catalog (or type a URL). Picking a
 * platform re-themes the app to it and continues to the normal sign-in flow.
 */
class SiteSelectionFragment : Fragment() {

    private val viewModel: SiteSelectionViewModel by viewModel()
    private val router: AuthRouter by inject()
    private val config: Config by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.actions.collect { action ->
                        when (action) {
                            is SiteSelectionViewModel.SiteSelectionAction.Success -> continueAfterSelection()
                        }
                    }
                }

                SiteSelectionScreen(
                    state = state,
                    callbacks = SiteSelectionCallbacks(
                        onBack = { requireActivity().supportFragmentManager.popBackStack() },
                        onInputChanged = viewModel::onInputChanged,
                        onSubmitManual = viewModel::onSubmitManual,
                        onQueryChanged = viewModel::onQueryChanged,
                        onCatalogItemSelected = viewModel::onCatalogItemSelected,
                    )
                )
            }
        }
    }

    private fun continueAfterSelection() {
        val fm = requireActivity().supportFragmentManager
        if (config.isPreLoginExperienceEnabled()) {
            router.navigateToLogistration(fm, courseId = null)
        } else {
            router.navigateToSignIn(fm, courseId = null, infoType = null)
        }
    }
}
