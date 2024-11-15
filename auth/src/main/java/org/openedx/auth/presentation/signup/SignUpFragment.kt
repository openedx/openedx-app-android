package org.openedx.auth.presentation.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.signup.compose.SignUpView
import org.openedx.core.AppUpdateState
import org.openedx.core.presentation.global.appupgrade.AppUpgradeRequiredScreen
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.foundation.presentation.rememberWindowSize

class SignUpFragment : Fragment() {

    private val viewModel by viewModel<SignUpViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_INFO_TYPE, "")
        )
    }
    private val router by inject<AuthRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getRegistrationFields()
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

                if (uiState.appUpgradeEvent == null) {
                    SignUpView(
                        windowSize = windowSize,
                        uiState = uiState,
                        uiMessage = uiMessage,
                        onBackClick = {
                            requireActivity().supportFragmentManager.popBackStackImmediate()
                        },
                        onRegisterClick = { authType ->
                            when (authType) {
                                AuthType.PASSWORD -> viewModel.register()
                                AuthType.GOOGLE,
                                AuthType.FACEBOOK,
                                AuthType.MICROSOFT -> viewModel.socialAuth(
                                    this@SignUpFragment,
                                    authType
                                )
                            }
                        },
                        onFieldUpdated = { key, value ->
                            viewModel.updateField(key, value)
                        },
                        onHyperLinkClick = { links, link ->
                            viewModel.openLink(parentFragmentManager, links, link)
                        }
                    )

                    LaunchedEffect(uiState.successLogin) {
                        if (uiState.successLogin) {
                            router.clearBackStack(requireActivity().supportFragmentManager)
                            router.navigateToMain(
                                parentFragmentManager,
                                viewModel.courseId,
                                viewModel.infoType
                            )
                        }
                    }
                } else {
                    AppUpgradeRequiredScreen(
                        onUpdateClick = {
                            AppUpdateState.openPlayMarket(requireContext())
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_INFO_TYPE = "info_type"
        fun newInstance(courseId: String?, infoType: String?): SignUpFragment {
            val fragment = SignUpFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_INFO_TYPE to infoType
            )
            return fragment
        }
    }
}
