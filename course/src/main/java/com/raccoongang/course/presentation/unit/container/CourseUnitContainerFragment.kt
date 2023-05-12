package com.raccoongang.course.presentation.unit.container

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.raccoongang.core.BlockType
import com.raccoongang.core.FragmentViewType
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.extension.serializable
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.presentation.global.InsetHolder
import com.raccoongang.core.presentation.global.viewBinding
import com.raccoongang.core.ui.BackBtn
import com.raccoongang.core.ui.rememberWindowSize
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.course.R
import com.raccoongang.course.databinding.FragmentCourseUnitContainerBinding
import com.raccoongang.course.presentation.ChapterEndFragmentDialog
import com.raccoongang.course.presentation.DialogListener
import com.raccoongang.course.presentation.ui.NavigationUnitsButtons
import com.raccoongang.course.presentation.ui.VerticalPageIndicator
import com.raccoongang.course.presentation.unit.NotSupportedUnitFragment
import com.raccoongang.course.presentation.unit.html.HtmlUnitFragment
import com.raccoongang.course.presentation.unit.video.VideoUnitFragment
import com.raccoongang.course.presentation.unit.video.YoutubeVideoUnitFragment
import com.raccoongang.discussion.presentation.threads.DiscussionThreadsFragment
import com.raccoongang.discussion.presentation.topics.DiscussionTopicsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CourseUnitContainerFragment : Fragment(R.layout.fragment_course_unit_container) {

    private val binding by viewBinding(FragmentCourseUnitContainerBinding::bind)

    private val viewModel by viewModel<CourseUnitContainerViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }

    private var blockId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        viewModel.loadBlocks(requireArguments().serializable(ARG_MODE)!!)
        viewModel.setupCurrentIndex(blockId)
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val insetHolder = requireActivity() as InsetHolder
        val statusBarParams = binding.statusBarInset.layoutParams as ConstraintLayout.LayoutParams
        statusBarParams.topMargin = insetHolder.topInset
        binding.statusBarInset.layoutParams = statusBarParams
        val bottomNavigationParams =
            binding.cvNavigationBar.layoutParams as ConstraintLayout.LayoutParams
        bottomNavigationParams.bottomMargin = insetHolder.bottomInset
        binding.cvNavigationBar.layoutParams = bottomNavigationParams
        val containerParams =
            binding.unitContainer.layoutParams as ConstraintLayout.LayoutParams
        containerParams.bottomMargin = insetHolder.bottomInset
        binding.unitContainer.layoutParams = containerParams

        if (savedInstanceState == null && childFragmentManager.findFragmentById(R.id.unitContainer) == null) {
            val fragment = unitBlockFragment(viewModel.getCurrentBlock())
            childFragmentManager.beginTransaction()
                .replace(R.id.unitContainer, fragment)
                .commit()
        }

        binding.cvNavigationBar.setContent {
            NewEdxTheme {
                var nextButtonText by rememberSaveable {
                    mutableStateOf(viewModel.nextButtonText)
                }
                var hasNextBlock by rememberSaveable {
                    mutableStateOf(viewModel.hasNextBlock)
                }
                var hasPrevBlock by rememberSaveable {
                    mutableStateOf(viewModel.hasNextBlock)
                }

                val windowSize = rememberWindowSize()

                updateNavigationButtons { next, hasPrev, hasNext ->
                    nextButtonText = next
                    hasPrevBlock = hasPrev
                    hasNextBlock = hasNext
                }

                NavigationUnitsButtons(
                    windowSize = windowSize,
                    hasPrevBlock = hasPrevBlock,
                    nextButtonText = nextButtonText,
                    hasNextBlock = hasNextBlock,
                    onPrevClick = {
                        val block = viewModel.moveToPrevBlock()
                        if (block != null) {
                            if (!block.type.isContainer()) {
                                navigateToUnit(block, R.id.unitContainer, true)
                                updateNavigationButtons { next, hasPrev, hasNext ->
                                    nextButtonText = next
                                    hasPrevBlock = hasPrev
                                    hasNextBlock = hasNext
                                }
                            }
                        }
                    },
                    onNextClick = {
                        val block = viewModel.moveToNextBlock()
                        if (block != null) {
                            if (!block.type.isContainer()) {
                                navigateToUnit(block, R.id.unitContainer, false)
                                updateNavigationButtons { next, hasPrev, hasNext ->
                                    nextButtonText = next
                                    hasPrevBlock = hasPrev
                                    hasNextBlock = hasNext
                                }
                            } else {
                                viewModel.sendEventPauseVideo()
                                val dialog = ChapterEndFragmentDialog.newInstance(block.displayName)
                                dialog.listener = object : DialogListener {
                                    override fun <T> onClick(value: T) {
                                        viewModel.proceedToNext()?.let {
                                            if (!it.type.isContainer()) {
                                                navigateToUnit(it, R.id.unitContainer, false)
                                                updateNavigationButtons { next, hasPrev, hasNext ->
                                                    nextButtonText = next
                                                    hasPrevBlock = hasPrev
                                                    hasNextBlock = hasNext
                                                }
                                            }
                                        }
                                    }
                                }
                                dialog.show(
                                    requireActivity().supportFragmentManager,
                                    ChapterEndFragmentDialog::class.simpleName
                                )
                            }
                        }
                    }
                )
            }
        }

        binding.cvCount.setContent {
            NewEdxTheme {

                val index by viewModel.indexInContainer.observeAsState(1)
                val units by viewModel.verticalBlockCounts.observeAsState(1)

                VerticalPageIndicator(
                    numberOfPages = units,
                    selectedColor = MaterialTheme.appColors.primary,
                    defaultColor = MaterialTheme.appColors.bottomSheetToggle,
                    selectedPage = index,
                    defaultRadius = 5.dp,
                    selectedLength = 7.dp,
                    modifier = Modifier
                        .width(24.dp)
                        .padding(end = 6.dp)
                )
            }
        }

        binding.btnBack.setContent {
            NewEdxTheme {
                BackBtn {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

    }

    private fun updateNavigationButtons(updatedData: (String, Boolean, Boolean) -> Unit) {
        val hasPrevBlock: Boolean = !viewModel.isFirstIndexInContainer
        val hasNextBlock: Boolean
        val nextButtonText = if (viewModel.isLastIndexInContainer) {
            hasNextBlock = false
            getString(R.string.course_navigation_finish)
        } else {
            hasNextBlock = true
            getString(R.string.course_navigation_next)
        }
        updatedData(nextButtonText, hasPrevBlock, hasNextBlock)
    }

    private fun navigateToUnit(block: Block, containerId: Int, navigateToPrev: Boolean) {
        with(childFragmentManager.beginTransaction()) {
            if (navigateToPrev) {
                setCustomAnimations(R.anim.course_slide_out_down, R.anim.course_slide_in_down)
            } else {
                setCustomAnimations(R.anim.course_slide_out_up, R.anim.course_slide_in_up)
            }
            replace(containerId, unitBlockFragment(block))
            commit()
        }
    }

    private fun unitBlockFragment(block: Block): Fragment {
        return when (block.type) {
            BlockType.HTML,
            BlockType.PROBLEM,
            BlockType.OPENASSESSMENT,
            BlockType.DRAG_AND_DROP_V2,
            BlockType.WORD_CLOUD,
            BlockType.LTI_CONSUMER,
            -> {
                HtmlUnitFragment.newInstance(block.id, block.studentViewUrl)
            }

            BlockType.VIDEO -> {
                val encodedVideos = block.studentViewData!!.encodedVideos!!
                val transcripts = block.studentViewData!!.transcripts
                with(encodedVideos) {
                    var isDownloaded = false
                    val videoUrl = if (viewModel.getDownloadModelById(block.id) != null) {
                        isDownloaded = true
                        viewModel.getDownloadModelById(block.id)!!.path
                    } else if (fallback != null) {
                        fallback!!.url
                    } else if (hls != null) {
                        hls!!.url
                    } else if (desktopMp4 != null) {
                        desktopMp4!!.url
                    } else if (mobileHigh != null) {
                        mobileHigh!!.url
                    } else if (mobileLow != null) {
                        mobileLow!!.url
                    } else {
                        ""
                    }
                    if (videoUrl.isNotEmpty()) {
                        VideoUnitFragment.newInstance(
                            block.id,
                            viewModel.courseId,
                            videoUrl,
                            transcripts?.toMap() ?: emptyMap(),
                            block.displayName,
                            isDownloaded
                        )
                    } else {
                        YoutubeVideoUnitFragment.newInstance(
                            block.id,
                            viewModel.courseId,
                            encodedVideos.youtube?.url!!,
                            transcripts?.toMap() ?: emptyMap(),
                            block.displayName
                        )
                    }
                }
            }

            BlockType.DISCUSSION -> {
                DiscussionThreadsFragment.newInstance(
                    DiscussionTopicsFragment.TOPIC,
                    viewModel.courseId,
                    block.studentViewData?.topicId ?: "",
                    block.displayName,
                    FragmentViewType.MAIN_CONTENT.name
                )
            }

            else -> {
                NotSupportedUnitFragment.newInstance(
                    block.id,
                    block.lmsWebUrl
                )
            }
        }
    }

    companion object {
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_COURSE_NAME = "courseName"
        private const val ARG_MODE = "mode"
        fun newInstance(
            blockId: String,
            courseId: String,
            courseName: String,
            mode: CourseViewMode,
        ): CourseUnitContainerFragment {
            val fragment = CourseUnitContainerFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_COURSE_NAME to courseName,
                ARG_MODE to mode
            )
            return fragment
        }
    }
}