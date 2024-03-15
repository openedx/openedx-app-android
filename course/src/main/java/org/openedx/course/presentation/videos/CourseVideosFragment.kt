package org.openedx.course.presentation.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.R
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.settings.VideoQualityType
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.container.CourseContainerFragment
import org.openedx.course.presentation.ui.CourseVideosScreen
import java.io.File

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
                val videoSettings by viewModel.videoSettings.collectAsState()

                CourseVideosScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    courseTitle = viewModel.courseTitle,
                    apiHostUrl = viewModel.apiHostUrl,
                    isCourseNestedListEnabled = viewModel.isCourseNestedListEnabled,
                    isCourseBannerEnabled = viewModel.isCourseBannerEnabled,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    isUpdating = isUpdating,
                    videoSettings = videoSettings,
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
                    onSubSectionClick = { subSectionBlock ->
                        viewModel.courseSubSectionUnit[subSectionBlock.id]?.let { unit ->
                            viewModel.sequentialClickedEvent(unit.blockId, unit.displayName)
                            router.navigateToCourseContainer(
                                fm = requireActivity().supportFragmentManager,
                                courseId = viewModel.courseId,
                                unitId = unit.id,
                                mode = CourseViewMode.VIDEOS
                            )
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
                    onDownloadAllClick = { isAllBlocksDownloadedOrDownloading ->
                        viewModel.logBulkDownloadToggleEvent(isAllBlocksDownloadedOrDownloading)
                        if (isAllBlocksDownloadedOrDownloading) {
                            viewModel.removeAllDownloadModels()
                        } else {
                            viewModel.saveAllDownloadModels(
                                requireContext().externalCacheDir.toString() +
                                        File.separator +
                                        requireContext()
                                            .getString(R.string.app_name)
                                            .replace(Regex("\\s"), "_")
                            )
                        }
                    },
                    onDownloadQueueClick = {
                        if (viewModel.hasDownloadModelsInQueue()) {
                            router.navigateToDownloadQueue(fm = requireActivity().supportFragmentManager)
                        }
                    },
                    onVideoDownloadQualityClick = {
                        if (viewModel.hasDownloadModelsInQueue()) {
                            viewModel.onChangingVideoQualityWhileDownloading()
                        } else {
                            router.navigateToVideoQuality(
                                requireActivity().supportFragmentManager, VideoQualityType.Download
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
            title: String,
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

