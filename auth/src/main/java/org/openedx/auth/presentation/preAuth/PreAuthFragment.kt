package org.openedx.auth.presentation.preAuth

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.openedx.auth.R
import org.openedx.auth.presentation.AuthRouter
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.SearchBar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

class PreAuthFragment : Fragment() {

    private val router: AuthRouter by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                PreLoginScreen(
                    onSignInClick = {
                        router.navigateToSignIn(parentFragmentManager)
                    },
                    onRegisterClick = {
                        router.navigateToSignUp(parentFragmentManager)
                    },
                    onSearchClick = { querySearch ->
                        router.navigateToDiscoverCourses(parentFragmentManager, querySearch)
                    }
                )
            }
        }
    }
}

@Composable
private fun PreLoginScreen(
    onSearchClick: (query: String) -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
) {

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize(),
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
                    start = 16.dp,
                    end = 16.dp,
                    top = 32.dp,
                    bottom = 32.dp
                )
            ) {
                Image(
                    painter = painterResource(id = org.openedx.core.R.drawable.core_ic_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 64.dp, bottom = 20.dp)
                        .wrapContentWidth(),
                    colorFilter = ColorFilter.tint(MaterialTheme.appColors.primary)
                )
                Text(
                    text = stringResource(id = R.string.pre_auth_title),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.headlineSmall,
                    modifier = Modifier.padding(bottom = 40.dp)
                )
                val focusManager = LocalFocusManager.current
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(
                        modifier = Modifier.padding(bottom = 10.dp),
                        style = MaterialTheme.appTypography.titleMedium,
                        text = stringResource(id = R.string.pre_auth_search_title),
                    )
                    SearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        label = stringResource(id = R.string.pre_auth_search_hint),
                        requestFocus = false,
                        searchValue = textFieldValue,
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
                        .padding(bottom = 32.dp)
                        .noRippleClickable {
                            onSearchClick("")
                        },
                    text = stringResource(id = R.string.pre_auth_explore_all_courses),
                    color = MaterialTheme.appColors.primary,
                    style = MaterialTheme.appTypography.labelLarge
                )

                Spacer(modifier = Modifier.weight(1f))

                Row {
                    OpenEdXButton(
                        width = Modifier
                            .width(0.dp)
                            .weight(1f),
                        text = stringResource(id = R.string.auth_register),
                        onClick = { onRegisterClick() }
                    )

                    OpenEdXOutlinedButton(
                        modifier = Modifier
                            .width(100.dp)
                            .padding(start = 16.dp),
                        text = stringResource(id = R.string.auth_sign_in),
                        onClick = { onSignInClick() },
                        borderColor = MaterialTheme.appColors.textFieldBorder,
                        textColor = MaterialTheme.appColors.primary
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Night", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignInScreenPreview() {
    OpenEdXTheme {
        PreLoginScreen(
            onSearchClick = {},
            onSignInClick = {},
            onRegisterClick = {}
        )
    }
}
