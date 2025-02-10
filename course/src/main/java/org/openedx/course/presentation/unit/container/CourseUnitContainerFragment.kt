package org.openedx.course.presentation.unit.container

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.global.InsetHolder
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseUnitContainerBinding
import org.openedx.course.presentation.ChapterEndFragmentDialog
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.DialogListener
import org.openedx.course.presentation.ui.CourseUnitToolbar
import org.openedx.course.presentation.ui.HorizontalPageIndicator
import org.openedx.course.presentation.ui.NavigationUnitsButtons
import org.openedx.course.presentation.ui.SubSectionUnitsList
import org.openedx.course.presentation.ui.SubSectionUnitsTitle
import org.openedx.course.presentation.ui.VerticalPageIndicator
import org.openedx.foundation.extension.serializable

class CourseUnitContainerFragment : Fragment(R.layout.fragment_course_unit_container) {

    private val binding: FragmentCourseUnitContainerBinding
        get() = _binding!!
    private var _binding: FragmentCourseUnitContainerBinding? = null

    private val viewModel by viewModel<CourseUnitContainerViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(UNIT_ID, "")
        )
    }

    private val router by inject<CourseRouter>()

    private var componentId: String = ""

    private lateinit var adapter: CourseUnitContainerAdapter

    private var lastClickTime = 0L

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            val blocks = viewModel.getUnitBlocks()
            blocks.getOrNull(position)?.let { currentBlock ->
                val encodedVideo = currentBlock.studentViewData?.encodedVideos
                binding.mediaRouteButton.isVisible = currentBlock.type == BlockType.VIDEO &&
                        encodedVideo?.hasNonYoutubeVideo == true &&
                        !encodedVideo.videoUrl.endsWith(".m3u8")
            }
        }
    }

    private val dialogListener = object : DialogListener {
        override fun <T> onClick(value: T) {
            viewModel.proceedToNext()
            val nextBlock = viewModel.getCurrentVerticalBlock()
            nextBlock?.let {
                viewModel.finishVerticalNextClickedEvent(
                    it.blockId,
                    it.displayName
                )
                if (it.type.isContainer()) {
                    router.navigateToCourseContainer(
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
            navigateToParentFragment()
        }
    }

    // Start workaround to fix an issue when onDestroy is not called after one fragment
    // was replaced with another using the 'FragmentManager.replace()' function
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateToParentFragment()
        }
    }

    private fun navigateToParentFragment() {
        activity?.supportFragmentManager?.let { fm ->
            for (i in fm.backStackEntryCount - 1 downTo 0) {
                val entryName = fm.getBackStackEntryAt(i).name
                if (entryName != CourseUnitContainerFragment::class.simpleName) {
                    fm.popBackStack(entryName, 0)
                    return
                }
            }
        }
    }
    // End workaround

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        componentId = requireArguments().getString(ARG_COMPONENT_ID, "")
        viewModel.loadBlocks(requireArguments().serializable(ARG_MODE)!!, componentId)
        viewModel.courseUnitContainerShowedEvent()
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

        setupViewPagerInsets()
        setupMediaRouteButton()
        initViewPager()
        handleSavedInstanceState(savedInstanceState)
        setupNavigationBar()
        setupProgressIndicators()
        setupBackButton()
        setupSubSectionUnits()
        checkUnitsListShown()
        setupChapterEndDialogListener()
    }

    private fun setupViewPagerInsets() {
        val insetHolder = requireActivity() as InsetHolder
        val containerParams = binding.viewPager.layoutParams as ConstraintLayout.LayoutParams
        containerParams.bottomMargin = insetHolder.bottomInset
        binding.viewPager.layoutParams = containerParams
    }

    private fun setupMediaRouteButton() {
        binding.mediaRouteButton.setAlwaysVisible(true)
        CastButtonFactory.setUpMediaRouteButton(requireContext(), binding.mediaRouteButton)
    }

    private fun handleSavedInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null && componentId.isEmpty()) {
            val currentBlockIndex = viewModel.getUnitBlocks().indexOfFirst {
                viewModel.getCurrentBlock().id == it.id
            }
            if (currentBlockIndex != -1) {
                binding.viewPager.currentItem = currentBlockIndex
            }
        }
        if (componentId.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                viewModel.indexInContainer.value?.let { index ->
                    binding.viewPager.setCurrentItem(index, true)
                }
            }
            requireArguments().putString(ARG_COMPONENT_ID, "")
            componentId = ""
        }
    }

    private fun setupNavigationBar() {
        binding.cvNavigationBar.setContent {
            NavigationBar()
        }
    }

    private fun setupProgressIndicators() {
        if (viewModel.isCourseUnitProgressEnabled) {
            binding.horizontalProgress.setContent {
                OpenEdXTheme {
                    val index by viewModel.indexInContainer.observeAsState(0)
                    val descendantsBlocks by viewModel.descendantsBlocks.collectAsState()

                    HorizontalPageIndicator(
                        blocks = descendantsBlocks,
                        selectedPage = index,
                        completedAndSelectedColor =
                        MaterialTheme.appColors.componentHorizontalProgressCompletedAndSelected,
                        completedColor = MaterialTheme.appColors.componentHorizontalProgressCompleted,
                        selectedColor = MaterialTheme.appColors.componentHorizontalProgressSelected,
                        defaultColor = MaterialTheme.appColors.componentHorizontalProgressDefault
                    )
                }
            }
            binding.horizontalProgress.isVisible = true
        } else {
            binding.cvCount.setContent {
                OpenEdXTheme {
                    val index by viewModel.indexInContainer.observeAsState(0)
                    val units by viewModel.verticalBlockCounts.observeAsState(0)

                    VerticalPageIndicator(
                        numberOfPages = units,
                        selectedColor = MaterialTheme.appColors.primary,
                        defaultColor = MaterialTheme.appColors.bottomSheetToggle,
                        selectedPage = index,
                        defaultRadius = 3.dp,
                        selectedLength = 5.dp,
                        modifier = Modifier.width(24.dp)
                    )
                }
            }
            binding.cvCount.isVisible = true
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setContent {
            val title = if (viewModel.isCourseExpandableSectionsEnabled) {
                val unitBlocks by viewModel.subSectionUnitBlocks.collectAsState()
                unitBlocks.firstOrNull()?.let {
                    viewModel.getSubSectionBlock(it.id).displayName
                } ?: ""
            } else {
                val index by viewModel.indexInContainer.observeAsState(0)
                val descendantsBlocks by viewModel.descendantsBlocks.collectAsState()
                descendantsBlocks.getOrNull(index)?.displayName ?: ""
            }

            CourseUnitToolbar(
                title = title,
                onBackClick = { navigateToParentFragment() }
            )
        }
    }

    private fun setupSubSectionUnits() {
        if (viewModel.isCourseExpandableSectionsEnabled) {
            binding.subSectionUnitsTitle.setContent {
                val unitBlocks by viewModel.subSectionUnitBlocks.collectAsState()
                val currentUnit = unitBlocks.firstOrNull { it.id == viewModel.unitId }
                val unitName = currentUnit?.displayName ?: ""
                val unitsListShowed by viewModel.unitsListShowed.observeAsState(false)

                OpenEdXTheme {
                    SubSectionUnitsTitle(
                        unitName = unitName,
                        unitsCount = unitBlocks.size,
                        unitsListShowed = unitsListShowed,
                        onUnitsClick = { handleUnitsClick() }
                    )
                }
            }

            binding.subSectionUnitsBg.setOnClickListener { handleUnitsClick() }

            binding.subSectionUnitsList.setContent {
                val unitBlocks by viewModel.subSectionUnitBlocks.collectAsState()
                // If there is more than one unit in the section, show the list
                if (unitBlocks.size > 1) {
                    val selectedUnitIndex = unitBlocks.indexOfFirst { it.id == viewModel.unitId }
                    OpenEdXTheme {
                        SubSectionUnitsList(
                            unitBlocks = unitBlocks,
                            selectedUnitIndex = selectedUnitIndex
                        ) { index, unit ->
                            if (index != selectedUnitIndex) {
                                router.navigateToCourseContainer(
                                    fm = requireActivity().supportFragmentManager,
                                    courseId = viewModel.courseId,
                                    unitId = unit.id,
                                    mode = requireArguments().serializable(ARG_MODE)!!
                                )
                            } else {
                                handleUnitsClick()
                            }
                        }
                    }
                }
            }
        } else {
            binding.subSectionUnitsTitle.isGone = true
        }
    }

    private fun checkUnitsListShown() {
        if (viewModel.unitsListShowed.value == true) {
            handleUnitsClick()
        }
    }

    private fun setupChapterEndDialogListener() {
        val chapterEndDialogTag = ChapterEndFragmentDialog::class.simpleName
        (requireActivity().supportFragmentManager.findFragmentByTag(chapterEndDialogTag) as? ChapterEndFragmentDialog)
            ?.let { fragment ->
                fragment.listener = dialogListener
            }
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(onBackPressedCallback)
    }

    override fun onPause() {
        onBackPressedCallback.remove()
        super.onPause()
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
        if (!viewModel.isCourseUnitProgressEnabled) {
            binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        }
        binding.viewPager.offscreenPageLimit = 1
        adapter = CourseUnitContainerAdapter(this, viewModel.getUnitBlocks(), viewModel)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    private fun handlePrevClick(buttonChanged: (String, Boolean, Boolean) -> Unit) {
        if (!restrictDoubleClick()) {
            val block = viewModel.moveToPrevBlock()
            if (block != null) {
                viewModel.prevBlockClickedEvent(block.blockId, block.displayName)
                if (!block.type.isContainer()) {
                    val prevIndex = binding.viewPager.currentItem - 1
                    binding.viewPager.setCurrentItem(prevIndex, true)
                    updateNavigationButtons { next, hasPrev, hasNext ->
                        buttonChanged(next, hasPrev, hasNext)
                    }
                }
            }
        }
    }

    private fun handleNextClick(buttonChanged: (String, Boolean, Boolean) -> Unit) {
        if (!restrictDoubleClick()) {
            val block = viewModel.moveToNextBlock()
            if (block != null) {
                viewModel.nextBlockClickedEvent(block.blockId, block.displayName)
                if (!block.type.isContainer()) {
                    val nextIndex = binding.viewPager.currentItem + 1
                    binding.viewPager.setCurrentItem(nextIndex, true)
                    updateNavigationButtons { next, hasPrev, hasNext ->
                        buttonChanged(next, hasPrev, hasNext)
                    }
                }
            } else {
                val currentVerticalBlock = viewModel.getCurrentVerticalBlock()
                val nextVerticalBlock = viewModel.getNextVerticalBlock()
                val dialog = ChapterEndFragmentDialog.newInstance(
                    currentVerticalBlock?.displayName ?: "",
                    nextVerticalBlock?.displayName ?: "",
                    !viewModel.isCourseUnitProgressEnabled
                )
                currentVerticalBlock?.let {
                    viewModel.finishVerticalClickedEvent(
                        it.blockId,
                        it.displayName
                    )
                }
                dialog.listener = dialogListener
                dialog.show(
                    requireActivity().supportFragmentManager,
                    ChapterEndFragmentDialog::class.simpleName
                )
            }
        }
    }

    private fun handleUnitsClick() {
        if (binding.subSectionUnitsList.visibility == View.VISIBLE) {
            binding.subSectionUnitsList.visibility = View.GONE
            binding.subSectionUnitsBg.visibility = View.GONE
            viewModel.setUnitsListVisibility(false)
        } else {
            binding.subSectionUnitsList.visibility = View.VISIBLE
            binding.subSectionUnitsBg.visibility = View.VISIBLE
            viewModel.setUnitsListVisibility(true)
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

            NavigationUnitsButtons(
                hasPrevBlock = hasPrevBlock,
                nextButtonText = nextButtonText,
                hasNextBlock = hasNextBlock,
                isVerticalNavigation = !viewModel.isCourseUnitProgressEnabled,
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
