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
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.signin.compose.LoginScreen
import org.openedx.core.AppUpdateState
import org.openedx.core.presentation.global.WhatsNewGlobalManager
import org.openedx.core.presentation.global.app_upgrade.AppUpgradeRequiredScreen
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme

class SignInFragment : Fragment() {

    private val viewModel: SignInViewModel by viewModel()
    private val router: AuthRouter by inject()
    private val whatsNewGlobalManager by inject<WhatsNewGlobalManager>()

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
                    LoginScreen(
                        windowSize = windowSize,
                        state = state,
                        uiMessage = uiMessage,
                        onEvent = { event ->
                            when (event) {
                                is AuthEvent.SignIn -> viewModel.login(event.login, event.password)
                                AuthEvent.SignInGoogle -> viewModel.signInGoogle(requireActivity())
                                AuthEvent.SignInFacebook -> {
                                    viewModel.signInFacebook(this@SignInFragment)
                                }

                                AuthEvent.SignInMicrosoft -> {
                                    viewModel.signInMicrosoft(requireActivity())
                                }

                                AuthEvent.ForgotPasswordClick -> {
                                    viewModel.forgotPasswordClickedEvent()
                                    router.navigateToRestorePassword(parentFragmentManager)
                                }

                                AuthEvent.RegisterClick -> {
                                    viewModel.signUpClickedEvent()
                                    router.navigateToSignUp(parentFragmentManager)
                                }
                                
                                AuthEvent.BackClick -> {
                                    requireActivity().supportFragmentManager.popBackStackImmediate()
                                }
                            }
                        },
                    )
                    LaunchedEffect(state.loginSuccess) {
                        val isNeedToShowWhatsNew =
                            whatsNewGlobalManager.shouldShowWhatsNew()
                        if (state.loginSuccess) {
                            if (isNeedToShowWhatsNew) {
                                router.navigateToWhatsNew(parentFragmentManager)
                            } else {
                                router.navigateToMain(parentFragmentManager)
                            }
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
}

internal sealed interface AuthEvent {
    data class SignIn(val login: String, val password: String) : AuthEvent
    object SignInGoogle : AuthEvent
    object SignInFacebook : AuthEvent
    object SignInMicrosoft : AuthEvent
    object RegisterClick : AuthEvent
    object ForgotPasswordClick : AuthEvent
    object BackClick : AuthEvent
}
