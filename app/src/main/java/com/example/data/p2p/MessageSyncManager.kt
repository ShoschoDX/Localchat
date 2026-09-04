package com.example.data.p2p

import android.util.Log
import com.example.data.database.LocalChatDatabase
import com.example.data.model.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessageSyncManager(
    private val database: LocalChatDatabase,
    private val connectionManager: P2PConnectionManager
) {
    companion object {
        private const val TAG = "MessageSyncManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val messageDao = database.messageDao()
    private val contactDao = database.contactDao()

    fun startObserving() {
        scope.launch {
            connectionManager.connectedDevices.collectLatest { connectedMap ->
                if (connectedMap.isNotEmpty()) {
                    for ((deviceId, device) in connectedMap) {
                        syncPendingMessagesForDevice(deviceId, device.ipAddress)
                    }
                }
            }
        }
    }

    /**
     * Finds any messages with WAITING_FOR_CONNECTION or SENDING for this peer
     * and synchronizes them directly over the active P2P link.
     */
    suspend fun syncPendingMessagesForDevice(deviceId: String, ipAddress: String?) {
        try {
            val contact = contactDao.getContactByDeviceId(deviceId) ?: return
            val pendingMessages = messageDao.getPendingMessagesForDevice(deviceId)
            if (pendingMessages.isEmpty()) return

            Log.d(TAG, "Syncing ${pendingMessages.size} pending messages to $deviceId")

            for (msg in pendingMessages) {
                connectionManager.sendChatMessage(
                    targetDeviceId = deviceId,
                    targetIp = ipAddress,
                    messageId = msg.id,
                    text = msg.text,
                    messageType = msg.messageType.name,
                    replyToId = msg.replyToMessageId,
                    replyToText = msg.replyToText,
                    attachmentName = msg.attachmentName,
                    attachmentSize = msg.attachmentSize
                ) { success ->
                    if (success) {
                        scope.launch {
                            messageDao.updateMessageStatus(msg.id, MessageStatus.DELIVERED)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending messages", e)
        }
    }
}
