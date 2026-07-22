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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.auth.R
import org.openedx.auth.presentation.AuthRouter
import org.openedx.core.config.Config
import org.openedx.core.ui.theme.OpenEdXTheme

/**
 * "Find my LMS" — browse or search the registry catalog (or type a URL). Picking a
 * platform re-themes the app to it and continues to the normal sign-in flow. The
 * search field's QR button opens the camera scanner directly, same as the landing.
 */
class SiteSelectionFragment : Fragment() {

    private val viewModel: SiteSelectionViewModel by viewModel()
    private val router: AuthRouter by inject()
    private val config: Config by inject()

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.onUrlScanned(it) }
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

                LaunchedEffect(Unit) {
                    viewModel.actions.collect { action ->
                        when (action) {
                            is SiteSelectionViewModel.SiteSelectionAction.Success ->
                                continueAfterSelection(action.preLoginDiscovery)
                        }
                    }
                }

                // The QR button opens the camera scanner directly (no instructions screen).
                SiteSelectionScreen(
                    state = state,
                    callbacks = SiteSelectionCallbacks(
                        onBack = { requireActivity().supportFragmentManager.popBackStack() },
                        onQrClick = { launchQrScanner() },
                        onSubmitManual = viewModel::onSubmitManual,
                        onQueryChanged = viewModel::onQueryChanged,
                        onCatalogItemSelected = viewModel::onCatalogItemSelected,
                        onCleanHistory = viewModel::onCleanHistory,
                        onHistoryItemSelected = viewModel::onHistoryItemSelected,
                    )
                )
            }
        }
    }

    private fun launchQrScanner() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(getString(R.string.auth_lms_qr_prompt))
            .setBeepEnabled(false)
            .setOrientationLocked(false)
            .setCaptureActivity(LmsQrScannerActivity::class.java)
        scanLauncher.launch(options)
    }

    private fun continueAfterSelection(preLoginDiscovery: Boolean) {
        val fm = requireActivity().supportFragmentManager
        when {
            // The selected LMS is configured to start on the course Discovery screen —
            // open it (native or webview per config) instead of sign-in, matching iOS.
            preLoginDiscovery -> if (config.getDiscoveryConfig().isViewTypeWebView()) {
                router.navigateToWebDiscoverCourses(fm, querySearch = "")
            } else {
                router.navigateToNativeDiscoverCourses(fm, querySearch = "")
            }

            config.isPreLoginExperienceEnabled() -> router.navigateToLogistration(fm, courseId = null)
            else -> router.navigateToSignIn(fm, courseId = null, infoType = null)
        }
    }
}
