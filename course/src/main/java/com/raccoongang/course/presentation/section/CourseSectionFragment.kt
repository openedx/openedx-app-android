package com.raccoongang.course.presentation.section

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.raccoongang.core.module.db.DownloadedState
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.ui.BackBtn
import com.raccoongang.core.ui.HandleUIMessage
import com.raccoongang.core.ui.WindowSize
import com.raccoongang.core.ui.WindowType
import com.raccoongang.core.ui.rememberWindowSize
import com.raccoongang.core.ui.statusBarsInset
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.core.ui.windowSizeValue
import com.raccoongang.course.R
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.course.presentation.ui.CardArrow
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File

class CourseSectionFragment : Fragment() {

    private val viewModel by viewModel<CourseSectionViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    private var title = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        val blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        viewModel.mode = requireArguments().serializable<CourseViewMode>(ARG_MODE)!!
        title = requireArguments().getString(ARG_TITLE, "")
        viewModel.getBlocks(blockId, viewModel.mode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(CourseSectionUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                CourseSectionScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    title = title,
                    uiMessage = uiMessage,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onItemClick = { block ->
                        if (block.descendants.isNotEmpty()) {
                            router.navigateToCourseContainer(
                                requireActivity().supportFragmentManager,
                                block.id,
                                courseId = viewModel.courseId,
                                courseName = block.displayName,
                                mode = viewModel.mode
                            )
                        }
                    },
                    onDownloadClick = {
                        if (viewModel.isBlockDownloading(it.id)) {
                            viewModel.cancelWork(it.id)
                        } else if (viewModel.isBlockDownloaded(it.id)) {
                            viewModel.removeDownloadedModels(it.id)
                        } else {
                            viewModel.saveDownloadModels(
                                requireContext().externalCacheDir.toString() +
                                        File.separator +
                                        requireContext()
                                            .getString(com.raccoongang.core.R.string.app_name)
                                            .replace(Regex("\\s"), "_"), it.id
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
        private const val ARG_TITLE = "title"
        private const val ARG_MODE = "mode"
        fun newInstance(
            courseId: String,
            blockId: String,
            title: String,
            mode: CourseViewMode,
        ): CourseSectionFragment {
            val fragment = CourseSectionFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_BLOCK_ID to blockId,
                ARG_TITLE to title,
                ARG_MODE to mode
            )
            return fragment
        }
    }
}

@Composable
private fun CourseSectionScreen(
    windowSize: WindowSize,
    uiState: CourseSectionUIState,
    title: String,
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
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    when (uiState) {
                        is CourseSectionUIState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is CourseSectionUIState.Blocks -> {
                            Column(Modifier.fillMaxSize()) {
                                LazyColumn(
                                    contentPadding = listPadding,
                                ) {
                                    items(uiState.blocks) { block ->
                                        CourseSubsectionItem(
                                            block = block,
                                            downloadedState = uiState.downloadedState[block.id],
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
private fun CourseSubsectionItem(
    block: Block,
    downloadedState: DownloadedState?,
    onClick: (Block) -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val icon =
        if (block.completion == 1.0) painterResource(R.drawable.course_ic_task_alt) else painterResource(
            id = getUnitBlockIcon(block)
        )
    val iconColor =
        if (block.completion == 1.0) MaterialTheme.appColors.primary else MaterialTheme.appColors.onSurface

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
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = block.displayName,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (downloadedState == DownloadedState.DOWNLOADED || downloadedState == DownloadedState.NOT_DOWNLOADED) {
                    val iconPainter = if (downloadedState == DownloadedState.DOWNLOADED) {
                        painterResource(id = R.drawable.course_ic_remove_download)
                    } else {
                        painterResource(id = R.drawable.course_ic_start_download)
                    }
                    IconButton(modifier = iconModifier,
                        onClick = { onDownloadClick(block) }) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = MaterialTheme.appColors.textPrimary
                        )
                    }
                } else if (downloadedState != null) {
                    Box(contentAlignment = Alignment.Center) {
                        if (downloadedState == DownloadedState.DOWNLOADING || downloadedState == DownloadedState.WAITING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                color = MaterialTheme.appColors.primary
                            )
                        }
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


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseSectionScreenPreview() {
    NewEdxTheme {
        CourseSectionScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseSectionUIState.Blocks(
                listOf(
                    mockBlock,
                    mockBlock,
                    mockBlock,
                    mockBlock
                ),
                mapOf()
            ),
            "Course default",
            uiMessage = null,
            onBackClick = {},
            onItemClick = {},
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseSectionScreenTabletPreview() {
    NewEdxTheme {
        CourseSectionScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseSectionUIState.Blocks(
                listOf(
                    mockBlock,
                    mockBlock,
                    mockBlock,
                    mockBlock
                ),
                mapOf()
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