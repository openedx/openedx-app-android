package org.openedx.core.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class NavigationFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragments = ArrayList<Fragment>()

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }
}
