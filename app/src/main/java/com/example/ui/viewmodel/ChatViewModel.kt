package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.LocalChatDatabase
import com.example.data.model.Contact
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.data.repository.ChatRepository
import com.example.data.storage.MediaStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application,
    val chatId: Long,
    val contactId: Long
) : AndroidViewModel(application) {
    private val database = LocalChatDatabase.getDatabase(application)
    val chatRepo = ChatRepository(database, application)
    val mediaHelper = MediaStorageHelper(application)

    val messages: StateFlow<List<Message>> = chatRepo.getMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contact: StateFlow<Contact?> = chatRepo.getContact(contactId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPeerConnected: StateFlow<Boolean> = combine(
        contact,
        chatRepo.p2pManager.connectedDevices
    ) { contactObj: Contact?, connectedMap: Map<String, com.example.data.p2p.ConnectedDevice> ->
        contactObj != null && contactObj.deviceId.isNotEmpty() && connectedMap.containsKey(contactObj.deviceId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val transferStates = chatRepo.fileTransferManager.transferStates

    fun connectToPeer() {
        val c = contact.value ?: return
        val ip = c.ipAddress
        if (!ip.isNullOrEmpty()) {
            chatRepo.p2pManager.connectToDevice(ip, c.port, c.deviceId) { _, _ -> }
        }
    }

    // Active replying to message
    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    // Active editing message
    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    // Multi-selection
    private val _selectedMessages = MutableStateFlow<Set<Message>>(emptySet())
    val selectedMessages: StateFlow<Set<Message>> = _selectedMessages.asStateFlow()

    // Voice recording dialog
    private val _showVoiceDialog = MutableStateFlow(false)
    val showVoiceDialog: StateFlow<Boolean> = _showVoiceDialog.asStateFlow()

    // Full screen media viewer
    private val _activeMediaViewerMessage = MutableStateFlow<Message?>(null)
    val activeMediaViewerMessage: StateFlow<Message?> = _activeMediaViewerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepo.markChatRead(chatId)
        }
    }

    fun setReplyingTo(message: Message?) {
        _replyingTo.value = message
        _editingMessage.value = null
    }

    fun setEditingMessage(message: Message?) {
        _editingMessage.value = message
        _replyingTo.value = null
    }

    fun toggleSelectMessage(message: Message) {
        val current = _selectedMessages.value.toMutableSet()
        if (current.contains(message)) {
            current.remove(message)
        } else {
            current.add(message)
        }
        _selectedMessages.value = current
    }

    fun clearSelection() {
        _selectedMessages.value = emptySet()
    }

    fun openVoiceDialog() {
        _showVoiceDialog.value = true
    }

    fun closeVoiceDialog() {
        _showVoiceDialog.value = false
    }

    fun openMediaViewer(message: Message) {
        _activeMediaViewerMessage.value = message
    }

    fun closeMediaViewer() {
        _activeMediaViewerMessage.value = null
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return

        val editing = _editingMessage.value
        if (editing != null) {
            viewModelScope.launch {
                chatRepo.editMessage(editing.id, text.trim())
                _editingMessage.value = null
            }
            return
        }

        val replying = _replyingTo.value
        viewModelScope.launch {
            chatRepo.sendMessage(
                chatId = chatId,
                text = text.trim(),
                type = MessageType.TEXT,
                replyToMessageId = replying?.id,
                replyToText = replying?.text,
                replyToSender = replying?.senderId
            )
            _replyingTo.value = null
        }
    }

    fun sendPhotoBitmap(bitmap: Bitmap, caption: String = "") {
        viewModelScope.launch {
            val localPath = mediaHelper.saveBitmap(bitmap, "PHOTO")
            chatRepo.sendMessage(
                chatId = chatId,
                text = caption,
                type = MessageType.IMAGE,
                attachmentPath = localPath,
                attachmentName = "Photo"
            )
        }
    }

    fun sendMediaUri(uri: Uri, isVideo: Boolean, caption: String = "") {
        viewModelScope.launch {
            val targetDir = if (isVideo) mediaHelper.videosDir else mediaHelper.imagesDir
            val localPath = mediaHelper.copyUriToLocal(uri, targetDir, if (isVideo) "video.mp4" else "image.jpg")
            chatRepo.sendMessage(
                chatId = chatId,
                text = caption,
                type = if (isVideo) MessageType.VIDEO else MessageType.IMAGE,
                attachmentPath = localPath,
                attachmentName = if (isVideo) "Video" else "Photo"
            )
        }
    }

    fun sendDocumentUri(uri: Uri, displayName: String, sizeString: String) {
        viewModelScope.launch {
            val localPath = mediaHelper.copyUriToLocal(uri, mediaHelper.documentsDir, displayName)
            chatRepo.sendMessage(
                chatId = chatId,
                text = displayName,
                type = MessageType.DOCUMENT,
                attachmentPath = localPath,
                attachmentName = displayName,
                attachmentSize = sizeString
            )
        }
    }

    fun sendVoiceNote(durationSeconds: Int) {
        closeVoiceDialog()
        viewModelScope.launch {
            val durationText = String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60)
            chatRepo.sendMessage(
                chatId = chatId,
                text = "Voice message ($durationText)",
                type = MessageType.AUDIO,
                attachmentName = "Voice Note",
                attachmentSize = durationText
            )
        }
    }

    fun sendSticker(stickerText: String) {
        viewModelScope.launch {
            chatRepo.sendMessage(
                chatId = chatId,
                text = stickerText,
                type = MessageType.STICKER
            )
        }
    }

    fun starSelectedMessages() {
        val selected = _selectedMessages.value
        viewModelScope.launch {
            selected.forEach { msg ->
                chatRepo.toggleStarMessage(msg.id, !msg.isStarred)
            }
            clearSelection()
        }
    }

    fun deleteSelectedMessages(softDelete: Boolean = true) {
        val selected = _selectedMessages.value
        viewModelScope.launch {
            selected.forEach { msg ->
                chatRepo.deleteMessage(msg.id, softDelete)
            }
            clearSelection()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepo.clearChatHistory(chatId)
        }
    }
}
