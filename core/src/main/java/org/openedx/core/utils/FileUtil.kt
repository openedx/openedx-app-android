package org.openedx.core.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
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

    fun unzipFile(filepath: String): String? {
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

    private fun deleteFile(filepath: String?): Boolean {
        try {
            if (filepath != null) {
                val file = File(filepath)
                if (file.exists()) {
                    if (file.delete()) {
                        Log.d(this.javaClass.name, "Deleted: " + file.path)
                        return true
                    } else {
                        Log.d(this.javaClass.name, "Delete failed: " + file.path)
                    }
                } else {
                    Log.d(this.javaClass.name, "Delete failed, file does NOT exist: " + file.path)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}

enum class Directories {
    VIDEOS, SUBTITLES
}
