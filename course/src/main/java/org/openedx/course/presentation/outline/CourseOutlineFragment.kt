package org.openedx.course.presentation.outline

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.container.CourseContainerFragment
import org.openedx.course.presentation.container.CourseContainerTab
import org.openedx.course.presentation.outline.CourseOutlineFragment.Companion.getUnitBlockIcon
import org.openedx.course.presentation.ui.CourseDatesBanner
import org.openedx.course.presentation.ui.CourseDatesBannerTablet
import org.openedx.course.presentation.ui.CourseExpandableChapterCard
import org.openedx.course.presentation.ui.CourseImageHeader
import org.openedx.course.presentation.ui.CourseSectionCard
import org.openedx.course.presentation.ui.CourseSubSectionItem
import java.io.File
import java.util.Date

class CourseOutlineFragment : Fragment() {

    private val viewModel by viewModel<CourseOutlineViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    private var snackBar: Snackbar? = null

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

                val uiState by viewModel.uiState.observeAsState(CourseOutlineUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.isUpdating.observeAsState(false)

                CourseOutlineScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    apiHostUrl = viewModel.apiHostUrl,
                    isCourseNestedListEnabled = viewModel.isCourseNestedListEnabled,
                    isCourseBannerEnabled = viewModel.isCourseBannerEnabled,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    onSwipeRefresh = {
                        viewModel.setIsUpdating()
                        (parentFragment as CourseContainerFragment).updateCourseStructure(true)
                    },
                    hasInternetConnection = viewModel.hasInternetConnection,
                    onReloadClick = {
                        (parentFragment as CourseContainerFragment).updateCourseStructure(false)
                    },
                    onItemClick = { block ->
                        viewModel.sequentialClickedEvent(block.blockId, block.displayName)
                        router.navigateToCourseSubsections(
                            fm = requireActivity().supportFragmentManager,
                            courseId = viewModel.courseId,
                            subSectionId = block.id,
                            mode = CourseViewMode.FULL
                        )
                    },
                    onExpandClick = { block ->
                        if (viewModel.switchCourseSections(block.id)) {
                            viewModel.sequentialClickedEvent(block.blockId, block.displayName)
                        }
                    },
                    onSubSectionClick = { subSectionBlock ->
                        viewModel.courseSubSectionUnit[subSectionBlock.id]?.let { unit ->
                            viewModel.verticalClickedEvent(unit.blockId, unit.displayName)
                            router.navigateToCourseContainer(
                                requireActivity().supportFragmentManager,
                                courseId = viewModel.courseId,
                                unitId = unit.id,
                                mode = CourseViewMode.FULL
                            )
                        }
                    },
                    onResumeClick = { componentId ->
                        viewModel.resumeSectionBlock?.let { subSection ->
                            viewModel.resumeCourseTappedEvent(subSection.id)
                            viewModel.resumeVerticalBlock?.let { unit ->
                                router.navigateToCourseSubsections(
                                    requireActivity().supportFragmentManager,
                                    courseId = viewModel.courseId,
                                    subSectionId = subSection.id,
                                    mode = CourseViewMode.FULL,
                                    unitId = unit.id,
                                    componentId = componentId
                                )
                            }
                        }
                    },
                    onDownloadClick = {
                        if (viewModel.isBlockDownloading(it.id)) {
                            router.navigateToDownloadQueue(
                                fm = requireActivity().supportFragmentManager,
                                viewModel.getDownloadableChildren(it.id) ?: arrayListOf()
                            )
                        } else if (viewModel.isBlockDownloaded(it.id)) {
                            viewModel.removeDownloadModels(it.id)
                        } else {
                            viewModel.saveDownloadModels(
                                requireContext().externalCacheDir.toString() +
                                        File.separator +
                                        requireContext()
                                            .getString(R.string.app_name)
                                            .replace(Regex("\\s"), "_"), it.id
                            )
                        }
                    },
                    onResetDatesClick = {
                        viewModel.resetCourseDatesBanner(onResetDates = {
                            (parentFragment as CourseContainerFragment).updateCourseDates()
                            showDatesUpdateSnackbar(it)
                        })
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        snackBar?.dismiss()
        super.onDestroyView()
    }

    private fun showDatesUpdateSnackbar(isSuccess: Boolean) {
        val message = if (isSuccess) {
            getString(R.string.core_dates_shift_dates_successfully_msg)
        } else {
            getString(R.string.core_dates_shift_dates_unsuccessful_msg)
        }
        snackBar = view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).apply {
                if (isSuccess) {
                    setAction(R.string.core_dates_view_all_dates) {
                        (parentFragment as CourseContainerFragment).navigateToTab(CourseContainerTab.DATES)
                    }
                }
            }
        }
        snackBar?.show()
    }


    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        fun newInstance(
            courseId: String,
            title: String
        ): CourseOutlineFragment {
            val fragment = CourseOutlineFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to title
            )
            return fragment
        }

        fun getUnitBlockIcon(block: Block): Int {
            return when (block.type) {
                BlockType.VIDEO -> org.openedx.course.R.drawable.ic_course_video
                BlockType.PROBLEM -> org.openedx.course.R.drawable.ic_course_pen
                BlockType.DISCUSSION -> org.openedx.course.R.drawable.ic_course_discussion
                else -> org.openedx.course.R.drawable.ic_course_block
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CourseOutlineScreen(
    windowSize: WindowSize,
    uiState: CourseOutlineUIState,
    apiHostUrl: String,
    isCourseNestedListEnabled: Boolean,
    isCourseBannerEnabled: Boolean,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    onReloadClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onItemClick: (Block) -> Unit,
    onExpandClick: (Block) -> Unit,
    onSubSectionClick: (Block) -> Unit,
    onResumeClick: (String) -> Unit,
    onDownloadClick: (Block) -> Unit,
    onResetDatesClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

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
                color = MaterialTheme.appColors.background
            ) {
                Box(Modifier.pullRefresh(pullRefreshState)) {
                    when (uiState) {
                        is CourseOutlineUIState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is CourseOutlineUIState.CourseData -> {
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
                                item { Spacer(Modifier.height(28.dp)) }
                                if (uiState.datesBannerInfo.isBannerAvailableForDashboard()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .padding(all = 8.dp)
                                        ) {
                                            if (windowSize.isTablet) {
                                                CourseDatesBannerTablet(
                                                    modifier = Modifier.padding(bottom = 16.dp),
                                                    banner = uiState.datesBannerInfo,
                                                    resetDates = onResetDatesClick,
                                                )
                                            } else {
                                                CourseDatesBanner(
                                                    modifier = Modifier.padding(bottom = 16.dp),
                                                    banner = uiState.datesBannerInfo,
                                                    resetDates = onResetDatesClick,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (uiState.resumeComponent != null) {
                                    item {
                                        Box(listPadding) {
                                            if (windowSize.isTablet) {
                                                ResumeCourseTablet(
                                                    block = uiState.resumeComponent,
                                                    onResumeClick = onResumeClick
                                                )
                                            } else {
                                                ResumeCourse(
                                                    block = uiState.resumeComponent,
                                                    onResumeClick = onResumeClick
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isCourseNestedListEnabled) {
                                    item {
                                        Spacer(Modifier.height(16.dp))
                                    }
                                    uiState.courseStructure.blockData.forEach { section ->
                                        val courseSubSections =
                                            uiState.courseSubSections[section.id]
                                        val courseSectionsState =
                                            uiState.courseSectionsState[section.id]

                                        item {
                                            Column {
                                                CourseExpandableChapterCard(
                                                    modifier = listPadding,
                                                    block = section,
                                                    onItemClick = onExpandClick,
                                                    arrowDegrees = if (courseSectionsState == true) -90f else 90f
                                                )
                                                Divider()
                                            }
                                        }

                                        courseSubSections?.forEach { subSectionBlock ->
                                            item {
                                                Column {
                                                    AnimatedVisibility(
                                                        visible = courseSectionsState == true
                                                    ) {
                                                        Column {
                                                            val downloadsCount =
                                                                uiState.subSectionsDownloadsCount[subSectionBlock.id]
                                                                    ?: 0

                                                            CourseSubSectionItem(
                                                                modifier = listPadding,
                                                                block = subSectionBlock,
                                                                downloadedState = uiState.downloadedState[subSectionBlock.id],
                                                                downloadsCount = downloadsCount,
                                                                onClick = onSubSectionClick,
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
                                                onItemClick = onItemClick,
                                                onDownloadClick = onDownloadClick
                                            )
                                            Divider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PullRefreshIndicator(
                        refreshing,
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

@Composable
private fun ResumeCourse(
    block: Block,
    onResumeClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = org.openedx.course.R.string.course_continue_with),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textPrimaryVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = getUnitBlockIcon(block)),
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary
            )
            Text(
                text = block.displayName,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(24.dp))
        OpenEdXButton(
            text = stringResource(id = org.openedx.course.R.string.course_resume),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                TextIcon(
                    text = stringResource(id = org.openedx.course.R.string.course_resume),
                    painter = painterResource(id = R.drawable.core_ic_forward),
                    color = MaterialTheme.appColors.buttonText,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        )
    }
}


@Composable
private fun ResumeCourseTablet(
    block: Block,
    onResumeClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 35.dp)
        ) {
            Text(
                text = stringResource(id = org.openedx.course.R.string.course_continue_with),
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textPrimaryVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size((MaterialTheme.appTypography.titleMedium.fontSize.value + 4).dp),
                    painter = painterResource(id = getUnitBlockIcon(block)),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimary
                )
                Text(
                    text = block.displayName,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4
                )
            }
        }
        OpenEdXButton(
            modifier = Modifier.width(210.dp),
            text = stringResource(id = org.openedx.course.R.string.course_resume),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                TextIcon(
                    text = stringResource(id = org.openedx.course.R.string.course_resume),
                    painter = painterResource(id = R.drawable.core_ic_forward),
                    color = MaterialTheme.appColors.buttonText,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseOutlineScreenPreview() {
    OpenEdXTheme {
        CourseOutlineScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseOutlineUIState.CourseData(
                mockCourseStructure,
                mapOf(),
                mockChapterBlock,
                mapOf(),
                mapOf(),
                mapOf(),
                CourseDatesBannerInfo(
                    missedDeadlines = false,
                    missedGatedContent = false,
                    verifiedUpgradeLink = "",
                    contentTypeGatingEnabled = false,
                    hasEnded = false
                )
            ),
            apiHostUrl = "",
            isCourseNestedListEnabled = true,
            isCourseBannerEnabled = true,
            uiMessage = null,
            refreshing = false,
            hasInternetConnection = true,
            onSwipeRefresh = {},
            onItemClick = {},
            onExpandClick = {},
            onSubSectionClick = {},
            onResumeClick = {},
            onReloadClick = {},
            onDownloadClick = {},
            onResetDatesClick = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseOutlineScreenTabletPreview() {
    OpenEdXTheme {
        CourseOutlineScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseOutlineUIState.CourseData(
                mockCourseStructure,
                mapOf(),
                mockChapterBlock,
                mapOf(),
                mapOf(),
                mapOf(),
                CourseDatesBannerInfo(
                    missedDeadlines = false,
                    missedGatedContent = false,
                    verifiedUpgradeLink = "",
                    contentTypeGatingEnabled = false,
                    hasEnded = false
                )
            ),
            apiHostUrl = "",
            isCourseNestedListEnabled = true,
            isCourseBannerEnabled = true,
            uiMessage = null,
            refreshing = false,
            hasInternetConnection = true,
            onSwipeRefresh = {},
            onItemClick = {},
            onExpandClick = {},
            onSubSectionClick = {},
            onResumeClick = {},
            onReloadClick = {},
            onDownloadClick = {},
            onResetDatesClick = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ResumeCoursePreview() {
    OpenEdXTheme {
        ResumeCourse(mockChapterBlock) {}
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
    descendantsType = BlockType.CHAPTER,
    completion = 0.0,
    containsGatedContent = false
)

private val mockCourseStructure = CourseStructure(
    root = "",
    blockData = listOf(mockSequentialBlock, mockSequentialBlock),
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
