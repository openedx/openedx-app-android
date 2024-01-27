package org.openedx.course.presentation.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

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

enum class CourseContainerTab(val position: Int) {
    OUTLINE(0),
    VIDEOS(1),
    DISCUSSION(2),
    DATES(3),
    HANDOUTS(4),
}
