package org.openedx.learn.presentation

import android.os.Bundle
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.openedx.core.adapter.NavigationFragmentAdapter
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.crop
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.courses.presentation.PrimaryCourseFragment
import org.openedx.dashboard.R
import org.openedx.dashboard.databinding.FragmentLearnBinding
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.learn.LearnType
import org.openedx.core.R as CoreR

class LearnFragment : Fragment(R.layout.fragment_learn) {

    private val binding by viewBinding(FragmentLearnBinding::bind)
    private val router by inject<DashboardRouter>()
    private lateinit var adapter: NavigationFragmentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.setContent {
            OpenEdXTheme {
                Header(
                    fragmentManager = requireParentFragment().parentFragmentManager,
                    viewPager = binding.viewPager
                )
            }
        }
        initViewPager()
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = 2

        adapter = NavigationFragmentAdapter(this).apply {
            addFragment(PrimaryCourseFragment())
            addFragment(router.getProgramFragmentInstance())
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.setUserInputEnabled(false)
    }
}

@Composable
private fun Header(
    viewModel: LearnViewModel = koinViewModel(),
    fragmentManager: FragmentManager,
    viewPager: ViewPager2
) {
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
        Title(
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
                viewPager = viewPager
            )
        }
    }
}

@Composable
private fun Title(
    modifier: Modifier = Modifier,
    label: String,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            text = label,
            color = MaterialTheme.appColors.textDark,
            style = MaterialTheme.appTypography.headlineBolt
        )
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            onClick = {
                onSettingsClick()
            }
        ) {
            Icon(
                imageVector = Icons.Default.ManageAccounts,
                tint = MaterialTheme.appColors.textAccent,
                contentDescription = stringResource(id = CoreR.string.core_accessibility_settings)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LearnDropdownMenu(
    modifier: Modifier = Modifier,
    viewPager: ViewPager2
) {
    var expanded by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf(LearnType.COURSES) }
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = ""
    )

    LaunchedEffect(currentValue) {
        viewPager.setCurrentItem(
            when (currentValue) {
                LearnType.COURSES -> 0
                LearnType.PROGRAMS -> 1
            }, false
        )
    }

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
                text = stringResource(id = currentValue.title),
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
            shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
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
                    if (currentValue == learnType) {
                        background = MaterialTheme.appColors.primary
                        textColor = MaterialTheme.appColors.buttonText
                    } else {
                        background = Color.Transparent
                        textColor = MaterialTheme.appColors.textDark
                    }
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(background),
                        onClick = {
                            currentValue = learnType
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
        Title(
            label = stringResource(id = R.string.dashboard_learn),
            onSettingsClick = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun LearnDropdownMenuPreview() {
    OpenEdXTheme {
        val context = LocalContext.current
        LearnDropdownMenu(
            viewPager = ViewPager2(context)
        )
    }
}
