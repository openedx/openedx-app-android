package org.openedx.core.presentation.global.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import org.koin.android.ext.android.inject
import org.openedx.core.config.Config
import org.openedx.core.ui.SSOWebContentScreen
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme

class SSOWebContentFragment : Fragment() {

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
                SSOWebContentScreen(
                    windowSize = windowSize,
                    url = config.getSSOURL(),
                    uriScheme = requireArguments().getString(ARG_TITLE, ""),
                    title = "",
                    onBackClick = {
                        // use it to close the webView
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onWebPageLoaded = {
                    },
                    onWebPageUpdated = {
                        val token = it
                        if (token.isNotEmpty()){
                            setFragmentResult("requestKey", bundleOf("bundleKey" to token))
                            requireActivity().supportFragmentManager.popBackStack()
                        }

                    })
            }
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        CookieManager.getInstance().flush()
//    }

    companion object {
        private const val ARG_TITLE = "argTitle"
        private const val ARG_URL = "argUrl"

        fun newInstance(title: String, url: String): SSOWebContentFragment {
            val fragment = SSOWebContentFragment()
            fragment.arguments = bundleOf(
                ARG_TITLE to title,
                ARG_URL to url,
            )
            return fragment
        }
    }
}
