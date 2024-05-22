package org.openedx.core.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class FileUtil(val context: Context) {

    fun getExternalAppDir(): File {
        val dir = context.externalCacheDir.toString() + File.separator +
                context.getString(org.openedx.core.R.string.app_name).replace(Regex("\\s"), "_")
        val file = File(dir)
        file.mkdirs()
        return file
    }

    inline fun < reified T> saveObjectToFile(obj: T, fileName: String = "${T::class.java.simpleName}.json") {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val jsonString = gson.toJson(obj)
        File(getExternalAppDir().path + fileName).writeText(jsonString)
    }

    inline fun <reified T> getObjectFromFile(fileName: String = "${T::class.java.simpleName}.json"): T? {
        val file = File(getExternalAppDir().path + fileName)
        return if (file.exists()) {
            val gson: Gson = GsonBuilder().setPrettyPrinting().create()
            val jsonString = file.readText()
            gson.fromJson(jsonString, T::class.java)
        } else {
            null
        }
    }
}

enum class Directories {
    VIDEOS, SUBTITLES
}
