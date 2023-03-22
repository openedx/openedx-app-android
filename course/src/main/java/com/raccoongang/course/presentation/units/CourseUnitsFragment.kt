package com.raccoongang.course.presentation.units

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.raccoongang.core.BlockType
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.domain.model.BlockCounts
import com.raccoongang.core.extension.serializable
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.course.R
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.course.presentation.ui.CardArrow
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File


class CourseUnitsFragment : Fragment() {

    private val viewModel by viewModel<CourseUnitsViewModel>()
    private val router by inject<CourseRouter>()

    private var courseName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        val blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        val mode = requireArguments().serializable<CourseViewMode>(ARG_MODE)!!
        courseName = requireArguments().getString(ARG_COURSE_NAME, "")
        viewModel.getBlocks(blockId, mode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()

                CourseUnitsScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    courseName = courseName,
                    uiMessage = uiMessage,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onItemClick = {
                        router.navigateToCourseContainer(
                            requireActivity().supportFragmentManager,
                            it.id,
                            requireArguments().getString(ARG_COURSE_ID, ""),
                            requireArguments().getString(ARG_COURSE_NAME, ""),
                            requireArguments().serializable(ARG_MODE)!!
                        )
                    },
                    onDownloadClick = {
                        if (it.isDownloading()) {
                            viewModel.cancelWork(it.id)
                        } else if (it.isDownloaded()) {
                            viewModel.removeDownloadedModel(it.id)
                        } else {
                            viewModel.saveDownloadModel(
                                requireContext().externalCacheDir.toString() +
                                        File.separator +
                                        requireContext()
                                            .getString(com.raccoongang.core.R.string.app_name)
                                            .replace(Regex("\\s"), "_"),
                                it
                            )
                        }
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_NAME = "courseName"
        private const val ARG_MODE = "mode"
        fun newInstance(
            courseId: String,
            blockId: String,
            courseName: String,
            mode: CourseViewMode,
        ): CourseUnitsFragment {
            val fragment = CourseUnitsFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_NAME to courseName,
                ARG_MODE to mode
            )
            return fragment
        }
    }
}


@Composable
private fun CourseUnitsScreen(
    windowSize: WindowSize,
    uiState: CourseUnitsUIState,
    courseName: String,
    uiMessage: UIMessage?,
    onBackClick: () -> Unit,
    onItemClick: (Block) -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val listPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(vertical = 24.dp),
                    compact = PaddingValues(24.dp)
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)


        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsInset(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(contentWidth) {
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
                        text = courseName,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    when (uiState) {
                        is CourseUnitsUIState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is CourseUnitsUIState.Blocks -> {
                            Column(Modifier.fillMaxSize()) {
                                LazyColumn(
                                    contentPadding = listPadding,
                                ) {
                                    items(uiState.blocks) { block ->
                                        CourseUnitItem(
                                            block = block,
                                            onClick = {
                                                onItemClick(it)
                                            },
                                            onDownloadClick = onDownloadClick
                                        )
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CourseUnitItem(
    block: Block,
    onClick: (Block) -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val icon =
        if (block.completion == 1.0) painterResource(R.drawable.course_ic_task_alt) else painterResource(
            id = getUnitBlockIcon(block)
        )
    val iconColor =
        if (block.completion == 1.0) MaterialTheme.appColors.primary else MaterialTheme.appColors.textPrimary
    val iconModifier = Modifier.size(24.dp)
    Column(Modifier.clickable { onClick(block) }) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(
                    horizontal = 20.dp,
                    vertical = 24.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconColor
            )
            Spacer(Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = block.displayName,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (block.isDownloaded()) {
                    IconButton(modifier = iconModifier,
                        onClick = { onDownloadClick(block) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.course_ic_remove_download),
                            contentDescription = null,
                            tint = MaterialTheme.appColors.textPrimary
                        )
                    }
                } else if (block.isDownloadable && !block.isDownloading()) {
                    IconButton(modifier = iconModifier,
                        onClick = { onDownloadClick(block) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.course_ic_start_download),
                            contentDescription = null,
                            tint = MaterialTheme.appColors.textPrimary
                        )
                    }
                } else if (block.isDownloadable) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(34.dp))
                        IconButton(modifier = iconModifier,
                            onClick = { onDownloadClick(block) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = MaterialTheme.appColors.error
                            )
                        }
                    }
                }
                CardArrow(
                    degrees = 0f
                )
            }
        }
    }
}

private fun getUnitBlockIcon(block: Block): Int {
    return when (block.type) {
        BlockType.VIDEO -> R.drawable.ic_course_video
        BlockType.PROBLEM -> R.drawable.ic_course_pen
        BlockType.DISCUSSION -> R.drawable.ic_course_discussion
        else -> R.drawable.ic_course_block
    }
}


@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseUnitsScreenPreview() {
    NewEdxTheme {
        CourseUnitsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseUnitsUIState.Blocks(
                listOf(
                    mockBlock,
                    mockBlock,
                    mockBlock,
                    mockBlock
                )
            ),
            "Course default",
            uiMessage = null,
            onBackClick = {},
            onItemClick = {},
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseUnitsScreenTabletPreview() {
    NewEdxTheme {
        CourseUnitsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseUnitsUIState.Blocks(
                listOf(
                    mockBlock,
                    mockBlock,
                    mockBlock,
                    mockBlock
                )
            ),
            "Course default",
            uiMessage = null,
            onBackClick = {},
            onItemClick = {},
            onDownloadClick = {}
        )
    }
}

private val mockBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.HTML,
    displayName = "Block",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(0),
    descendants = emptyList(),
    completion = 0.0
)