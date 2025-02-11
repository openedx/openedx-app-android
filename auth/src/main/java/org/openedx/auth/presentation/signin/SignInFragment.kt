package org.openedx.auth.presentation.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.presentation.signin.compose.LoginScreen
import org.openedx.core.AppUpdateState
import org.openedx.core.presentation.global.appupgrade.AppUpgradeRequiredScreen
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.foundation.presentation.rememberWindowSize

class SignInFragment : Fragment() {

    private val viewModel: SignInViewModel by viewModel {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_INFO_TYPE, ""),
            requireArguments().getString(ARG_AUTH_CODE, ""),
        )
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
                val state by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()
                val appUpgradeEvent by viewModel.appUpgradeEvent.observeAsState(null)

                if (appUpgradeEvent == null) {
                    if (viewModel.authCode != "" && !state.loginFailure && !state.loginSuccess) {
                        viewModel.signInAuthCode(viewModel.authCode)
                    }
                    LoginScreen(
                        windowSize = windowSize,
                        state = state,
                        uiMessage = uiMessage,
                        onEvent = { event ->
                            when (event) {
                                is AuthEvent.SignIn -> viewModel.login(event.login, event.password)
                                is AuthEvent.SocialSignIn -> viewModel.socialAuth(
                                    this@SignInFragment,
                                    event.authType
                                )

                                AuthEvent.ForgotPasswordClick -> {
                                    viewModel.navigateToForgotPassword(parentFragmentManager)
                                }

                                AuthEvent.SignInBrowser -> {
                                    viewModel.signInBrowser(requireActivity())
                                }

                                AuthEvent.RegisterClick -> {
                                    viewModel.navigateToSignUp(parentFragmentManager)
                                }

                                AuthEvent.BackClick -> {
                                    requireActivity().supportFragmentManager.popBackStackImmediate()
                                }

                                is AuthEvent.OpenLink -> viewModel.openLink(
                                    parentFragmentManager,
                                    event.links,
                                    event.link
                                )
                            }
                        },
                    )
                    LaunchedEffect(state.loginSuccess) {
                        viewModel.proceedWhatsNew(parentFragmentManager)
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
        private const val ARG_AUTH_CODE = "auth_code"
        fun newInstance(courseId: String?, infoType: String?, authCode: String? = null): SignInFragment {
            val fragment = SignInFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_INFO_TYPE to infoType,
                ARG_AUTH_CODE to authCode,
            )
            return fragment
        }
    }
}

internal sealed interface AuthEvent {
    data class SignIn(val login: String, val password: String) : AuthEvent
    data class SocialSignIn(val authType: AuthType) : AuthEvent
    data class OpenLink(val links: Map<String, String>, val link: String) : AuthEvent
    object SignInBrowser : AuthEvent
    object RegisterClick : AuthEvent
    object ForgotPasswordClick : AuthEvent
    object BackClick : AuthEvent
}
