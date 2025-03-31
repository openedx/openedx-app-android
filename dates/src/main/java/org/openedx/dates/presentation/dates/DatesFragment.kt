package org.openedx.dates.presentation.dates

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

class DatesFragment : Fragment() {

    private val viewModel by viewModel<DatesViewModel>()

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
                val uiState by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.collectAsState(null)
                DatesScreen(
                    uiState = uiState,
                    uiMessage = uiMessage,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    useRelativeDates = viewModel.useRelativeDates,
                    onAction = { action ->
                        when (action) {
                            DatesViewActions.OpenSettings -> {
                                viewModel.onSettingsClick(requireActivity().supportFragmentManager)
                            }

                            DatesViewActions.SwipeRefresh -> {
                                viewModel.refreshData()
                            }

                            DatesViewActions.LoadMore -> {
                                viewModel.fetchMore()
                            }

                            DatesViewActions.ShiftDueDate -> {
                                viewModel.shiftDueDate()
                            }

                            is DatesViewActions.OpenEvent -> {
                                viewModel.navigateToCourseOutline(
                                    requireActivity().supportFragmentManager,
                                    action.date
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val LOAD_MORE_THRESHOLD = 0.8f
    }
}
