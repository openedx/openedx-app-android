package org.openedx.profile.presentation.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.koin.androidx.compose.koinViewModel
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.rememberWindowSize

class CalendarFragment : Fragment() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        if (!isGranted.containsValue(false)) {
            val dialog = NewCalendarDialogFragment.newInstance(NewCalendarDialogType.CREATE_NEW)
            dialog.show(
                requireActivity().supportFragmentManager,
                NewCalendarDialogFragment.DIALOG_TAG
            )
        } else {
            val dialog = CalendarAccessDialogFragment.newInstance()
            dialog.show(
                requireActivity().supportFragmentManager,
                CalendarAccessDialogFragment.DIALOG_TAG
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val viewModel: CalendarViewModel = koinViewModel()
                val uiState by viewModel.uiState.collectAsState()

                CalendarView(
                    windowSize = windowSize,
                    uiState = uiState,
                    setUpCalendarSync = {
                        viewModel.setUpCalendarSync(permissionLauncher)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onCalendarSyncSwitchClick = {
                        viewModel.setCalendarSyncEnabled(it, requireActivity().supportFragmentManager)
                    },
                    onRelativeDateSwitchClick = {
                        viewModel.setRelativeDateEnabled(it)
                    },
                    onChangeSyncOptionClick = {
                        val dialog = NewCalendarDialogFragment.newInstance(NewCalendarDialogType.UPDATE)
                        dialog.show(
                            requireActivity().supportFragmentManager,
                            NewCalendarDialogFragment.DIALOG_TAG
                        )
                    },
                    onCourseToSyncClick = {
                        viewModel.navigateToCoursesToSync(requireActivity().supportFragmentManager)
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarView(
    windowSize: WindowSize,
    uiState: CalendarUIState,
    setUpCalendarSync: () -> Unit,
    onBackClick: () -> Unit,
    onChangeSyncOptionClick: () -> Unit,
    onCourseToSyncClick: () -> Unit,
    onCalendarSyncSwitchClick: (Boolean) -> Unit,
    onRelativeDateSwitchClick: (Boolean) -> Unit
) {
    if (!uiState.isCalendarExist) {
        CalendarSetUpView(
            windowSize = windowSize,
            useRelativeDates = uiState.isRelativeDateEnabled,
            setUpCalendarSync = setUpCalendarSync,
            onRelativeDateSwitchClick = onRelativeDateSwitchClick,
            onBackClick = onBackClick
        )
    } else {
        CalendarSettingsView(
            windowSize = windowSize,
            uiState = uiState,
            onBackClick = onBackClick,
            onCalendarSyncSwitchClick = onCalendarSyncSwitchClick,
            onRelativeDateSwitchClick = onRelativeDateSwitchClick,
            onChangeSyncOptionClick = onChangeSyncOptionClick,
            onCourseToSyncClick = onCourseToSyncClick
        )
    }
}
