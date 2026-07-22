package org.openedx.profile.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.profile.presentation.profile.compose.ProfileView
import org.openedx.profile.presentation.profile.compose.ProfileViewAction
import org.openedx.profile.presentation.reportlms.ReportLmsSheet
import org.openedx.profile.presentation.reportlms.ReportLmsViewModel

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModel()
    private val reportViewModel: ReportLmsViewModel by viewModel()
    private val corePreferences: CorePreferences by inject()

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
                val uiMessage by viewModel.uiMessage.collectAsState(initial = null)
                val refreshing by viewModel.isUpdating.observeAsState(false)
                var showReportSheet by remember { mutableStateOf(false) }

                ProfileView(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    // Curated/institution registries have no learner reporting.
                    showReportLms = viewModel.isLmsDirectoryEnabled && !corePreferences.lmsDirectoryCurated,
                    onSettingsClick = {
                        viewModel.profileRouter.navigateToSettings(requireActivity().supportFragmentManager)
                    },
                    onAction = { action ->
                        when (action) {
                            ProfileViewAction.EditAccountClick -> {
                                viewModel.profileEditClicked(
                                    requireParentFragment().parentFragmentManager
                                )
                            }
                            ProfileViewAction.ReportLmsClick -> {
                                showReportSheet = true
                            }
                            ProfileViewAction.SwipeRefresh -> {
                                viewModel.updateAccount()
                            }
                        }
                    }
                )

                if (showReportSheet) {
                    ReportLmsSheet(
                        viewModel = reportViewModel,
                        onDismiss = {
                            showReportSheet = false
                            reportViewModel.reset()
                        },
                    )
                }
            }
        }
    }
}
