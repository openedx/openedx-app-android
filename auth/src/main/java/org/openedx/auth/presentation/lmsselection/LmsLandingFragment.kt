package org.openedx.auth.presentation.lmsselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
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
 * LMS Directory landing shown before sign-in when the feature is on and no platform
 * has been chosen yet. Offers browse/search or QR sign-in. Reuses
 * [SiteSelectionViewModel] for the QR path (validate + select + re-theme).
 */
class LmsLandingFragment : Fragment() {

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
                LaunchedEffect(Unit) {
                    viewModel.actions.collect { action ->
                        when (action) {
                            is SiteSelectionViewModel.SiteSelectionAction.Success -> continueAfterSelection()
                        }
                    }
                }
                LmsLandingScreen(
                    onFindClick = { router.navigateToLmsSelection(requireActivity().supportFragmentManager) },
                    onQrClick = { launchQrScanner() },
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
        scanLauncher.launch(options)
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
