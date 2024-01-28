package org.openedx.course.presentation.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.openedx.course.R

class CourseContainerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragments = HashMap<CourseContainerTab, Fragment>()

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        val tab = CourseContainerTab.values().find { it.position == position }
        return fragments[tab] ?: throw IllegalStateException("Fragment not found for tab $tab")
    }

    fun addFragment(tab: CourseContainerTab, fragment: Fragment) {
        fragments[tab] = fragment
    }

    fun getFragment(tab: CourseContainerTab): Fragment? = fragments[tab]
}

enum class CourseContainerTab(val itemId: Int, val position: Int, val titleResId: Int) {
    OUTLINE(itemId = R.id.outline, position = 0, titleResId = R.string.course_navigation_course),
    VIDEOS(itemId = R.id.videos, position = 1, titleResId = R.string.course_navigation_video),
    DISCUSSION(
        itemId = R.id.discussions,
        position = 2,
        titleResId = R.string.course_navigation_discussion
    ),
    DATES(itemId = R.id.dates, position = 3, titleResId = R.string.course_navigation_dates),
    HANDOUTS(
        itemId = R.id.resources,
        position = 4,
        titleResId = R.string.course_navigation_handouts
    ),
}
