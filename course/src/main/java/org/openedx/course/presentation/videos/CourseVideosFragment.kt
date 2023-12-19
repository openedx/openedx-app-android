package org.openedx.course.presentation.videos

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.container.CourseContainerFragment
import org.openedx.course.presentation.ui.CourseExpandableChapterCard
import org.openedx.course.presentation.ui.CourseImageHeader
import org.openedx.course.presentation.ui.CourseSectionCard
import org.openedx.course.presentation.ui.CourseSectionItem
import java.io.File
import java.util.Date

class CourseVideosFragment : Fragment() {

    private val viewModel by viewModel<CourseVideoViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        with(requireArguments()) {
            viewModel.courseTitle = getString(ARG_TITLE, "")
        }
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

                val uiState by viewModel.uiState.observeAsState(CourseVideosUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val isUpdating by viewModel.isUpdating.observeAsState(false)

                CourseVideosScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    apiHostUrl = viewModel.apiHostUrl,
                    isCourseNestedListEnabled = viewModel.isCourseNestedListEnabled,
                    isCourseBannerEnabled = viewModel.isCourseBannerEnabled,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    isUpdating = isUpdating,
                    onSwipeRefresh = {
                        viewModel.setIsUpdating()
                        (parentFragment as CourseContainerFragment).updateCourseStructure(true)
                    },
                    onReloadClick = {
                        (parentFragment as CourseContainerFragment).updateCourseStructure(false)
                    },
                    onItemClick = { block ->
                        router.navigateToCourseSubsections(
                            fm = requireActivity().supportFragmentManager,
                            courseId = viewModel.courseId,
                            subSectionId = block.id,
                            mode = CourseViewMode.VIDEOS
                        )
                    },
                    onExpandClick = { block ->
                        viewModel.switchCourseSections(block.id)
                    },
                    onSectionClick = { sectionBlock ->
                        viewModel.courseSubSection[sectionBlock.id]?.let { block ->
                            viewModel.verticalClickedEvent(block.blockId, block.displayName)
                            router.navigateToCourseContainer(
                                fm = requireActivity().supportFragmentManager,
                                courseId = viewModel.courseId,
                                unitId = block.id,
                                mode = CourseViewMode.VIDEOS
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
                                            .getString(R.string.app_name)
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
        private const val ARG_TITLE = "title"
        fun newInstance(
            courseId: String,
            title: String
        ): CourseVideosFragment {
            val fragment = CourseVideosFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to title
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CourseVideosScreen(
    windowSize: WindowSize,
    uiState: CourseVideosUIState,
    uiMessage: UIMessage?,
    apiHostUrl: String,
    isCourseNestedListEnabled: Boolean,
    isCourseBannerEnabled: Boolean,
    isUpdating: Boolean,
    hasInternetConnection: Boolean,
    onSwipeRefresh: () -> Unit,
    onItemClick: (Block) -> Unit,
    onExpandClick: (Block) -> Unit,
    onSectionClick: (Block) -> Unit,
    onReloadClick: () -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState =
        rememberPullRefreshState(refreshing = isUpdating, onRefresh = { onSwipeRefresh() })

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
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

        val listBottomPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(bottom = 24.dp),
                    compact = PaddingValues(bottom = 24.dp)
                )
            )
        }

        val listPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.padding(horizontal = 6.dp),
                    compact = Modifier.padding(horizontal = 24.dp)
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = screenWidth,
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.screenBackgroundShape
            ) {
                Box(Modifier.pullRefresh(pullRefreshState)) {
                    Column(
                        Modifier
                            .fillMaxSize()
                    ) {
                        when (uiState) {
                            is CourseVideosUIState.Empty -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(id = org.openedx.course.R.string.course_does_not_include_videos),
                                        color = MaterialTheme.appColors.textPrimary,
                                        style = MaterialTheme.appTypography.headlineSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 40.dp)
                                    )
                                }
                            }

                            is CourseVideosUIState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                }
                            }

