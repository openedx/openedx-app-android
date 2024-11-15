package org.openedx.profile.presentation.anothersaccount

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.R
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.ui.ProfileInfoSection
import org.openedx.profile.presentation.ui.ProfileTopic

class AnothersProfileFragment : Fragment() {

    private val viewModel: AnothersProfileViewModel by viewModel {
        parametersOf(requireArguments().getString(ARG_USERNAME, ""))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
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

                val uiState by viewModel.uiState
                val uiMessage by viewModel.uiMessage

                AnothersProfileScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                )
            }
        }
    }

    companion object {
        private const val ARG_USERNAME = "username"
        fun newInstance(
            username: String,
        ): AnothersProfileFragment {
            val fragment = AnothersProfileFragment()
            fragment.arguments = bundleOf(
                ARG_USERNAME to username
            )
            return fragment
        }
    }
}

@Composable
private fun AnothersProfileScreen(
    windowSize: WindowSize,
    uiState: AnothersProfileUIState,
    uiMessage: UIMessage?,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()

    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                compact = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        )
    }

    val topBarWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier
                    .fillMaxWidth()
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsInset(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .then(topBarWidth),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.core_profile),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    BackBtn {
                        onBackClick()
                    }
                }
            }
        }
    ) { paddingValues ->
        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .background(MaterialTheme.appColors.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState) {
                is AnothersProfileUIState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                    }
                }

                is AnothersProfileUIState.Data -> {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .then(contentWidth)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProfileTopic(
                            image = uiState.account.profileImage.imageUrlFull,
                            title = uiState.account.name,
                            subtitle = uiState.account.username
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        ProfileInfoSection(uiState.account)

                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() {
    OpenEdXTheme {
        AnothersProfileScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = AnothersProfileUIState.Data(mockAccount),
            uiMessage = null,
            onBackClick = {}
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenTabletPreview() {
    OpenEdXTheme {
        AnothersProfileScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = AnothersProfileUIState.Data(mockAccount),
            uiMessage = null,
            onBackClick = {}
        )
    }
}

private val mockAccount = Account(
    username = "thom84",
    bio = "He as compliment unreserved projecting. Between had observe pretend delight for believe. Do newspaper " +
            "questions consulted sweetness do. Our sportsman his unwilling fulfilled departure law.",
    requiresParentalConsent = true,
    name = "Thomas",
    country = "Ukraine",
    isActive = true,
    profileImage = ProfileImage("", "", "", "", false),
    yearOfBirth = 2000,
    levelOfEducation = "Bachelor",
    goals = "130",
    languageProficiencies = emptyList(),
    gender = "male",
    mailingAddress = "",
    "",
    null,
    accountPrivacy = Account.Privacy.ALL_USERS
)
