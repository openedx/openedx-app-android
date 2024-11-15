package org.openedx.core.module.download

import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.utils.Sha1Util
import org.openedx.core.utils.unzipFile
import org.openedx.foundation.utils.FileUtil
import java.io.File

class DownloadHelper(
    private val preferencesManager: CorePreferences,
    private val fileUtil: FileUtil,
) {

    fun generateDownloadModelFromBlock(
        folder: String,
        block: Block,
        courseId: String
    ): DownloadModel? {
        return when (val downloadableType = block.downloadableType) {
            FileType.VIDEO -> {
                val videoInfo =
                    block.studentViewData?.encodedVideos?.getPreferredVideoInfoForDownloading(
                        preferencesManager.videoSettings.videoDownloadQuality
                    )
                val size = videoInfo?.fileSize ?: 0
                val url = videoInfo?.url ?: ""
                val extension = url.split('.').lastOrNull() ?: "mp4"
                val path =
                    folder + File.separator + "${Sha1Util.SHA1(url)}.$extension"
                DownloadModel(
                    block.id,
                    block.displayName,
                    courseId,
                    size,
                    path,
                    url,
                    downloadableType,
                    DownloadedState.WAITING,
                    null
                )
            }

            FileType.X_BLOCK -> {
                val url = if (block.downloadableType == FileType.X_BLOCK) {
                    block.offlineDownload?.fileUrl ?: ""
                } else {
                    ""
                }
                val size = block.offlineDownload?.fileSize ?: 0
                val extension = "zip"
                val path =
                    folder + File.separator + "${Sha1Util.SHA1(url)}.$extension"
                val lastModified = block.offlineDownload?.lastModified
                DownloadModel(
                    block.id,
                    block.displayName,
                    courseId,
                    size,
                    path,
                    url,
                    downloadableType,
                    DownloadedState.WAITING,
                    lastModified
                )
            }

            null -> null
        }
    }

    suspend fun updateDownloadStatus(downloadModel: DownloadModel): DownloadModel? {
        return when (downloadModel.type) {
            FileType.VIDEO -> {
                downloadModel.copy(
                    downloadedState = DownloadedState.DOWNLOADED,
                    size = File(downloadModel.path).length()
                )
            }

            FileType.X_BLOCK -> {
                val unzippedFolderPath = fileUtil.unzipFile(downloadModel.path) ?: return null
                downloadModel.copy(
                    downloadedState = DownloadedState.DOWNLOADED,
                    size = calculateDirectorySize(File(unzippedFolderPath)),
                    path = unzippedFolderPath
                )
            }
        }
    }

    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0

        return directory.listFiles()?.sumOf { file ->
            if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        } ?: 0
    }
}
