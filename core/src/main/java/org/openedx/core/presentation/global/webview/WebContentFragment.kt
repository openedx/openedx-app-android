package org.openedx.core.presentation.global.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.openedx.core.ui.WebContentScreen
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme

class WebContentFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                WebContentScreen(
                    windowSize = windowSize,
                    title = requireArguments().getString(ARG_TITLE, ""),
                    contentUrl = requireArguments().getString(ARG_URL, ""),
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    })
            }
        }
    }

    companion object {
        private const val ARG_TITLE = "argTitle"
        private const val ARG_URL = "argUrl"

        fun newInstance(title: String, url: String): WebContentFragment {
            val fragment = WebContentFragment()
            fragment.arguments = bundleOf(
                ARG_TITLE to title,
                ARG_URL to url,
            )
            return fragment
        }
    }
}
