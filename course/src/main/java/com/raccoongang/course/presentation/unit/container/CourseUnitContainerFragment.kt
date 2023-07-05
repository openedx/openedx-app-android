package com.raccoongang.course.presentation.unit.container

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
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
import androidx.viewpager2.widget.ViewPager2
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
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.course.presentation.DialogListener
import com.raccoongang.course.presentation.ui.NavigationUnitsButtons
import com.raccoongang.course.presentation.ui.VerticalPageIndicator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CourseUnitContainerFragment : Fragment(R.layout.fragment_course_unit_container) {

    private val binding by viewBinding(FragmentCourseUnitContainerBinding::bind)

    private val viewModel by viewModel<CourseUnitContainerViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }

    private val router by inject<CourseRouter>()

    private var blockId: String = ""

    private lateinit var adapter: CourseUnitContainerAdapter

    private var lastClickTime = 0L

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
        val containerParams =
            binding.viewPager.layoutParams as FrameLayout.LayoutParams
        containerParams.bottomMargin = insetHolder.bottomInset
        binding.viewPager.layoutParams = containerParams

        initViewPager()
        if (savedInstanceState == null) {
            val currentBlockIndex = viewModel.getUnitBlocks().indexOfFirst {
                viewModel.getCurrentBlock().id == it.id
            }
            if (currentBlockIndex != -1) {
                binding.viewPager.currentItem = currentBlockIndex
            }
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
                        if (!restrictDoubleClick()) {
                            val block = viewModel.moveToPrevBlock()
                            if (block != null) {
                                viewModel.prevBlockClickedEvent(block.blockId, block.displayName)
                                if (!block.type.isContainer()) {
                                    binding.viewPager.setCurrentItem(
                                        binding.viewPager.currentItem - 1,
                                        true
                                    )
                                    updateNavigationButtons { next, hasPrev, hasNext ->
                                        nextButtonText = next
                                        hasPrevBlock = hasPrev
                                        hasNextBlock = hasNext
                                    }
                                }
                            }
                        }
                    },
                    onNextClick = {
                        if (!restrictDoubleClick()) {
                            val block = viewModel.moveToNextBlock()
                            if (block != null) {
                                viewModel.nextBlockClickedEvent(block.blockId, block.displayName)
                                if (!block.type.isContainer()) {
                                    binding.viewPager.setCurrentItem(
                                        binding.viewPager.currentItem + 1,
                                        true
                                    )
                                    updateNavigationButtons { next, hasPrev, hasNext ->
                                        nextButtonText = next
                                        hasPrevBlock = hasPrev
                                        hasNextBlock = hasNext
                                    }
                                }
                            } else {
                                val currentVerticalBlock = viewModel.getCurrentVerticalBlock()
                                val nextVerticalBlock = viewModel.getNextVerticalBlock()
                                val dialog = ChapterEndFragmentDialog.newInstance(
                                    currentVerticalBlock?.displayName ?: "",
                                    nextVerticalBlock?.displayName ?: ""
                                )
                                currentVerticalBlock?.let {
                                    viewModel.finishVerticalClickedEvent(
                                        it.blockId,
                                        it.displayName
                                    )
                                }
                                dialog.listener = object : DialogListener {
                                    override fun <T> onClick(value: T) {
                                        viewModel.proceedToNext()
                                        val nextBlock = viewModel.getCurrentVerticalBlock()
                                        nextBlock?.let {
                                            viewModel.finishVerticalNextClickedEvent(
                                                it.blockId,
                                                it.displayName
                                            )
                                            if (it.type.isContainer()) {
                                                router.replaceCourseContainer(
                                                    requireActivity().supportFragmentManager,
                                                    it.id,
                                                    viewModel.courseId,
                                                    requireArguments().getString(
                                                        ARG_COURSE_NAME,
                                                        ""
                                                    ),
                                                    requireArguments().serializable(ARG_MODE)!!
                                                )
                                            }
                                        }
                                    }

                                    override fun onDismiss() {
                                        viewModel.finishVerticalBackClickedEvent()
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
                    defaultRadius = 3.dp,
                    selectedLength = 5.dp,
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

    private fun restrictDoubleClick(): Boolean {
        if (SystemClock.elapsedRealtime() - lastClickTime < 500) {
            return true
        }
        lastClickTime = SystemClock.elapsedRealtime()
        return false
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.viewPager.offscreenPageLimit = 1
        adapter = CourseUnitContainerAdapter(this, viewModel, viewModel.getUnitBlocks())
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
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