package org.openedx.course.presentation.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.openedx.course.R

class CourseContainerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragments = HashMap<CourseContainerTab, Fragment>()

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        val tab = CourseContainerTab.values().find { it.ordinal == position }
        return fragments[tab] ?: throw IllegalStateException("Fragment not found for tab $tab")
    }

    fun addFragment(tab: CourseContainerTab, fragment: Fragment) {
        fragments[tab] = fragment
    }

    fun getFragment(tab: CourseContainerTab): Fragment? = fragments[tab]
}

enum class CourseContainerTab(val itemId: Int, val titleResId: Int) {
    COURSE(itemId = R.id.course, titleResId = R.string.course_navigation_course),
    VIDEOS(itemId = R.id.videos, titleResId = R.string.course_navigation_videos),
    DISCUSSION(itemId = R.id.discussions, titleResId = R.string.course_navigation_discussions),
    DATES(itemId = R.id.dates, titleResId = R.string.course_navigation_dates),
    MORE(itemId = R.id.resources, titleResId = R.string.course_navigation_more),
}
