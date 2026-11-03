package org.openedx.core.module.download

import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.instance.StorageNamespaceProvider
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
    private val namespaces: StorageNamespaceProvider,
) {

    /**
     * Files are named after their URL, and two sites can serve the same URL (a shared CDN, the
     * same video). Each instance therefore downloads into a folder of its own; the legacy
     * namespace keeps using [baseFolder] itself, where existing downloads already are.
     */
    internal fun instanceFolder(baseFolder: String): String {
        val segment = namespaces.current.fileSegment ?: return baseFolder
        val folder = File(File(baseFolder, INSTANCES_FOLDER), segment)
        folder.mkdirs()
        return folder.path
    }

    fun generateDownloadModelFromBlock(
        baseFolder: String,
        block: Block,
        courseId: String
    ): DownloadModel? {
        val folder = instanceFolder(baseFolder)
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

    private companion object {
        const val INSTANCES_FOLDER = "instances"
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
