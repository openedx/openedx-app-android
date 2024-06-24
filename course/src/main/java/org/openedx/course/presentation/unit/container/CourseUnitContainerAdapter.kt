package org.openedx.course.presentation.unit.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.openedx.core.FragmentViewType
import org.openedx.core.domain.model.Block
import org.openedx.course.presentation.unit.NotAvailableUnitFragment
import org.openedx.course.presentation.unit.NotAvailableUnitType
import org.openedx.course.presentation.unit.html.HtmlUnitFragment
import org.openedx.course.presentation.unit.video.VideoUnitFragment
import org.openedx.course.presentation.unit.video.YoutubeVideoUnitFragment
import org.openedx.discussion.presentation.threads.DiscussionThreadsFragment
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel
import java.io.File

class CourseUnitContainerAdapter(
    fragment: Fragment,
    val blocks: List<Block>,
    private val viewModel: CourseUnitContainerViewModel,
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = blocks.size

    override fun createFragment(position: Int): Fragment = unitBlockFragment(blocks[position])

    private fun unitBlockFragment(block: Block): Fragment {
        val downloadedModel = viewModel.getDownloadModelById(block.id)
        val offlineUrl = downloadedModel?.let { it.path + File.separator + "index.html" } ?: ""
        val noNetwork = !viewModel.hasNetworkConnection

        return when {
            noNetwork && block.isDownloadable && offlineUrl.isEmpty() -> {
                createNotAvailableUnitFragment(block, NotAvailableUnitType.NOT_DOWNLOADED)
            }

            noNetwork && !block.isDownloadable -> {
                createNotAvailableUnitFragment(block, NotAvailableUnitType.OFFLINE_UNSUPPORTED)
            }

            block.isVideoBlock && block.studentViewData?.encodedVideos?.run { hasVideoUrl || hasYoutubeUrl } == true -> {
                createVideoFragment(block)
            }

            block.isDiscussionBlock && !block.studentViewData?.topicId.isNullOrEmpty() -> {
                createDiscussionFragment(block)
            }

            !block.studentViewMultiDevice -> {
                createNotAvailableUnitFragment(block, NotAvailableUnitType.MOBILE_UNSUPPORTED)
            }

            block.isHTMLBlock || block.isProblemBlock || block.isOpenAssessmentBlock || block.isDragAndDropBlock ||
                    block.isWordCloudBlock || block.isLTIConsumerBlock || block.isSurveyBlock -> {
                val lastModified = if (downloadedModel != null && noNetwork) {
                    downloadedModel.lastModified ?: ""
                } else {
                    ""
                }
                HtmlUnitFragment.newInstance(
                    block.id,
                    block.studentViewUrl,
                    viewModel.courseId,
                    offlineUrl,
                    lastModified
                )
            }

            else -> {
                createNotAvailableUnitFragment(block, NotAvailableUnitType.MOBILE_UNSUPPORTED)
            }
        }
    }

    private fun createNotAvailableUnitFragment(block: Block, type: NotAvailableUnitType): Fragment {
        return NotAvailableUnitFragment.newInstance(block.id, block.lmsWebUrl, type)
    }

    private fun createVideoFragment(block: Block): Fragment {
        val encodedVideos = block.studentViewData!!.encodedVideos!!
        val transcripts = block.studentViewData!!.transcripts ?: emptyMap()
        val downloadedModel = viewModel.getDownloadModelById(block.id)
        val isDownloaded = downloadedModel != null
        val videoUrl = downloadedModel?.path ?: encodedVideos.videoUrl

        return if (videoUrl.isNotEmpty()) {
            VideoUnitFragment.newInstance(
                block.id,
                viewModel.courseId,
                videoUrl,
                transcripts,
                block.displayName,
                isDownloaded
            )
        } else {
            YoutubeVideoUnitFragment.newInstance(
                block.id,
                viewModel.courseId,
                encodedVideos.youtube?.url ?: "",
                transcripts,
                block.displayName
            )
        }
    }

    private fun createDiscussionFragment(block: Block): Fragment {
        return DiscussionThreadsFragment.newInstance(
            DiscussionTopicsViewModel.TOPIC,
            viewModel.courseId,
            block.studentViewData?.topicId ?: "",
            block.displayName,
            FragmentViewType.MAIN_CONTENT.name,
            block.id
        )
    }
}
