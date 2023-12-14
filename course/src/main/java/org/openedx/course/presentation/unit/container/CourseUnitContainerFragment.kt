package org.openedx.course.presentation.unit.container

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.BlockType
import org.openedx.core.extension.serializable
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.global.InsetHolder
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseUnitContainerBinding
import org.openedx.course.presentation.ChapterEndFragmentDialog
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.DialogListener
import org.openedx.course.presentation.ui.*


class CourseUnitContainerFragment : Fragment(R.layout.fragment_course_unit_container) {

    private val binding: FragmentCourseUnitContainerBinding
        get() = _binding!!
    private var _binding: FragmentCourseUnitContainerBinding? = null

    private val viewModel by viewModel<CourseUnitContainerViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }

    private val router by inject<CourseRouter>()

    private var unitId: String = ""
    private var componentId: String = ""

    private lateinit var adapter: CourseUnitContainerAdapter

    private var lastClickTime = 0L

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            val blocks = viewModel.getUnitBlocks()
            blocks.getOrNull(position)?.let { currentBlock ->
                val encodedVideo = currentBlock.studentViewData?.encodedVideos
                binding.mediaRouteButton.isVisible = currentBlock.type == BlockType.VIDEO
                        && encodedVideo?.hasNonYoutubeVideo == true
                        && !encodedVideo.videoUrl.endsWith(".m3u8")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        unitId = requireArguments().getString(UNIT_ID, "")
        componentId = requireArguments().getString(ARG_COMPONENT_ID, "")
        viewModel.loadBlocks(requireArguments().serializable(ARG_MODE)!!)
        viewModel.setupCurrentIndex(unitId, componentId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCourseUnitContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val insetHolder = requireActivity() as InsetHolder
        val containerParams = binding.viewPager.layoutParams as ConstraintLayout.LayoutParams
        containerParams.bottomMargin = insetHolder.bottomInset
        binding.viewPager.layoutParams = containerParams

        binding.mediaRouteButton.setAlwaysVisible(true)
        CastButtonFactory.setUpMediaRouteButton(requireContext(), binding.mediaRouteButton)

        initViewPager()
        if (savedInstanceState == null && componentId.isEmpty()) {
            val currentBlockIndex = viewModel.getUnitBlocks().indexOfFirst {
                viewModel.getCurrentBlock().id == it.id
            }
            if (currentBlockIndex != -1) {
                binding.viewPager.currentItem = currentBlockIndex
            }
        }
        if (componentId.isEmpty().not()) {
            lifecycleScope.launch(Dispatchers.Main) {
                viewModel.indexInContainer.value?.let { index ->
                    binding.viewPager.setCurrentItem(index, true)
                }
            }
            requireArguments().putString(ARG_COMPONENT_ID, "")
            componentId = ""
        }

        binding.cvNavigationBar.setContent {
            NavigationBar()
        }

        if (viewModel.isCourseUnitProgressEnabled) {
            binding.horizontalProgress.setContent {
                OpenEdXTheme {
                    val index by viewModel.indexInContainer.observeAsState(1)

                    HorizontalPageIndicator(
                        blocks = viewModel.descendantsBlocks,
                        selectedPage = index,
                        completedColor = Color(0xFF2EA171),
                        selectedColor = Color(0xFFF0CB00),
                        defaultColor = Color(0xFFD6D3D1)
                    )
                }
            }
            binding.horizontalProgress.isVisible = true

        } else {
            binding.cvCount.setContent {
                OpenEdXTheme {
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
                    )
                }
            }
            binding.cvCount.isVisible = true
        }

        binding.btnBack.setContent {
            val sectionsBlocks by viewModel.subSectionsBlocks.collectAsState()
            val currentSection = sectionsBlocks.firstOrNull { it.id == blockId }
            val title =
                if (currentSection == null) "" else viewModel.getModuleBlock(currentSection.id).displayName

            CourseUnitToolbar(
                title = title,
                onBackClick = {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            )
        }

        if (viewModel.isCourseExpandableSectionsEnabled) {
            binding.unitSubSectionsTitle.setContent {
                val subSectionsBlocks by viewModel.subSectionsBlocks.collectAsState()
                val currentSubSection = subSectionsBlocks.firstOrNull { it.id == blockId }
                val subSectionName = currentSubSection?.displayName ?: ""
                val blockShowed by viewModel.selectBlockDialogShowed.observeAsState()

                OpenEdXTheme {
                    UnitSubSectionsTitle(
                        subSectionName = subSectionName,
                        subSectionsCount = subSectionsBlocks.size,
                        blockListShowed = blockShowed,
                        onBlockClick = { handleSectionClick() }
                    )
                }
            }

            binding.subSectionsBlocksBg.setOnClickListener { handleSectionClick() }

            binding.subSectionsBlocksList.setContent {
                val sectionsBlocks by viewModel.subSectionsBlocks.collectAsState()
                val selectedIndex = sectionsBlocks.indexOfFirst { it.id == blockId }
                OpenEdXTheme {
                    UnitSubSectionsList(
                        sectionsBlocks = sectionsBlocks,
                        selectedSection = selectedIndex
                    ) { index, block ->
                        if (index != selectedIndex) {
                            router.replaceCourseContainer(
                                fm = requireActivity().supportFragmentManager,
                                courseId = viewModel.courseId,
                                unitId = block.id,
                                mode = requireArguments().serializable(ARG_MODE)!!
                            )

                        } else {
                            handleSectionClick()
                        }
                    }
                }
            }

        } else {
            binding.unitSubSectionsTitle.isGone = true
        }

        if (viewModel.selectBlockDialogShowed.value == true) handleSectionClick()
    }

    override fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
        _binding = null
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
        adapter = CourseUnitContainerAdapter(this, viewModel.getUnitBlocks(), viewModel)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    private fun handlePrevClick(
        prevIndex: Int = viewModel.currentIndex - 1,
        buttonChanged: (String, Boolean, Boolean) -> Unit
    ) {
        if (!restrictDoubleClick()) {
            val block = viewModel.moveToBlock(prevIndex)
            if (block != null) {
                viewModel.prevBlockClickedEvent(block.blockId, block.displayName)
                if (!block.type.isContainer()) {
                    binding.viewPager.setCurrentItem(
                        viewModel.currentIndex, true
                    )
                    updateNavigationButtons { next, hasPrev, hasNext ->
                        buttonChanged(next, hasPrev, hasNext)
                    }
                }
            }
        }
    }

    private fun handleNextClick(
        nextIndex: Int = viewModel.currentIndex + 1,
        buttonChanged: (String, Boolean, Boolean) -> Unit
    ) {
        if (!restrictDoubleClick()) {
            val block = viewModel.moveToBlock(nextIndex)
            if (block != null) {
                viewModel.nextBlockClickedEvent(block.blockId, block.displayName)
                if (!block.type.isContainer()) {
                    binding.viewPager.setCurrentItem(
                        viewModel.currentIndex, true
                    )
                    updateNavigationButtons { next, hasPrev, hasNext ->
                        buttonChanged(next, hasPrev, hasNext)
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
                                    fm = requireActivity().supportFragmentManager,
                                    courseId = viewModel.courseId,
                                    unitId = it.id,
                                    mode = requireArguments().serializable(ARG_MODE)!!
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

    private fun handleSectionClick() {
        if (binding.subSectionsBlocksList.visibility == View.VISIBLE) {
            binding.subSectionsBlocksList.visibility = View.GONE
            binding.subSectionsBlocksBg.visibility = View.GONE
            viewModel.hideSelectBlockDialog()

        } else {
            binding.subSectionsBlocksList.visibility = View.VISIBLE
            binding.subSectionsBlocksBg.visibility = View.VISIBLE
            viewModel.showSelectBlockDialog()
        }
    }

    private fun proceedToNextSection(nextBlock: Block) {
        if (nextBlock.type.isContainer()) {
            router.replaceCourseContainer(
                requireActivity().supportFragmentManager,
                nextBlock.id,
                viewModel.courseId,
                requireArguments().serializable(ARG_MODE)!!
            )
        }
    }

    @Composable
    private fun NavigationBar() {
        OpenEdXTheme {
            var nextButtonText by rememberSaveable {
                mutableStateOf(viewModel.nextButtonText)
            }
            var hasNextBlock by rememberSaveable {
                mutableStateOf(viewModel.hasNextBlock)
            }
            var hasPrevBlock by rememberSaveable {
                mutableStateOf(viewModel.hasNextBlock)
            }

            updateNavigationButtons { next, hasPrev, hasNext ->
                nextButtonText = next
                hasPrevBlock = hasPrev
                hasNextBlock = hasNext
            }
            val windowSize = rememberWindowSize()

            NavigationUnitsButtons(
                windowSize = windowSize,
                hasPrevBlock = hasPrevBlock,
                nextButtonText = nextButtonText,
                hasNextBlock = hasNextBlock,
                onPrevClick = {
                    handlePrevClick { next, hasPrev, hasNext ->
                        nextButtonText = next
                        hasPrevBlock = hasPrev
                        hasNextBlock = hasNext
                    }
                },
                onNextClick = {
                    handleNextClick { next, hasPrev, hasNext ->
                        nextButtonText = next
                        hasPrevBlock = hasPrev
                        hasNextBlock = hasNext
                    }
                }
            )
        }
    }

    companion object {

        private const val ARG_COURSE_ID = "courseId"
        private const val UNIT_ID = "unitId"
        private const val ARG_COMPONENT_ID = "componentId"
        private const val ARG_MODE = "mode"

        fun newInstance(
            courseId: String,
            unitId: String,
            componentId: String?,
            mode: CourseViewMode,
        ): CourseUnitContainerFragment {
            val fragment = CourseUnitContainerFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                UNIT_ID to unitId,
                ARG_COMPONENT_ID to componentId,
                ARG_MODE to mode
            )
            return fragment
        }
    }
}
