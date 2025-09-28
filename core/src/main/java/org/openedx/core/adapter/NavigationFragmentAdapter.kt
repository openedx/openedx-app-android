package org.openedx.core.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class NavigationFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragmentFactories = ArrayList<() -> Fragment>()

    override fun getItemCount(): Int = fragmentFactories.size

    override fun createFragment(position: Int): Fragment = fragmentFactories[position].invoke()

    fun addFragment(fragmentFactory: () -> Fragment) {
        fragmentFactories.add(fragmentFactory)
    }
}
