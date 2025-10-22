package org.openedx.core.domain.helper

import android.content.Context
import org.openedx.core.domain.model.Block
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.utils.VideoPreview

/**
 * Helper class for handling video preview generation.
 * This class encapsulates the logic for getting video previews from blocks,
 * avoiding the need to inject Context directly into ViewModels.
 */
class VideoPreviewHelper(
    private val context: Context,
    private val networkConnection: NetworkConnection
) {

    /**
     * Gets video preview for a single block
     * @param block The block to get video preview for
     * @param offlineUrl Optional offline URL for the video
     * @return VideoPreview object or null if no preview available
     */
    fun getVideoPreview(block: Block, offlineUrl: String? = null): VideoPreview? {
        return block.getVideoPreview(
            context = context,
            isOnline = networkConnection.isOnline(),
            offlineUrl = offlineUrl
        )
    }

    /**
     * Gets video previews for multiple blocks
     * @param blocks List of blocks to get video previews for
     * @param offlineUrls Optional map of block IDs to offline URLs
     * @return Map of block IDs to VideoPreview objects
     */
    fun getVideoPreviews(
        blocks: List<Block>,
        offlineUrls: Map<String, String>? = null
    ): Map<String, VideoPreview?> {
        return blocks.associate { block ->
            val offlineUrl = offlineUrls?.get(block.id)
            block.id to getVideoPreview(block, offlineUrl)
        }
    }

    /**
     * Gets video preview for a single block with a specific offline URL
     * @param blockId The ID of the block
     * @param block The block to get video preview for
     * @param offlineUrl Optional offline URL for the video
     * @return Pair of block ID and VideoPreview object or null
     */
    fun getVideoPreviewWithId(
        blockId: String,
        block: Block,
        offlineUrl: String? = null
    ): Pair<String, VideoPreview?> {
        return blockId to getVideoPreview(block, offlineUrl)
    }
}
