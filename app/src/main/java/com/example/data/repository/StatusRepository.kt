package com.example.data.repository

import com.example.data.database.LocalChatDatabase
import com.example.data.model.Status
import com.example.data.model.StatusType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class StatusRepository(private val database: LocalChatDatabase) {
    private val statusDao = database.statusDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            statusDao.purgeExpiredStatuses()
        }
    }

    val activeStatuses: Flow<List<Status>> = statusDao.getActiveStatuses()

    suspend fun createTextStatus(text: String, bgColorHex: String): Long {
        val status = Status(
            authorName = "My Status",
            type = StatusType.TEXT,
            content = text,
            caption = "",
            backgroundColorHex = bgColorHex,
            isMine = true,
            timestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        )
        return statusDao.insertStatus(status)
    }

    suspend fun createMediaStatus(mediaPath: String, caption: String, isVideo: Boolean): Long {
        val status = Status(
            authorName = "My Status",
            type = if (isVideo) StatusType.VIDEO else StatusType.IMAGE,
            content = mediaPath,
            caption = caption,
            isMine = true,
            timestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        )
        return statusDao.insertStatus(status)
    }

    suspend fun markViewed(statusId: Long) {
        statusDao.markStatusViewed(statusId)
    }

    suspend fun deleteStatus(statusId: Long) {
        statusDao.deleteStatus(statusId)
    }

    suspend fun purgeExpired() {
        statusDao.purgeExpiredStatuses()
    }
}
