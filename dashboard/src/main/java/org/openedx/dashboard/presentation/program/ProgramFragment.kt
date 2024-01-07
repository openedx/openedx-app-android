package org.openedx.dashboard.presentation.program

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.R
import org.openedx.core.presentation.catalog.CatalogWebViewScreen
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.system.AppCookieManager
import org.openedx.core.ui.ConnectionErrorView
import org.openedx.core.ui.ToolbarWithBackBtn
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.windowSizeValue

class ProgramFragment : Fragment() {

    private val viewModel by viewModel<ProgramViewModel>()
    private val edxCookieManager by inject<AppCookieManager>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.hasInternetConnection)
                }
                val coroutineScope = rememberCoroutineScope()

                ProgramInfoScreen(
                    windowSize = windowSize,
                    contentUrl = getInitialUrl(),
                    uriScheme = viewModel.uriScheme,
                    hasInternetConnection = hasInternetConnection,
                    checkInternetConnection = {
                        hasInternetConnection = viewModel.hasInternetConnection
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onInfoCardClicked = { pathId, _ ->
                        viewModel.infoCardClicked(
                            fragmentManager = requireActivity().supportFragmentManager,
                            pathId = pathId
                        )
                    },
                    openExternalLink = { url ->
                        ActionDialogFragment.newInstance(
                            title = getString(R.string.core_leaving_the_app),
                            message = getString(
                                R.string.core_leaving_the_app_message,
                                getString(R.string.platform_name)
                            ),
                            url = url,
                        ).show(
                            requireActivity().supportFragmentManager,
                            ActionDialogFragment::class.simpleName
                        )
                    },
                    refreshSessionCookie = {
                        coroutineScope.launch {
                            edxCookieManager.tryToRefreshSessionCookie()
                        }
                    },
                )
            }
        }
    }

    private fun getInitialUrl(): String {
        return arguments?.let { args ->
            val pathId = args.getString(ARG_PATH_ID) ?: ""
            viewModel.programConfig.programDetailUrlTemplate.replace("{$ARG_PATH_ID}", pathId)
        } ?: viewModel.programConfig.programUrl
    }

    companion object {
        private const val ARG_PATH_ID = "path_id"

        fun newInstance(
            pathId: String,
        ): ProgramFragment {
            val fragment = ProgramFragment()
            fragment.arguments = bundleOf(
                ARG_PATH_ID to pathId,
            )
            return fragment
        }
    }
}

@Composable
private fun ProgramInfoScreen(
    windowSize: WindowSize,
    contentUrl: String,
    uriScheme: String,
    hasInternetConnection: Boolean,
    checkInternetConnection: () -> Unit,
    onBackClick: () -> Unit,
    openExternalLink: (String) -> Unit,
    onInfoCardClicked: (String, String) -> Unit,
    refreshSessionCookie: () -> Unit = {},
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current
    var isLoading by remember { mutableStateOf(true) }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background
    ) {
        val modifierScreenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        Modifier.widthIn(Dp.Unspecified, 560.dp)
                    } else {
                        Modifier.widthIn(Dp.Unspecified, 650.dp)
                    },
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Column(
            modifier = modifierScreenWidth
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ToolbarWithBackBtn(
                label = stringResource(id = org.openedx.dashboard.R.string.dashboard_programs),
                onBackClick = onBackClick
            )

            Surface {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (hasInternetConnection) {
                        val webView = CatalogWebViewScreen(
                            url = contentUrl,
                            uriScheme = uriScheme,
                            isAllLinksExternal = true,
                            onWebPageLoaded = { isLoading = false },
                            openExternalLink = openExternalLink,
                            onInfoCardClicked = onInfoCardClicked,
                            refreshSessionCookie = refreshSessionCookie
                        )

                        AndroidView(
                            modifier = Modifier
                                .background(MaterialTheme.appColors.background),
                            factory = {
                                webView
                            },
                            update = {
                                webView.loadUrl(contentUrl)
                            }
                        )
                    } else {
                        ConnectionErrorView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(MaterialTheme.appColors.background)
                        ) {
                            checkInternetConnection()
                        }
                    }
                    if (isLoading && hasInternetConnection) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MyProgramsPreview() {
    OpenEdXTheme {
        ProgramInfoScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            contentUrl = "https://www.edx.org/",
            uriScheme = "https",
            hasInternetConnection = false,
            checkInternetConnection = {},
            onBackClick = {},
            onInfoCardClicked = { _, _ -> },
            openExternalLink = {}
        )
    }
}