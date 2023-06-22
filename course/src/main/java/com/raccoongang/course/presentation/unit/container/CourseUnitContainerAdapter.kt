package com.raccoongang.course.presentation.unit.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.raccoongang.core.BlockType
import com.raccoongang.core.FragmentViewType
import com.raccoongang.core.domain.model.Block
import com.raccoongang.course.presentation.unit.NotSupportedUnitFragment
import com.raccoongang.course.presentation.unit.html.HtmlUnitFragment
import com.raccoongang.course.presentation.unit.video.VideoUnitFragment
import com.raccoongang.course.presentation.unit.video.YoutubeVideoUnitFragment
import com.raccoongang.discussion.presentation.threads.DiscussionThreadsFragment
import com.raccoongang.discussion.presentation.topics.DiscussionTopicsFragment

class CourseUnitContainerAdapter(
    fragment: Fragment,
    private val viewModel: CourseUnitContainerViewModel,
    private var blocks: List<Block>
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