                            is CourseVideosUIState.CourseData -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = listBottomPadding
                                ) {
                                    if (isCourseBannerEnabled) {
                                        item {
                                            CourseImageHeader(
                                                modifier = Modifier
                                                    .aspectRatio(1.86f)
                                                    .padding(6.dp),
                                                apiHostUrl = apiHostUrl,
                                                courseImage = uiState.courseStructure.media?.image?.large
                                                    ?: "",
                                                courseCertificate = uiState.courseStructure.certificate,
                                                courseName = uiState.courseStructure.name
                                            )
                                        }
                                    }

                                    if (isCourseNestedListEnabled) {
                                        item {
                                            Spacer(Modifier.height(16.dp))
                                        }
                                        uiState.courseStructure.blockData.forEach { block ->
                                            val courseSections =
                                                uiState.courseSections[block.id]
                                            val courseSectionsState =
                                                uiState.courseSectionsState[block.id]

                                            item {
                                                Column {
                                                    CourseExpandableChapterCard(
                                                        modifier = listPadding,
                                                        block = block,
                                                        downloadedState = uiState.downloadedState[block.id],
                                                        onItemClick = { blockSelected ->
                                                            onExpandClick(blockSelected)
                                                        },
                                                        onDownloadClick = onDownloadClick,
                                                        arrowDegrees = if (courseSectionsState == true) -90f else 90f
                                                    )
                                                    Divider()
                                                }
                                            }

                                            courseSections?.forEach { subSectionBlock ->
                                                item {
                                                    Column {
                                                        AnimatedVisibility(
                                                            visible = courseSectionsState == true
                                                        ) {
                                                            Column {
                                                                val downloadsCount =
                                                                    uiState.downloadsCount[subSectionBlock.id]
                                                                        ?: 0

                                                                CourseSectionItem(
                                                                    modifier = listPadding,
                                                                    block = subSectionBlock,
                                                                    downloadedState = uiState.downloadedState[subSectionBlock.id],
                                                                    downloadsCount = downloadsCount,
                                                                    onClick = { sectionBlock ->
                                                                        onSectionClick(
                                                                            sectionBlock
                                                                        )
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
                                        return@LazyColumn
                                    }

                                    items(uiState.courseStructure.blockData) { block ->
                                        Column(listPadding) {
                                            if (block.type == BlockType.CHAPTER) {
                                                Text(
                                                    modifier = Modifier.padding(
                                                        top = 36.dp,
                                                        bottom = 8.dp
                                                    ),
                                                    text = block.displayName,
                                                    style = MaterialTheme.appTypography.titleMedium,
                                                    color = MaterialTheme.appColors.textPrimaryVariant
                                                )
                                            } else {
                                                CourseSectionCard(
                                                    block = block,
                                                    downloadedState = uiState.downloadedState[block.id],
                                                    onItemClick = { blockSelected ->
                                                        onItemClick(blockSelected)
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
                    PullRefreshIndicator(
                        isUpdating,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter)
                    )
                    if (!isInternetConnectionShown && !hasInternetConnection) {
                        OfflineModeDialog(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            onDismissCLick = {
                                isInternetConnectionShown = true
                            },
                            onReloadClick = {
                                isInternetConnectionShown = true
                                onReloadClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseVideosScreenPreview() {
    OpenEdXTheme {
        CourseVideosScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiMessage = null,
            uiState = CourseVideosUIState.CourseData(
                mockCourseStructure,
                emptyMap(),
                mapOf(),
                mapOf(),
                mapOf()
            ),
            apiHostUrl = "",
            isCourseNestedListEnabled = false,
            isCourseBannerEnabled = true,
            onItemClick = { },
            onExpandClick = { },
            onSectionClick = { },
            hasInternetConnection = true,
            isUpdating = false,
            onSwipeRefresh = {},
            onReloadClick = {},
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseVideosScreenEmptyPreview() {
    OpenEdXTheme {
        CourseVideosScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiMessage = null,
            uiState = CourseVideosUIState.Empty(
                "This course does not include any videos."
            ),
            apiHostUrl = "",
            isCourseNestedListEnabled = false,
            isCourseBannerEnabled = true,
            onItemClick = { },
            onExpandClick = { },
            onSectionClick = { },
            onSwipeRefresh = {},
            onReloadClick = {},
            hasInternetConnection = true,
            isUpdating = false,
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseVideosScreenTabletPreview() {
    OpenEdXTheme {
        CourseVideosScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiMessage = null,
            uiState = CourseVideosUIState.CourseData(
                mockCourseStructure,
                emptyMap(),
                mapOf(),
                mapOf(),
                mapOf()
            ),
            apiHostUrl = "",
            isCourseNestedListEnabled = false,
            isCourseBannerEnabled = true,
            onItemClick = { },
            onExpandClick = { },
            onSectionClick = { },
            onSwipeRefresh = {},
            onReloadClick = {},
            isUpdating = false,
            hasInternetConnection = true,
            onDownloadClick = {}
        )
    }
}


private val mockChapterBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.CHAPTER,
    displayName = "Chapter",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    descendantsType = BlockType.CHAPTER,
    completion = 0.0,
    containsGatedContent = false
)

private val mockSequentialBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.SEQUENTIAL,
    displayName = "Sequential",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    descendantsType = BlockType.SEQUENTIAL,
    completion = 0.0,
    containsGatedContent = false
)

private val mockCourseStructure = CourseStructure(
    root = "",
    blockData = listOf(mockSequentialBlock, mockChapterBlock),
    id = "id",
    name = "Course name",
    number = "",
    org = "Org",
    start = Date(),
    startDisplay = "",
    startType = "",
    end = Date(),
    coursewareAccess = CoursewareAccess(
        true,
        "",
        "",
        "",
        "",
        ""
    ),
    media = null,
    certificate = null,
    isSelfPaced = false
)