package org.openedx.course.presentation.handouts

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.extension.isEmailValid
import org.openedx.core.extension.replaceLinkTags
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.EmailUtil
import java.nio.charset.StandardCharsets

class WebViewFragment : Fragment() {

    private val viewModel by viewModel<HandoutsViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TYPE, "")
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val htmlBody by viewModel.htmlContent.observeAsState("")
                val colorBackgroundValue = MaterialTheme.appColors.background.value
                val colorTextValue = MaterialTheme.appColors.textPrimary.value

                WebContentScreen(
                    windowSize = windowSize,
                    title = requireArguments().getString(ARG_TITLE, ""),
                    htmlBody = viewModel.injectDarkMode(
                        htmlBody,
                        colorBackgroundValue,
                        colorTextValue
                    ),
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    })
            }
        }
    }

    companion object {
        private val ARG_TITLE = "argTitle"
        private val ARG_TYPE = "argType"
        private val ARG_COURSE_ID = "argCourse"

        fun newInstance(
            title: String,
            type: String,
            courseId: String
        ): WebViewFragment {
            val fragment = WebViewFragment()
            fragment.arguments = bundleOf(
                ARG_TITLE to title,
                ARG_TYPE to type,
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }
}

@Composable
private fun WebContentScreen(
    windowSize: WindowSize,
    title: String,
    onBackClick: () -> Unit,
    htmlBody: String
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) {

        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(screenWidth) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BackBtn {
                        onBackClick()
                    }

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        text = title,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    Modifier.fillMaxSize(),
                    color = MaterialTheme.appColors.background
                ) {
                    if (htmlBody.isNotEmpty()) {
                        var webViewAlpha by rememberSaveable { mutableStateOf(0f) }
                        Surface(
                            Modifier
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                                .alpha(webViewAlpha),
                            color = MaterialTheme.appColors.background
                        ) {
                            HandoutsContent(body = htmlBody, onWebPageLoaded = {
                                webViewAlpha = 1f
                            })
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.appColors.background)
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

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun HandoutsContent(body: String, onWebPageLoaded: () -> Unit) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    AndroidView(modifier = Modifier, factory = {
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    onWebPageLoaded()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val clickUrl = request?.url?.toString() ?: ""
                    return if (clickUrl.isNotEmpty() &&
                        (clickUrl.startsWith("http://") ||
                                clickUrl.startsWith("https://"))
                    ) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl)))
                        true
                    } else if (clickUrl.startsWith("mailto:")) {
                        val email = clickUrl.replace("mailto:", "")
                        if (email.isEmailValid()) {
                            EmailUtil.sendEmailIntent(context, email, "", "")
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            }
            with(settings) {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                setSupportZoom(true)
                loadsImagesAutomatically = true
                domStorageEnabled = true
            }
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            loadDataWithBaseURL(
                org.openedx.core.BuildConfig.BASE_URL,
                body.replaceLinkTags(isDarkTheme),
                "text/html",
                StandardCharsets.UTF_8.name(),
                null
            )
        }
    }, update = {
        it.loadDataWithBaseURL(
            org.openedx.core.BuildConfig.BASE_URL,
            body.replaceLinkTags(isDarkTheme),
            "text/html",
            StandardCharsets.UTF_8.name(),
            null
        )
    })
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WebContentScreenPreview() {
    WebContentScreen(
        windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
        title = "Handouts", onBackClick = { }, htmlBody = ""
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
fun WebContentScreenTabletPreview() {
    WebContentScreen(
        windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
        title = "Handouts", onBackClick = { }, htmlBody = ""
    )
}