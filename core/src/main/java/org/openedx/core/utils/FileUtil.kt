package org.openedx.core.utils

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.openedx.foundation.utils.FileUtil
import java.io.File

fun FileUtil.unzipFile(filepath: String): String? {
    val archive = File(filepath)
    val destinationFolder = File(
        archive.parentFile.absolutePath + "/" + archive.name + "-unzipped"
    )
    try {
        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs()
        }
        val zip = ZipFile(archive)
        zip.extractAll(destinationFolder.absolutePath)
        deleteFile(archive.absolutePath)
        return destinationFolder.absolutePath
    } catch (e: ZipException) {
        e.printStackTrace()
        deleteFile(destinationFolder.absolutePath)
    }
    return null
}

enum class Directories {
    VIDEOS, SUBTITLES
}
