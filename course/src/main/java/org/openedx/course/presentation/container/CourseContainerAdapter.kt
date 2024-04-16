package org.openedx.course.presentation.container

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.parcelize.Parcelize
import org.openedx.core.CourseContainerTabEntity
import org.openedx.course.R

class CourseContainerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragments = HashMap<CourseContainerTab, Fragment>()

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        val tab = CourseContainerTab.entries.find { it.ordinal == position }
        return fragments[tab] ?: throw IllegalStateException("Fragment not found for tab $tab")
    }

    fun addFragment(tab: CourseContainerTab, fragment: Fragment) {
        fragments[tab] = fragment
    }

    fun getFragment(tab: CourseContainerTab): Fragment? = fragments[tab]
}

@Parcelize
enum class CourseContainerTab(val itemId: Int, val titleResId: Int): Parcelable {
    COURSE(itemId = R.id.course, titleResId = R.string.course_navigation_course),
    VIDEOS(itemId = R.id.videos, titleResId = R.string.course_navigation_videos),
    DISCUSSION(itemId = R.id.discussions, titleResId = R.string.course_navigation_discussions),
    DATES(itemId = R.id.dates, titleResId = R.string.course_navigation_dates),
    MORE(itemId = R.id.resources, titleResId = R.string.course_navigation_more);

    companion object {
        fun fromEntity(courseContainerTabEntity: CourseContainerTabEntity): CourseContainerTab {
            return when (courseContainerTabEntity) {
                CourseContainerTabEntity.COURSE -> COURSE
                CourseContainerTabEntity.VIDEOS -> VIDEOS
                CourseContainerTabEntity.DISCUSSION -> DISCUSSION
                CourseContainerTabEntity.DATES -> DATES
                CourseContainerTabEntity.MORE -> MORE
            }
        }
    }
}
