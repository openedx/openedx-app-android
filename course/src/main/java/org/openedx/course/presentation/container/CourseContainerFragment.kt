package org.openedx.course.presentation.container

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseContainerBinding
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.calendarsync.CalendarSyncDialog
import org.openedx.course.presentation.calendarsync.CalendarSyncDialogType
import org.openedx.course.presentation.container.CourseContainerTab
import org.openedx.course.presentation.dates.CourseDatesFragment
import org.openedx.course.presentation.handouts.HandoutsFragment
import org.openedx.course.presentation.outline.CourseOutlineFragment
import org.openedx.course.presentation.ui.CourseToolbar
import org.openedx.course.presentation.videos.CourseVideosFragment
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment
import org.openedx.course.presentation.container.CourseContainerTab as Tabs

class CourseContainerFragment : Fragment(R.layout.fragment_course_container) {

    private val binding by viewBinding(FragmentCourseContainerBinding::bind)
    private val viewModel by viewModel<CourseContainerViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TITLE, ""),
            requireArguments().getString(ARG_ENROLLMENT_MODE, "")
        )
    }
    private val router by inject<CourseRouter>()

    private var adapter: CourseContainerAdapter? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        viewModel.logCalendarPermissionAccess(!isGranted.containsValue(false))
        if (!isGranted.containsValue(false)) {
            viewModel.setCalendarSyncDialogType(CalendarSyncDialogType.SYNC_DIALOG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.preloadCourseStructure()
    }

    private var snackBar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(viewModel.courseName)
        if (viewModel.calendarSyncUIState.value.isCalendarSyncEnabled) {
            setUpCourseCalendar()
        }
        observe()
    }

    override fun onDestroyView() {
        snackBar?.dismiss()
        super.onDestroyView()
    }

    private fun observe() {
        viewModel.dataReady.observe(viewLifecycleOwner) { isReady ->
            if (isReady == true) {
                setupToolbar(viewModel.courseName)
                initViewPager()
            } else {
                router.navigateToNoAccess(
                    requireActivity().supportFragmentManager,
                    viewModel.courseName
                )
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            snackBar = Snackbar.make(binding.root, it, Snackbar.LENGTH_INDEFINITE)
                .setAction(org.openedx.core.R.string.core_error_try_again) {
                    viewModel.preloadCourseStructure()
                }
            snackBar?.show()

        }
        viewModel.showProgress.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
        }
    }

    private fun setupToolbar(courseName: String) {
        binding.toolbar.setContent {
            CourseToolbar(
                title = courseName,
                onBackClick = {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            )
        }
    }

    private fun initViewPager() {
        binding.viewPager.isVisible = true
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        adapter = CourseContainerAdapter(this).apply {
            addFragment(
                Tabs.COURSE,
                CourseOutlineFragment.newInstance(viewModel.courseId, viewModel.courseName)
            )
            addFragment(
                Tabs.VIDEOS,
                CourseVideosFragment.newInstance(viewModel.courseId, viewModel.courseName)
            )
            addFragment(
                Tabs.DISCUSSION,
                DiscussionTopicsFragment.newInstance(viewModel.courseId, viewModel.courseName)
            )
            addFragment(
                Tabs.DATES,
                CourseDatesFragment.newInstance(
                    viewModel.courseId,
                    viewModel.courseName,
                    viewModel.isSelfPaced,
                    viewModel.enrollmentMode,
                )
            )
            addFragment(
                Tabs.HANDOUTS,
                HandoutsFragment.newInstance(viewModel.courseId)
            )
        }
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.adapter = adapter

        if (viewModel.isCourseTopTabBarEnabled) {
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = getString(
                    Tabs.entries.find { it.ordinal == position }?.titleResId
                        ?: R.string.course_navigation_course
                )
            }.attach()
            binding.tabLayout.isVisible = true
            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        viewModel.courseContainerTabClickedEvent(Tabs.entries[it.position])
                    }
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {}

                override fun onTabReselected(p0: TabLayout.Tab?) {}
            })
        } else {
            binding.viewPager.isUserInputEnabled = false
            binding.bottomNavView.setOnItemSelectedListener { menuItem ->
                Tabs.entries.find { menuItem.itemId == it.itemId }?.let { tab ->
                    viewModel.courseContainerTabClickedEvent(tab)
                    binding.viewPager.setCurrentItem(tab.ordinal, false)
                }
                true
            }
            binding.bottomNavView.isVisible = true
            viewModel.courseContainerTabClickedEvent(Tabs.COURSE)
        }
        viewModel.courseContainerTabClickedEvent(Tabs.entries[binding.viewPager.currentItem])
    }

    private fun setUpCourseCalendar() {
        binding.composeContainer.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenEdXTheme {
                    val syncState by viewModel.calendarSyncUIState.collectAsState()

                    LaunchedEffect(key1 = syncState.checkForOutOfSync) {
                        if (syncState.isCalendarSyncEnabled && syncState.checkForOutOfSync.get()) {
                            viewModel.checkIfCalendarOutOfDate()
                        }
                    }

                    LaunchedEffect(syncState.uiMessage.get()) {
                        syncState.uiMessage.get().takeIfNotEmpty()?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            syncState.uiMessage.set("")
                        }
                    }

                    CalendarSyncDialog(
                        syncDialogType = syncState.dialogType,
                        calendarTitle = syncState.calendarTitle,
                        syncDialogPosAction = { dialog ->
                            when (dialog) {
                                CalendarSyncDialogType.SYNC_DIALOG -> {
                                    viewModel.logCalendarAddDates(true)
                                    viewModel.addOrUpdateEventsInCalendar(
                                        updatedEvent = false,
                                    )
                                }

                                CalendarSyncDialogType.UN_SYNC_DIALOG -> {
                                    viewModel.logCalendarRemoveDates(true)
                                    viewModel.deleteCourseCalendar()
                                }

                                CalendarSyncDialogType.PERMISSION_DIALOG -> {
                                    permissionLauncher.launch(viewModel.calendarPermissions)
                                }

                                CalendarSyncDialogType.OUT_OF_SYNC_DIALOG -> {
                                    viewModel.logCalendarSyncUpdate(true)
                                    viewModel.addOrUpdateEventsInCalendar(
                                        updatedEvent = true,
                                    )
                                }

                                CalendarSyncDialogType.EVENTS_DIALOG -> {
                                    viewModel.logCalendarSyncedConfirmation(true)
                                    viewModel.openCalendarApp()
                                }

                                else -> {}
                            }
                        },
                        syncDialogNegAction = { dialog ->
                            when (dialog) {
                                CalendarSyncDialogType.SYNC_DIALOG ->
                                    viewModel.logCalendarAddDates(false)

                                CalendarSyncDialogType.UN_SYNC_DIALOG ->
                                    viewModel.logCalendarRemoveDates(false)

                                CalendarSyncDialogType.OUT_OF_SYNC_DIALOG -> {
                                    viewModel.logCalendarSyncUpdate(false)
                                    viewModel.deleteCourseCalendar()
                                }

                                CalendarSyncDialogType.EVENTS_DIALOG ->
                                    viewModel.logCalendarSyncedConfirmation(false)

                                CalendarSyncDialogType.LOADING_DIALOG,
                                CalendarSyncDialogType.PERMISSION_DIALOG,
                                CalendarSyncDialogType.NONE,
                                -> {
                                }
                            }

                            viewModel.setCalendarSyncDialogType(CalendarSyncDialogType.NONE)
                        },
                        dismissSyncDialog = {
                            viewModel.setCalendarSyncDialogType(CalendarSyncDialogType.NONE)
                        }
                    )
                }
            }
        }
    }

    fun updateCourseStructure(withSwipeRefresh: Boolean) {
        viewModel.updateData(withSwipeRefresh)
    }

    fun updateCourseDates() {
        adapter?.getFragment(Tabs.DATES)?.let {
            (it as CourseDatesFragment).updateData()
        }
    }

    fun navigateToTab(tab: CourseContainerTab) {
        adapter?.getFragment(tab)?.let {
            binding.viewPager.setCurrentItem(tab.ordinal, true)
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        private const val ARG_ENROLLMENT_MODE = "enrollmentMode"
        fun newInstance(
            courseId: String,
            courseTitle: String,
            enrollmentMode: String,
        ): CourseContainerFragment {
            val fragment = CourseContainerFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to courseTitle,
                ARG_ENROLLMENT_MODE to enrollmentMode
            )
            return fragment
        }
    }
}

