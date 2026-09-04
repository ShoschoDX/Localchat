package com.example.data.repository

import com.example.data.database.LocalChatDatabase
import com.example.data.model.CallLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CallRepository(private val database: LocalChatDatabase) {
    private val callDao = database.callDao()

    val allCalls: Flow<List<CallLog>> = callDao.getAllCalls()

    suspend fun logCall(
        contactId: Long,
        contactName: String,
        avatarColorHex: String,
        isVideo: Boolean,
        isIncoming: Boolean,
        isMissed: Boolean,
        durationSeconds: Int = 0
    ) {
        val call = CallLog(
            contactId = contactId,
            contactName = contactName,
            avatarColorHex = avatarColorHex,
            isVideo = isVideo,
            isIncoming = isIncoming,
            isMissed = isMissed,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds
        )
        callDao.insertCall(call)
    }

    suspend fun clearCallLogs() {
        callDao.clearAllCalls()
    }
}
