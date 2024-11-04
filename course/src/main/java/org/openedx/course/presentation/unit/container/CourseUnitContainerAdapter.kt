package org.openedx.course.presentation.unit.container

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.openedx.core.FragmentViewType
import org.openedx.core.domain.model.Block
import org.openedx.core.module.db.DownloadModel
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
            isBlockNotDownloaded(block, noNetwork, offlineUrl) -> {
                createNotAvailableUnitFragment(block, NotAvailableUnitType.NOT_DOWNLOADED)
            }

            isBlockOfflineUnsupported(block, noNetwork) -> {
                createNotAvailableUnitFragment(block, NotAvailableUnitType.OFFLINE_UNSUPPORTED)
            }

            isVideoBlockAvailable(block) -> {
                createVideoFragment(block)
            }

            isDiscussionBlockAvailable(block) -> {
                createDiscussionFragment(block)
            }

            isSupportedHtmlBlock(block) -> {
                createHtmlUnitFragment(block, downloadedModel, noNetwork, offlineUrl)
            }

            else -> {
                createNotAvailableUnitFragment(block, NotAvailableUnitType.MOBILE_UNSUPPORTED)
            }
        }
    }

    private fun isBlockNotDownloaded(block: Block, noNetwork: Boolean, offlineUrl: String): Boolean {
        return noNetwork && block.isDownloadable && offlineUrl.isEmpty()
    }

    private fun isBlockOfflineUnsupported(block: Block, noNetwork: Boolean): Boolean {
        return noNetwork && !block.isDownloadable
    }

    private fun isVideoBlockAvailable(block: Block): Boolean {
        val encodedVideos = block.studentViewData?.encodedVideos
        val hasVideo = encodedVideos?.hasVideoUrl == true || encodedVideos?.hasYoutubeUrl == true
        return block.isVideoBlock && hasVideo
    }

    private fun isDiscussionBlockAvailable(block: Block): Boolean {
        val topicId = block.studentViewData?.topicId
        return block.isDiscussionBlock && !topicId.isNullOrEmpty()
    }

    private fun isSupportedHtmlBlock(block: Block): Boolean {
        return block.isHTMLBlock ||
                block.isProblemBlock ||
                block.isOpenAssessmentBlock ||
                block.isDragAndDropBlock ||
                block.isWordCloudBlock ||
                block.isLTIConsumerBlock ||
                block.isSurveyBlock
    }

    private fun createHtmlUnitFragment(
        block: Block,
        downloadedModel: DownloadModel?,
        noNetwork: Boolean,
        offlineUrl: String
    ): Fragment {
        val lastModified = if (downloadedModel != null && noNetwork) {
            downloadedModel.lastModified ?: ""
        } else {
            ""
        }
        return HtmlUnitFragment.newInstance(
            block.id,
            block.studentViewUrl,
            viewModel.courseId,
            offlineUrl,
            lastModified
        )
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
