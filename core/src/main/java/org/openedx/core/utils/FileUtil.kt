package org.openedx.core.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.util.Collections

class FileUtil(val context: Context) {

    fun getExternalAppDir(): File {
        val dir = context.externalCacheDir.toString() + File.separator +
                context.getString(org.openedx.core.R.string.app_name).replace(Regex("\\s"), "_")
        val file = File(dir)
        file.mkdirs()
        return file
    }

    inline fun <reified T> saveObjectToFile(
        obj: T,
        fileName: String = "${T::class.java.simpleName}.json",
    ) {
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

    /**
     * Deletes all the files and directories in the app's external storage directory.
     */
    fun deleteOldAppDirectory() {
        val externalFilesDir = context.getExternalFilesDir(null)
        val externalAppDir = File(externalFilesDir?.parentFile, Directories.VIDEOS.name)
        if (externalAppDir.isDirectory) {
            deleteRecursive(externalAppDir, Collections.emptyList())
        }
    }

    /**
     * Deletes a file or directory and all its content recursively.
     *
     * @param fileOrDirectory The file or directory that needs to be deleted.
     * @param exceptions      Names of the files or directories that need to be skipped while deletion.
     */
    private fun deleteRecursive(
        fileOrDirectory: File,
        exceptions: List<String>,
    ) {
        if (exceptions.contains(fileOrDirectory.name)) return

        if (fileOrDirectory.isDirectory) {
            val filesList = fileOrDirectory.listFiles()
            if (filesList != null) {
                for (child in filesList) {
                    deleteRecursive(child, exceptions)
                }
            }
        }

        // Don't break the recursion upon encountering an error
        // noinspection ResultOfMethodCallIgnored
        fileOrDirectory.delete()
    }
}

enum class Directories {
    VIDEOS, SUBTITLES
}
