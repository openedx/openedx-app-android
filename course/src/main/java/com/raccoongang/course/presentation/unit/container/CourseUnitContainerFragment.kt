package com.raccoongang.course.presentation.unit.container

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.raccoongang.course.R
import com.raccoongang.course.databinding.FragmentCourseUnitContainerBinding
import com.raccoongang.course.presentation.ChapterEndFragmentDialog
import com.raccoongang.course.presentation.ui.NavigationUnitsButtons
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

        if (savedInstanceState == null && childFragmentManager.findFragmentById(R.id.unitContainer) == null) {
            val fragment = unitBlockFragment(viewModel.getCurrentBlock())
            childFragmentManager.beginTransaction()
                .replace(R.id.unitContainer, fragment)
                .commit()
        }

        binding.cvNavigationBar.setContent {
            NewEdxTheme {
                var prevButtonText by rememberSaveable {
                    mutableStateOf(viewModel.prevButtonText)
                }
                var nextButtonText by rememberSaveable {
                    mutableStateOf(viewModel.nextButtonText)
                }
                var hasNextBlock by rememberSaveable {
                    mutableStateOf(viewModel.hasNextBlock)
                }

                val windowSize = rememberWindowSize()

                updateNavigationButtons { prev, next, bool ->
                    prevButtonText = prev
                    nextButtonText = next
                    hasNextBlock = bool
                }

                NavigationUnitsButtons(
                    windowSize = windowSize,
                    prevButtonText = prevButtonText,
                    nextButtonText = nextButtonText,
                    hasNextBlock = hasNextBlock,
                    onPrevClick = {
                        val block = viewModel.moveToPrevBlock()
                        if (block != null) {
                            if (!block.type.isContainer()) {
                                navigateToUnit(block, R.id.unitContainer)
                                updateNavigationButtons { prev, next, bool ->
                                    prevButtonText = prev
                                    nextButtonText = next
                                    hasNextBlock = bool
                                }
                            }
                        }
                    },
                    onNextClick = {
                        val block = viewModel.moveToNextBlock()
                        if (block != null) {
                            if (!block.type.isContainer()) {
                                navigateToUnit(block, R.id.unitContainer)
                                updateNavigationButtons { prev, next, bool ->
                                    prevButtonText = prev
                                    nextButtonText = next
                                    hasNextBlock = bool
                                }
                            } else {
                                viewModel.sendEventPauseVideo()
                                val dialog = ChapterEndFragmentDialog.newInstance(block.displayName)
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

        binding.btnBack.setContent {
            NewEdxTheme {
                BackBtn {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

    }

    private fun updateNavigationButtons(updatedData: (String?, String, Boolean) -> Unit) {
        val prevButtonText = if (viewModel.isFirstIndexInContainer) {
            null
        } else {
            getString(R.string.course_navigation_prev)
        }
        val hasNextBlock: Boolean
        val nextButtonText = if (viewModel.isLastIndexInContainer) {
            hasNextBlock = false
            getString(R.string.course_navigation_finish)
        } else {
            hasNextBlock = true
            getString(R.string.course_navigation_next)
        }
        updatedData(prevButtonText, nextButtonText, hasNextBlock)
    }

    private fun navigateToUnit(block: Block, containerId: Int) {
        childFragmentManager.beginTransaction()
            .replace(containerId, unitBlockFragment(block))
            .commit()
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
                            transcripts?.en,
                            block.displayName,
                            isDownloaded
                        )
                    } else {
                        YoutubeVideoUnitFragment.newInstance(
                            block.id,
                            viewModel.courseId,
                            encodedVideos.youtube?.url!!,
                            transcripts?.en,
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