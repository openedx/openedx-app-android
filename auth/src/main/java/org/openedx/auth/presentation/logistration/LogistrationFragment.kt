package org.openedx.auth.presentation.logistration

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.auth.R
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.SearchBar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.theme.compose.LogistrationLogoView

class LogistrationFragment : Fragment() {

    private val viewModel: LogistrationViewModel by viewModel {
        parametersOf(arguments?.getString(ARG_COURSE_ID, "") ?: "")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                LogistrationScreen(
                    onSignInClick = {
                        viewModel.navigateToSignIn(parentFragmentManager)
                    },
                    onRegisterClick = {
                        viewModel.navigateToSignUp(parentFragmentManager)
                    },
                    onSearchClick = { querySearch ->
                        viewModel.navigateToDiscovery(parentFragmentManager, querySearch)
                    },
                    isRegistrationEnabled = viewModel.isRegistrationEnabled
                )
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        fun newInstance(courseId: String?): LogistrationFragment {
            val fragment = LogistrationFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LogistrationScreen(
    onSearchClick: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
    isRegistrationEnabled: Boolean,
) {
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .semantics {
                testTagsAsResourceId = true
            }
            .fillMaxSize()
            .navigationBarsPadding(),
        backgroundColor = MaterialTheme.appColors.background
    ) {
        Surface(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .displayCutoutForLandscape(),
            color = MaterialTheme.appColors.background
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 32.dp,
                )
            ) {
                LogistrationLogoView()
                Text(
                    text = stringResource(id = R.string.pre_auth_title),
                    style = MaterialTheme.appTypography.headlineSmall,
                    modifier = Modifier
                        .testTag("txt_screen_title")
                        .padding(bottom = 40.dp)
                )
                val focusManager = LocalFocusManager.current
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(
                        modifier = Modifier
                            .testTag("txt_search_label")
                            .padding(bottom = 10.dp),
                        style = MaterialTheme.appTypography.titleMedium,
                        text = stringResource(id = R.string.pre_auth_search_title),
                    )
                    SearchBar(
                        modifier = Modifier
                            .testTag("tf_discovery_search")
                            .fillMaxWidth()
                            .height(48.dp),
                        label = stringResource(id = R.string.pre_auth_search_hint),
                        requestFocus = false,
                        searchValue = textFieldValue,
                        clearOnSubmit = true,
                        keyboardActions = {
                            focusManager.clearFocus()
                            onSearchClick(textFieldValue.text)
                        },
                        onValueChanged = { text ->
                            textFieldValue = text
                        },
                        onClearValue = {
                            textFieldValue = TextFieldValue("")
                        }
                    )
                }

                Text(
                    modifier = Modifier
                        .testTag("txt_explore_all_courses")
                        .padding(bottom = 32.dp)
                        .noRippleClickable {
                            onSearchClick("")
                        },
                    text = stringResource(id = R.string.pre_auth_explore_all_courses),
                    color = MaterialTheme.appColors.primary,
                    style = MaterialTheme.appTypography.labelLarge,
                    textDecoration = TextDecoration.Underline
                )

                Spacer(modifier = Modifier.weight(1f))

                AuthButtonsPanel(
                    onRegisterClick = onRegisterClick,
                    onSignInClick = onSignInClick,
                    showRegisterButton = isRegistrationEnabled
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Night", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LogistrationPreview() {
    OpenEdXTheme {
        LogistrationScreen(
            onSearchClick = {},
            onSignInClick = {},
            onRegisterClick = {},
            isRegistrationEnabled = true,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Night", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LogistrationRegistrationDisabledPreview() {
    OpenEdXTheme {
        LogistrationScreen(
            onSearchClick = {},
            onSignInClick = {},
            onRegisterClick = {},
            isRegistrationEnabled = false,
        )
    }
}
