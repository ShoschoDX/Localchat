package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStorageHelper(private val context: Context) {

    private val mediaBaseDir: File by lazy {
        File(context.filesDir, "LocalChat/Media").apply { mkdirs() }
    }

    val imagesDir: File by lazy { File(mediaBaseDir, "Images").apply { mkdirs() } }
    val audioDir: File by lazy { File(mediaBaseDir, "Audio").apply { mkdirs() } }
    val videosDir: File by lazy { File(mediaBaseDir, "Videos").apply { mkdirs() } }
    val documentsDir: File by lazy { File(mediaBaseDir, "Documents").apply { mkdirs() } }
    val stickersDir: File by lazy { File(mediaBaseDir, "Stickers").apply { mkdirs() } }
    val backupsDir: File by lazy { File(context.filesDir, "LocalChat/Backups").apply { mkdirs() } }

    fun saveBitmap(bitmap: Bitmap, prefix: String = "IMG"): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(imagesDir, "${prefix}_${timeStamp}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file.absolutePath
    }

    fun copyUriToLocal(uri: Uri, targetDir: File, defaultName: String = "file"): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(targetDir, "${timeStamp}_$defaultName")
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    fun createAudioRecordFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(audioDir, "AUD_${timeStamp}.m4a")
    }

    fun getTotalStorageUsedBytes(): Long {
        val baseDir = File(context.filesDir, "LocalChat")
        return calculateDirSize(baseDir)
    }

    fun clearTempCache(): Boolean {
        return context.cacheDir.deleteRecursively()
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var bytes = 0L
        dir.listFiles()?.forEach { file ->
            bytes += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return bytes
    }
}
