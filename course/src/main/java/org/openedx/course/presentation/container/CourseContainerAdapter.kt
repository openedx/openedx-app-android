package org.openedx.course.presentation.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CourseContainerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragments = ArrayList<Fragment>()

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }

    fun getFragment(position: Int): Fragment = fragments[position]
}