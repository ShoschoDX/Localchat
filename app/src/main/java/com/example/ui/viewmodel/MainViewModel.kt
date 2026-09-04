package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.LocalChatDatabase
import com.example.data.model.AppSettings
import com.example.data.model.CallLog
import com.example.data.model.ChatWithContact
import com.example.data.model.Contact
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.data.model.Status
import com.example.data.repository.BackupRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.NearbyDevice
import com.example.data.repository.NearbyManager
import com.example.data.repository.StatusRepository
import com.example.data.storage.MediaStorageHelper
import com.example.ui.components.SearchCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LocalChatDatabase.getDatabase(application)
    val chatRepo = ChatRepository(database, application)
    val statusRepo = StatusRepository(database)
    val callRepo = CallRepository(database)
    val backupRepo = BackupRepository(application, database)
    val nearbyManager = NearbyManager(application, chatRepo.p2pManager)
    val mediaHelper = MediaStorageHelper(application)

    val chats: StateFlow<List<ChatWithContact>> = chatRepo.chatsWithContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<Contact>> = chatRepo.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statuses: StateFlow<List<Status>> = statusRepo.activeStatuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calls: StateFlow<List<CallLog>> = callRepo.allCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings?> = chatRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Nearby
    val nearbyDevices: StateFlow<List<NearbyDevice>> = nearbyManager.nearbyDevices
    val isNearbyScanning: StateFlow<Boolean> = nearbyManager.isDiscovering
    val localIpAddress: StateFlow<String> = nearbyManager.localIpAddress

    // App Lock State
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Global Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow(SearchCategory.ALL)
    val searchCategory: StateFlow<SearchCategory> = _searchCategory.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Filtered chats based on search
    val filteredChats: StateFlow<List<ChatWithContact>> = combine(
        chats,
        searchQuery,
        searchCategory
    ) { chatList, query, category ->
        if (query.isBlank() && category == SearchCategory.ALL) {
            chatList
        } else {
            chatList.filter { item ->
                val matchesQuery = query.isBlank() ||
                        item.contact.name.contains(query, ignoreCase = true) ||
                        item.chat.lastMessageText.contains(query, ignoreCase = true)

                val matchesCategory = when (category) {
                    SearchCategory.ALL -> true
                    SearchCategory.UNREAD -> item.chat.unreadCount > 0
                    SearchCategory.PHOTOS -> item.chat.lastMessageText.contains("Photo", ignoreCase = true)
                    SearchCategory.VIDEOS -> item.chat.lastMessageText.contains("Video", ignoreCase = true)
                    SearchCategory.AUDIO -> item.chat.lastMessageText.contains("Voice", ignoreCase = true)
                    SearchCategory.DOCUMENTS -> item.chat.lastMessageText.contains("Document", ignoreCase = true)
                    SearchCategory.LINKS -> item.chat.lastMessageText.contains("http", ignoreCase = true)
                }

                matchesQuery && matchesCategory
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchCategory(category: SearchCategory) {
        _searchCategory.value = category
    }

    fun setSearching(searching: Boolean) {
        _isSearching.value = searching
        if (!searching) {
            _searchQuery.value = ""
            _searchCategory.value = SearchCategory.ALL
        }
    }

    fun markChatRead(chatId: Long) {
        viewModelScope.launch {
            chatRepo.markChatRead(chatId)
        }
    }

    fun togglePinChat(chatId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            chatRepo.togglePinChat(chatId, isPinned)
        }
    }

    fun toggleMuteChat(chatId: Long, isMuted: Boolean) {
        viewModelScope.launch {
            chatRepo.toggleMuteChat(chatId, isMuted)
        }
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            chatRepo.deleteChat(chatId)
        }
    }

    fun addContact(name: String, phone: String, about: String = "Using Local Chat", onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = chatRepo.addContact(name, phone, about)
            onComplete(id)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            chatRepo.deleteContact(contact)
        }
    }

    fun toggleBlockContact(contactId: Long, isBlocked: Boolean) {
        viewModelScope.launch {
            chatRepo.toggleBlockContact(contactId, isBlocked)
        }
    }

    fun createTextStatus(text: String, bgColorHex: String) {
        viewModelScope.launch {
            statusRepo.createTextStatus(text, bgColorHex)
        }
    }

    fun createMediaStatus(mediaPath: String, caption: String, isVideo: Boolean) {
        viewModelScope.launch {
            statusRepo.createMediaStatus(mediaPath, caption, isVideo)
        }
    }

    fun deleteStatus(statusId: Long) {
        viewModelScope.launch {
            statusRepo.deleteStatus(statusId)
        }
    }

    fun logCall(contactId: Long, contactName: String, avatarColorHex: String, isVideo: Boolean, isIncoming: Boolean, isMissed: Boolean, durationSeconds: Int = 0) {
        viewModelScope.launch {
            callRepo.logCall(contactId, contactName, avatarColorHex, isVideo, isIncoming, isMissed, durationSeconds)
        }
    }

    fun clearCalls() {
        viewModelScope.launch {
            callRepo.clearCallLogs()
        }
    }

    fun startNearbyScanning() {
        nearbyManager.startDiscovery()
    }

    fun toggleConnectNearbyDevice(deviceId: String) {
        nearbyManager.toggleConnectDevice(deviceId)
    }

    val incomingPairRequest = chatRepo.p2pManager.incomingPairRequests
    val incomingCallSignal = chatRepo.p2pManager.incomingCallSignals
    val myDeviceId = chatRepo.p2pManager.localDeviceId

    fun acceptPairRequest(request: com.example.data.model.PairRequest) {
        chatRepo.p2pManager.acceptPairRequest(request)
    }

    fun declinePairRequest(request: com.example.data.model.PairRequest) {
        chatRepo.p2pManager.declinePairRequest(request)
    }

    fun dismissPairRequest() {
        chatRepo.p2pManager.dismissPairRequest()
    }

    fun clearCallSignal() {
        chatRepo.p2pManager.clearCallSignal()
    }

    fun sendCallSignal(targetDeviceId: String, callType: String, action: String) {
        chatRepo.p2pManager.sendCallSignal(targetDeviceId, callType, action)
    }

    fun connectToDevice(ip: String, port: Int = 8888, targetDeviceId: String? = null, onResult: (Boolean, String?) -> Unit) {
        chatRepo.p2pManager.connectToDevice(ip, port, targetDeviceId, onResult)
    }

    fun updateProfile(name: String, about: String, avatarUri: String? = null) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettings()
            chatRepo.updateSettings(
                current.copy(
                    myName = name,
                    myAbout = about,
                    myAvatarUri = avatarUri ?: current.myAvatarUri
                )
            )
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            chatRepo.updateSettings(newSettings)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val current = settings.value ?: AppSettings()
            chatRepo.updateSettings(current.copy(isOnboardingCompleted = true))
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (settings.value?.appLockEnabled == true) {
            _isAppLocked.value = true
        }
    }
}
