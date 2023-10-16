package org.openedx.course.presentation.unit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.R as courseR

class NotSupportedUnitFragment : Fragment() {

    private var blockId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
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
                NotSupportedUnitScreen(
                    windowSize = windowSize,
                    uri = requireArguments().getString(ARG_BLOCK_URL, "")
                )
            }
        }
    }

    companion object {
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_BLOCK_URL = "blockUrl"
        fun newInstance(
            blockId: String,
            blockUrl: String
        ): NotSupportedUnitFragment {
            val fragment = NotSupportedUnitFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_ID to blockId,
                ARG_BLOCK_URL to blockUrl
            )
            return fragment
        }
    }

}

@Composable
private fun NotSupportedUnitScreen(
    windowSize: WindowSize,
    uri: String
) {
    val scaffoldState = rememberScaffoldState()
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState
    ) {

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.width(326.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            )
        }

        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(it)
                    .then(contentWidth),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    painter = painterResource(id = courseR.drawable.course_ic_not_supported_block),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimary
                )
                Spacer(Modifier.height(36.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = courseR.string.course_this_interactive_component),
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = courseR.string.course_explore_other_parts),
                    style = MaterialTheme.appTypography.bodyLarge,
                    color = MaterialTheme.appColors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(40.dp))
                Button(modifier = Modifier
                    .width(216.dp)
                    .height(42.dp),
                    shape = MaterialTheme.appShapes.buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.appColors.buttonBackground
                    ),
                    onClick = {
                        uriHandler.openUri(uri)
                    }) {
                    Text(
                        text = stringResource(id = courseR.string.course_open_in_browser),
                        color = MaterialTheme.appColors.buttonText,
                        style = MaterialTheme.appTypography.labelLarge
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}