package org.openedx.courses.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.openedx.core.ui.theme.OpenEdXTheme

class DashboardGalleryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                DashboardGalleryView(fragmentManager = requireActivity().supportFragmentManager)
            }
        }
    }

    companion object {
        const val TABLET_COURSE_LIST_ITEM_COUNT = 7
        const val MOBILE_COURSE_LIST_ITEM_COUNT = 7
    }
}
