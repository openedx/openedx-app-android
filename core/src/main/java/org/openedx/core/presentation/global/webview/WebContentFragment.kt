package org.openedx.core.presentation.global.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.openedx.core.config.Config
import org.openedx.core.ui.WebContentScreen
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.foundation.presentation.rememberWindowSize

class WebContentFragment : Fragment() {

    private val config: Config by inject()

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
                    apiHostUrl = config.getApiHostURL(),
                    windowSize = windowSize,
                    title = requireArguments().getString(ARG_TITLE, ""),
                    contentUrl = requireArguments().getString(ARG_URL, ""),
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().flush()
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
