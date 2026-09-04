package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.LocalChatDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(
    private val context: Context,
    private val database: LocalChatDatabase
) {
    suspend fun createBackup(targetUri: Uri? = null, passwordProtection: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("local_chat.db")
            val mediaDir = File(context.filesDir, "LocalChat/Media")
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFileName = "LocalChat_Backup_$timeStamp.zip"

            val backupDir = File(context.filesDir, "LocalChat/Backups").apply { mkdirs() }
            val tempZipFile = File(backupDir, backupFileName)

            ZipOutputStream(FileOutputStream(tempZipFile)).use { zos ->
                // Add database
                if (dbFile.exists()) {
                    zos.putNextEntry(ZipEntry("database/local_chat.db"))
                    FileInputStream(dbFile).use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                // Add media directory files
                if (mediaDir.exists()) {
                    mediaDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relativePath = file.relativeTo(mediaDir).path
                        zos.putNextEntry(ZipEntry("media/$relativePath"))
                        FileInputStream(file).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }

            // If targetUri provided from SAF, write to it
            if (targetUri != null) {
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    FileInputStream(tempZipFile).use { input ->
                        input.copyTo(output)
                    }
                }
            }

            Result.success("Backup successfully saved as $backupFileName (${tempZipFile.length() / 1024} KB)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(zipUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(zipUri)
                ?: return@withContext Result.failure(Exception("Cannot open backup file stream"))

            val dbFile = context.getDatabasePath("local_chat.db")
            val mediaDir = File(context.filesDir, "LocalChat/Media").apply { mkdirs() }

            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name.startsWith("database/") -> {
                            FileOutputStream(dbFile).use { out ->
                                zis.copyTo(out)
                            }
                        }
                        entry.name.startsWith("media/") -> {
                            val relativePath = entry.name.removePrefix("media/")
                            val targetFile = File(mediaDir, relativePath)
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { out ->
                                zis.copyTo(out)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success("Chats and local media successfully restored! Please restart or refresh chats.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
