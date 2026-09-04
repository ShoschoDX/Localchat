package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Message
import com.example.ui.components.LocalAvatar
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.theme.BrandTeal
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Long,
    contactId: Long,
    onBack: () -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onOpenMediaGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val app = context.applicationContext as android.app.Application
    val chatViewModel: ChatViewModel = viewModel(
        key = "chat_$chatId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(app, chatId, contactId) as T
            }
        }
    )

    val messages by chatViewModel.messages.collectAsState()
    val contact by chatViewModel.contact.collectAsState()
    val replyingTo by chatViewModel.replyingTo.collectAsState()
    val editingMessage by chatViewModel.editingMessage.collectAsState()
    val selectedMessages by chatViewModel.selectedMessages.collectAsState()
    val showVoiceDialog by chatViewModel.showVoiceDialog.collectAsState()
    val activeMediaMessage by chatViewModel.activeMediaViewerMessage.collectAsState()
    val isPeerConnected by chatViewModel.isPeerConnected.collectAsState()

    var showOptionsMenu by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var isInChatSearching by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Media launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { chatViewModel.sendMediaUri(it, isVideo = false) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { chatViewModel.sendPhotoBitmap(it) }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val name = it.lastPathSegment ?: "document.pdf"
            chatViewModel.sendDocumentUri(it, name, "Local doc")
        }
    }

    val isActionMode = selectedMessages.isNotEmpty()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isActionMode) {
                // Action Mode Top Bar
                TopAppBar(
                    title = { Text("${selectedMessages.size} selected", fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { chatViewModel.clearSelection() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            chatViewModel.starSelectedMessages()
                            scope.launch { snackbarHostState.showSnackbar("Updated starred status") }
                        }) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Star")
                        }
                        IconButton(onClick = {
                            val textToCopy = selectedMessages.joinToString("\n") { it.text }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Local Chat Message", textToCopy))
                            chatViewModel.clearSelection()
                            scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                        }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        if (selectedMessages.size == 1) {
                            val singleMsg = selectedMessages.first()
                            IconButton(onClick = {
                                chatViewModel.setReplyingTo(singleMsg)
                                chatViewModel.clearSelection()
                            }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply")
                            }
                            if (singleMsg.senderId == "ME") {
                                IconButton(onClick = {
                                    chatViewModel.setEditingMessage(singleMsg)
                                    chatViewModel.clearSelection()
                                }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                        }
                        IconButton(onClick = {
                            val textToShare = selectedMessages.joinToString("\n") { it.text }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, textToShare)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                            chatViewModel.clearSelection()
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = {
                            chatViewModel.deleteSelectedMessages(softDelete = true)
                            scope.launch { snackbarHostState.showSnackbar("Message deleted") }
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                // Regular Top Bar
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMediaGallery() }
                        ) {
                            LocalAvatar(
                                name = contact?.name ?: "Contact",
                                avatarUri = contact?.avatarUri,
                                avatarColorHex = contact?.avatarColorHex ?: "#00A884",
                                size = 40.dp,
                                showOnlineBadge = true,
                                isOnline = isPeerConnected
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = contact?.name ?: "Contact",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isPeerConnected) "Connected (Local P2P)" else "Offline (Local Queue)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPeerConnected) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onStartCall(true) }) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call")
                        }
                        IconButton(onClick = { onStartCall(false) }) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Voice Call")
                        }
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Media, links & docs") },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onOpenMediaGallery()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search in chat") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        isInChatSearching = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear chat history") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        chatViewModel.clearChat()
                                        scope.launch { snackbarHostState.showSnackbar("Chat cleared") }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search filter inside chat if open
                if (isInChatSearching) {
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = inChatSearchQuery,
                                onValueChange = { inChatSearchQuery = it },
                                placeholder = { Text("Find in messages…") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                isInChatSearching = false
                                inChatSearchQuery = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        }
                    }
                }

                // Messages list with Date Headers
                val displayMessages = if (inChatSearchQuery.isBlank()) {
                    messages
                } else {
                    messages.filter { it.text.contains(inChatSearchQuery, ignoreCase = true) }
                }

                if (!isPeerConnected) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device offline on LAN. Messages are saved locally and will auto-deliver when peer connects.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    reverseLayout = false
                ) {
                    var previousDate: String? = null

                    displayMessages.forEach { message ->
                        val currentDate = formatDateHeader(message.timestamp)
                        if (currentDate != previousDate) {
                            previousDate = currentDate
                            item(key = "header_$currentDate") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = currentDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "msg_${message.id}") {
                            MessageBubble(
                                message = message,
                                isSelected = selectedMessages.contains(message),
                                onLongClick = {
                                    chatViewModel.toggleSelectMessage(message)
                                },
                                onClick = {
                                    if (isActionMode) {
                                        chatViewModel.toggleSelectMessage(message)
                                    } else if (message.attachmentPath != null) {
                                        chatViewModel.openMediaViewer(message)
                                    }
                                },
                                onOpenMedia = {
                                    chatViewModel.openMediaViewer(it)
                                }
                            )
                        }
                    }
                }

                // Message composer
                ChatComposer(
                    replyingTo = replyingTo,
                    editingMessage = editingMessage,
                    onCancelReplyOrEdit = {
                        chatViewModel.setReplyingTo(null)
                        chatViewModel.setEditingMessage(null)
                    },
                    onSendMessage = { text ->
                        chatViewModel.sendTextMessage(text)
                    },
                    onPickImageFromGallery = {
                        galleryLauncher.launch("image/*")
                    },
                    onTakePhotoWithCamera = {
                        cameraLauncher.launch(null)
                    },
                    onPickDocument = {
                        documentLauncher.launch(arrayOf("*/*"))
                    },
                    onOpenVoiceRecord = {
                        chatViewModel.openVoiceDialog()
                    },
                    onSendSticker = { sticker ->
                        chatViewModel.sendSticker(sticker)
                    }
                )
            }

            // Voice recorder overlay dialog
            if (showVoiceDialog) {
                VoiceRecorderDialog(
                    onDismiss = { chatViewModel.closeVoiceDialog() },
                    onSendRecording = { duration ->
                        chatViewModel.sendVoiceNote(duration)
                    }
                )
            }

            // Fullscreen Photo/Video Viewer
            activeMediaMessage?.let { mediaMsg ->
                FullscreenViewerDialog(
                    message = mediaMsg,
                    onDismiss = { chatViewModel.closeMediaViewer() },
                    onDelete = {
                        chatViewModel.deleteSelectedMessages(softDelete = false)
                    },
                    onToggleStar = {
                        scope.launch {
                            chatViewModel.chatRepo.toggleStarMessage(mediaMsg.id, !mediaMsg.isStarred)
                        }
                    }
                )
            }
        }
    }
}

private fun formatDateHeader(timestamp: Long): String {
    val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        messageCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                messageCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
        messageCalendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                messageCalendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
