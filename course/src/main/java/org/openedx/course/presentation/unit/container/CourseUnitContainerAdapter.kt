package org.openedx.course.presentation.unit.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.openedx.core.BlockType
import org.openedx.core.FragmentViewType
import org.openedx.core.domain.model.Block
import org.openedx.course.presentation.unit.NotSupportedUnitFragment
import org.openedx.course.presentation.unit.html.HtmlUnitFragment
import org.openedx.course.presentation.unit.video.VideoUnitFragment
import org.openedx.course.presentation.unit.video.YoutubeVideoUnitFragment
import org.openedx.discussion.presentation.threads.DiscussionThreadsFragment
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment

class CourseUnitContainerAdapter(
    fragment: Fragment,
    val blocks: List<Block>,
    private val viewModel: CourseUnitContainerViewModel,
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = blocks.size

    override fun createFragment(position: Int): Fragment = unitBlockFragment(blocks[position])

    private fun unitBlockFragment(block: Block): Fragment {
        return when (block.type) {
            BlockType.HTML,
            BlockType.PROBLEM,
            BlockType.OPENASSESSMENT,
            BlockType.DRAG_AND_DROP_V2,
            BlockType.WORD_CLOUD,
            BlockType.LTI_CONSUMER,
            -> {
                HtmlUnitFragment.newInstance(block.id, block.studentViewUrl)
            }

            BlockType.VIDEO -> {
                val encodedVideos = block.studentViewData!!.encodedVideos!!
                val transcripts = block.studentViewData!!.transcripts
                with(encodedVideos) {
                    var isDownloaded = false
                    val videoUrl = if (viewModel.getDownloadModelById(block.id) != null) {
                        isDownloaded = true
                        viewModel.getDownloadModelById(block.id)!!.path
                    } else if (fallback != null) {
                        fallback!!.url
                    } else if (hls != null) {
                        hls!!.url
                    } else if (desktopMp4 != null) {
                        desktopMp4!!.url
                    } else if (mobileHigh != null) {
                        mobileHigh!!.url
                    } else if (mobileLow != null) {
                        mobileLow!!.url
                    } else {
                        ""
                    }
                    if (videoUrl.isNotEmpty()) {
                        VideoUnitFragment.newInstance(
                            block.id,
                            viewModel.courseId,
                            videoUrl,
                            transcripts?.toMap() ?: emptyMap(),
                            block.displayName,
                            isDownloaded
                        )
                    } else {
                        YoutubeVideoUnitFragment.newInstance(
                            block.id,
                            viewModel.courseId,
                            encodedVideos.youtube?.url!!,
                            transcripts?.toMap() ?: emptyMap(),
                            block.displayName
                        )
                    }
                }
            }

            BlockType.DISCUSSION -> {
                DiscussionThreadsFragment.newInstance(
                    DiscussionTopicsFragment.TOPIC,
                    viewModel.courseId,
                    block.studentViewData?.topicId ?: "",
                    block.displayName,
                    FragmentViewType.MAIN_CONTENT.name,
                    block.id
                )
            }

            else -> {
                NotSupportedUnitFragment.newInstance(
                    block.id,
                    block.lmsWebUrl
                )
            }
        }
    }
}