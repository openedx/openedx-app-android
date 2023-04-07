package com.raccoongang.core.utils

import android.content.Context
import java.io.File

object FileUtil {

    fun getExternalAppDir(context: Context): File {
        val dir = context.externalCacheDir.toString() + File.separator +
                context.getString(com.raccoongang.core.R.string.app_name).replace(Regex("\\s"), "_")
        val file = File(dir)
        file.mkdirs()
        return file
    }


}

enum class Directories {
    VIDEOS, SUBTITLES
}
