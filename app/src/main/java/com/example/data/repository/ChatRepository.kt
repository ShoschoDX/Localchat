package com.example.data.repository

import android.content.Context
import com.example.data.database.LocalChatDatabase
import com.example.data.model.AppSettings
import com.example.data.model.Chat
import com.example.data.model.ChatWithContact
import com.example.data.model.Contact
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.PairedDevice
import com.example.data.p2p.FileTransferManager
import com.example.data.p2p.MessageSyncManager
import com.example.data.p2p.P2PConnectionManager
import com.example.data.p2p.P2PIdentityManager
import com.example.data.p2p.P2PMessageListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class ChatRepository(
    private val database: LocalChatDatabase,
    private val context: Context
) {
    private val contactDao = database.contactDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val settingsDao = database.settingsDao()
    private val deviceDao = database.deviceDao()
    private val repoScope = CoroutineScope(Dispatchers.IO)

    val identityManager = P2PIdentityManager(context)
    val p2pManager = P2PConnectionManager(context, identityManager)
    val fileTransferManager = FileTransferManager(context, database, p2pManager)
    val messageSyncManager = MessageSyncManager(database, p2pManager)

    init {
        repoScope.launch {
            initCleanSettingsIfEmpty()
        }

        p2pManager.setMessageListener(object : P2PMessageListener {
            override fun onChatMessageReceived(
                messageId: Long,
                senderDeviceId: String,
                receiverDeviceId: String,
                text: String,
                messageType: String,
                timestamp: Long,
                replyToId: Long?,
                replyToText: String?,
                attachmentName: String?,
                attachmentSize: String?,
                attachmentBase64: String?
            ) {
                repoScope.launch {
                    handleIncomingChatMessage(
                        messageId = messageId,
                        senderDeviceId = senderDeviceId,
                        text = text,
                        messageType = messageType,
                        timestamp = timestamp,
                        replyToId = replyToId,
                        replyToText = replyToText,
                        attachmentName = attachmentName,
                        attachmentSize = attachmentSize,
                        attachmentBase64 = attachmentBase64
                    )
                }
            }

            override fun onMessageAckReceived(messageId: Long, status: String, senderDeviceId: String) {
                repoScope.launch {
                    val messageStatus = if (status == "READ") MessageStatus.READ else MessageStatus.DELIVERED
                    messageDao.updateMessageStatus(messageId, messageStatus)
                }
            }

            override fun onDevicePaired(
                deviceId: String,
                name: String,
                ip: String,
                port: Int,
                about: String,
                publicKey: String
            ) {
                repoScope.launch {
                    savePairedDeviceAndContact(deviceId, name, ip, port, about, publicKey)
                }
            }
        })

        messageSyncManager.startObserving()
    }

    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()

    val chatsWithContacts: Flow<List<ChatWithContact>> = combine(
        chatDao.getAllChats(),
        contactDao.getAllContacts()
    ) { chats, contacts ->
        val contactMap = contacts.associateBy { it.id }
        chats.mapNotNull { chat ->
            contactMap[chat.contactId]?.let { contact ->
                ChatWithContact(chat, contact)
            }
        }
    }

    fun getContact(contactId: Long): Flow<Contact?> = contactDao.getContactByIdFlow(contactId)

    fun getMessages(chatId: Long): Flow<List<Message>> = messageDao.getMessagesForChat(chatId)

    fun getStarredMessages(): Flow<List<Message>> = messageDao.getStarredMessages()

    fun searchMessages(query: String): Flow<List<Message>> = messageDao.searchMessages(query)

    fun getMediaMessages(chatId: Long): Flow<List<Message>> = messageDao.getMediaForChat(chatId)

    val settings: Flow<AppSettings?> = settingsDao.getSettings()

    suspend fun markChatRead(chatId: Long) {
        chatDao.markChatAsRead(chatId)
        // Optionally send READ ACK to peer
        repoScope.launch {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            val contact = contactDao.getContactById(chat.contactId) ?: return@launch
            if (contact.deviceId.isNotEmpty()) {
                // Send read signal
            }
        }
    }

    suspend fun togglePinChat(chatId: Long, isPinned: Boolean) {
        chatDao.setChatPinned(chatId, isPinned)
    }

    suspend fun toggleMuteChat(chatId: Long, isMuted: Boolean) {
        chatDao.setChatMuted(chatId, isMuted)
    }

    suspend fun deleteChat(chatId: Long) {
        messageDao.clearChatMessages(chatId)
        chatDao.deleteChat(chatId)
    }

    suspend fun clearChatHistory(chatId: Long) {
        messageDao.clearChatMessages(chatId)
        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            chatDao.updateChat(chat.copy(lastMessageText = "", unreadCount = 0))
        }
    }

    suspend fun getOrCreateChatForContact(contactId: Long): Long {
        val existing = chatDao.getChatByContactId(contactId)
        if (existing != null) return existing.id

        val newChat = Chat(
            contactId = contactId,
            lastMessageText = "Direct P2P Chat",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        return chatDao.insertChat(newChat)
    }

    /**
     * Sends a real P2P message directly to the contact's device.
     * If the peer device is currently unreachable, the message is stored locally
     * with status WAITING_FOR_CONNECTION and will automatically sync when they reconnect.
     */
    suspend fun sendMessage(
        chatId: Long,
        text: String,
        type: MessageType = MessageType.TEXT,
        attachmentPath: String? = null,
        attachmentName: String? = null,
        attachmentSize: String? = null,
        replyToMessageId: Long? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ) {
        val chat = chatDao.getChatById(chatId) ?: return
        val contact = contactDao.getContactById(chat.contactId) ?: return

        val isConnected = contact.deviceId.isNotEmpty() &&
                p2pManager.connectedDevices.value.containsKey(contact.deviceId)

        val initialStatus = if (isConnected) MessageStatus.SENDING else MessageStatus.WAITING_FOR_CONNECTION

        val message = Message(
            chatId = chatId,
            senderId = "ME",
            senderDeviceId = p2pManager.localDeviceId,
            receiverDeviceId = contact.deviceId,
            text = text,
            messageType = type,
            timestamp = System.currentTimeMillis(),
            status = initialStatus,
            replyToMessageId = replyToMessageId,
            replyToText = replyToText,
            replyToSender = replyToSender,
            attachmentPath = attachmentPath,
            attachmentName = attachmentName,
            attachmentSize = attachmentSize
        )
        val msgId = messageDao.insertMessage(message)

        val previewText = when (type) {
            MessageType.IMAGE -> "📷 Photo"
            MessageType.VIDEO -> "🎥 Video"
            MessageType.AUDIO -> "🎤 Voice note"
            MessageType.DOCUMENT -> "📄 ${attachmentName ?: "Document"}"
            MessageType.STICKER -> "🎨 Sticker"
            MessageType.TEXT -> text
        }
        chatDao.updateChat(
            chat.copy(
                lastMessageText = previewText,
                lastMessageTimestamp = System.currentTimeMillis()
            )
        )

        // If this is a media file, use FileTransferManager
        if (attachmentPath != null && File(attachmentPath).exists()) {
            fileTransferManager.sendFile(
                targetDeviceId = contact.deviceId,
                targetIp = contact.ipAddress,
                messageId = msgId,
                file = File(attachmentPath),
                messageType = type.name
            ) { success, _ ->
                repoScope.launch {
                    val status = if (success) MessageStatus.SENT else MessageStatus.WAITING_FOR_CONNECTION
                    messageDao.updateMessageStatus(msgId, status)
                }
            }
        } else if (contact.deviceId.isNotEmpty()) {
            // Direct P2P text message transmission
            p2pManager.sendChatMessage(
                targetDeviceId = contact.deviceId,
                targetIp = contact.ipAddress,
                messageId = msgId,
                text = text,
                messageType = type.name,
                replyToId = replyToMessageId,
                replyToText = replyToText
            ) { success ->
                repoScope.launch {
                    val status = if (success) MessageStatus.SENT else MessageStatus.WAITING_FOR_CONNECTION
                    messageDao.updateMessageStatus(msgId, status)
                }
            }
        }
    }

    private suspend fun handleIncomingChatMessage(
        messageId: Long,
        senderDeviceId: String,
        text: String,
        messageType: String,
        timestamp: Long,
        replyToId: Long?,
        replyToText: String?,
        attachmentName: String?,
        attachmentSize: String?,
        attachmentBase64: String?
    ) {
        // Resolve contact by deviceId or create contact
        var contact = contactDao.getContactByDeviceId(senderDeviceId)
        if (contact == null) {
            val newContact = Contact(
                deviceId = senderDeviceId,
                name = "User ($senderDeviceId)",
                about = "Available on Local Chat",
                avatarColorHex = getRandomColorHex(),
                isOnline = true
            )
            val cId = contactDao.insertContact(newContact)
            contact = contactDao.getContactById(cId) ?: return
        }

        val chatId = getOrCreateChatForContact(contact.id)

        // Save incoming attachment if present
        var localAttachmentPath: String? = null
        if (!attachmentBase64.isNullOrBlank() && !attachmentName.isNullOrBlank()) {
            val savedFile = fileTransferManager.saveReceivedFile(attachmentName, attachmentBase64)
            localAttachmentPath = savedFile?.absolutePath
        }

        val type = try {
            MessageType.valueOf(messageType)
        } catch (_: Exception) {
            MessageType.TEXT
        }

        val incomingMessage = Message(
            chatId = chatId,
            senderId = senderDeviceId,
            senderDeviceId = senderDeviceId,
            receiverDeviceId = p2pManager.localDeviceId,
            text = text,
            messageType = type,
            timestamp = timestamp,
            status = MessageStatus.DELIVERED,
            replyToMessageId = replyToId,
            replyToText = replyToText,
            replyToSender = contact.name,
            attachmentPath = localAttachmentPath,
            attachmentName = attachmentName,
            attachmentSize = attachmentSize
        )
        messageDao.insertMessage(incomingMessage)

        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            val preview = when (type) {
                MessageType.IMAGE -> "📷 Photo"
                MessageType.VIDEO -> "🎥 Video"
                MessageType.AUDIO -> "🎤 Voice note"
                MessageType.DOCUMENT -> "📄 ${attachmentName ?: "Document"}"
                MessageType.STICKER -> "🎨 Sticker"
                MessageType.TEXT -> text
            }
            chatDao.updateChat(
                chat.copy(
                    lastMessageText = preview,
                    lastMessageTimestamp = timestamp,
                    unreadCount = chat.unreadCount + 1
                )
            )
        }
    }

    suspend fun savePairedDeviceAndContact(
        deviceId: String,
        name: String,
        ip: String,
        port: Int,
        about: String,
        publicKey: String
    ): Long {
        deviceDao.insertDevice(
            PairedDevice(
                deviceId = deviceId,
                name = name,
                ipAddress = ip,
                port = port,
                publicKey = publicKey,
                isTrusted = true,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        )

        val existing = contactDao.getContactByDeviceId(deviceId)
        val contactId = if (existing != null) {
            contactDao.updateContact(
                existing.copy(
                    name = name.ifBlank { existing.name },
                    ipAddress = ip,
                    port = port,
                    about = about.ifBlank { existing.about },
                    isOnline = true
                )
            )
            existing.id
        } else {
            contactDao.insertContact(
                Contact(
                    deviceId = deviceId,
                    name = name.ifBlank { "User ($deviceId)" },
                    ipAddress = ip,
                    port = port,
                    about = about.ifBlank { "Available on Local Chat" },
                    publicKey = publicKey,
                    avatarColorHex = getRandomColorHex(),
                    isOnline = true
                )
            )
        }

        getOrCreateChatForContact(contactId)
        return contactId
    }

    suspend fun editMessage(messageId: Long, newText: String) {
        messageDao.editMessage(messageId, newText)
    }

    suspend fun deleteMessage(messageId: Long, softDelete: Boolean = true) {
        if (softDelete) {
            messageDao.softDeleteMessage(messageId)
        } else {
            messageDao.hardDeleteMessage(messageId)
        }
    }

    suspend fun toggleStarMessage(messageId: Long, isStarred: Boolean) {
        messageDao.setStarred(messageId, isStarred)
    }

    suspend fun addContact(name: String, phone: String, about: String = "Available on Local Chat"): Long {
        val contact = Contact(
            name = name,
            phone = phone,
            about = about,
            avatarColorHex = getRandomColorHex()
        )
        val contactId = contactDao.insertContact(contact)
        getOrCreateChatForContact(contactId)
        return contactId
    }

    suspend fun toggleBlockContact(contactId: Long, isBlocked: Boolean) {
        val contact = contactDao.getContactById(contactId) ?: return
        contactDao.updateContact(contact.copy(isBlocked = isBlocked))
    }

    suspend fun deleteContact(contact: Contact) {
        val chat = chatDao.getChatByContactId(contact.id)
        if (chat != null) {
            deleteChat(chat.id)
        }
        contactDao.deleteContact(contact)
        if (contact.deviceId.isNotEmpty()) {
            deviceDao.deleteDeviceById(contact.deviceId)
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.saveSettings(settings)
        p2pManager.myDisplayName = settings.myName
        p2pManager.myAbout = settings.myAbout
    }

    /**
     * Initializes default clean settings without ANY fake users or demo chats.
     */
    private suspend fun initCleanSettingsIfEmpty() {
        val existing = settingsDao.getSettingsSync()
        if (existing == null) {
            val newSettings = AppSettings(
                myName = "",
                myAbout = "Available on Local Chat",
                deviceId = p2pManager.localDeviceId,
                isOnboardingCompleted = false
            )
            settingsDao.saveSettings(newSettings)
        } else {
            p2pManager.myDisplayName = existing.myName
            p2pManager.myAbout = existing.myAbout
        }
    }

    private fun getRandomColorHex(): String {
        val colors = listOf("#00A884", "#34B7F1", "#9C27B0", "#FF9800", "#E91E63", "#4CAF50", "#3F51B5")
        return colors.random()
    }
}
