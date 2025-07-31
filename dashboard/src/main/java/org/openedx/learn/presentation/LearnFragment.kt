package org.openedx.learn.presentation

import android.os.Bundle
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.adapter.NavigationFragmentAdapter
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.MainToolbar
import org.openedx.core.ui.crop
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.dashboard.R
import org.openedx.dashboard.databinding.FragmentLearnBinding
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.learn.LearnType

class LearnFragment : Fragment(R.layout.fragment_learn) {

    private val binding by viewBinding(FragmentLearnBinding::bind)
    private val viewModel by viewModel<LearnViewModel> {
        parametersOf(requireArguments().getString(ARG_OPEN_TAB, LearnTab.COURSES.name))
    }
    private lateinit var adapter: NavigationFragmentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewPager()
        binding.header.setContent {
            OpenEdXTheme {
                val uiState by viewModel.uiState.collectAsState()
                binding.viewPager.setCurrentItem(
                    when (uiState.learnType) {
                        LearnType.COURSES -> 0
                        LearnType.PROGRAMS -> 1
                    },
                    false
                )
                Header(
                    fragmentManager = requireParentFragment().parentFragmentManager,
                    selectedLearnType = uiState.learnType,
                    onUpdateLearnType = { learnType ->
                        viewModel.updateLearnType(learnType)
                    },
                )
            }
        }
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = 2

        adapter = NavigationFragmentAdapter(this).apply {
            addFragment { viewModel.getDashboardFragment }
            addFragment { viewModel.getProgramFragment }
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.setUserInputEnabled(false)
    }

    companion object {
        private const val ARG_OPEN_TAB = "open_tab"
        fun newInstance(
            openTab: String = LearnTab.COURSES.name,
        ): LearnFragment {
            val fragment = LearnFragment()
            fragment.arguments = bundleOf(
                ARG_OPEN_TAB to openTab
            )
            return fragment
        }
    }
}

@Composable
private fun Header(
    fragmentManager: FragmentManager,
    selectedLearnType: LearnType,
    onUpdateLearnType: (LearnType) -> Unit
) {
    val viewModel: LearnViewModel = koinViewModel()
    val windowSize = rememberWindowSize()
    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 650.dp),
                compact = Modifier.fillMaxWidth(),
            )
        )
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.appColors.background)
            .statusBarsInset()
            .displayCutoutForLandscape()
            .then(contentWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MainToolbar(
            label = stringResource(id = R.string.dashboard_learn),
            onSettingsClick = {
                viewModel.onSettingsClick(fragmentManager)
            }
        )
        if (viewModel.isProgramTypeWebView) {
            LearnDropdownMenu(
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(horizontal = 16.dp),
                selectedLearnType = selectedLearnType,
                onUpdateLearnType = onUpdateLearnType
            )
        }
    }
}

@Composable
private fun LearnDropdownMenu(
    modifier: Modifier = Modifier,
    selectedLearnType: LearnType,
    onUpdateLearnType: (LearnType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = ""
    )

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    expanded = true
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = selectedLearnType.title),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleSmall
            )
            Icon(
                modifier = Modifier.rotate(iconRotation),
                imageVector = Icons.Default.ExpandMore,
                tint = MaterialTheme.appColors.textDark,
                contentDescription = null
            )
        }

        MaterialTheme(
            colors = MaterialTheme.colors.copy(surface = MaterialTheme.appColors.background),
            shapes = MaterialTheme.shapes.copy(
                medium = RoundedCornerShape(
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
            )
        ) {
            DropdownMenu(
                modifier = Modifier
                    .crop(vertical = 8.dp)
                    .widthIn(min = 182.dp),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                for (learnType in LearnType.entries) {
                    val background: Color
                    val textColor: Color
                    if (selectedLearnType == learnType) {
                        background = MaterialTheme.appColors.primary
                        textColor = MaterialTheme.appColors.primaryButtonText
                    } else {
                        background = Color.Transparent
                        textColor = MaterialTheme.appColors.textDark
                    }
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(background),
                        onClick = {
                            onUpdateLearnType(learnType)
                            expanded = false
                        }
                    ) {
                        Text(
                            text = stringResource(id = learnType.title),
                            style = MaterialTheme.appTypography.titleSmall,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HeaderPreview() {
    OpenEdXTheme {
        MainToolbar(
            label = stringResource(id = R.string.dashboard_learn),
            onSettingsClick = {}
        )
    }
}

@Preview
@Composable
private fun LearnDropdownMenuPreview() {
    OpenEdXTheme {
        LearnDropdownMenu(
            selectedLearnType = LearnType.COURSES,
            onUpdateLearnType = {}
        )
    }
}
