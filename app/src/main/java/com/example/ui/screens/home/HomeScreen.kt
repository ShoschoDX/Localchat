package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.NearbyDevice
import com.example.ui.components.IncomingCallDialog
import com.example.ui.components.IncomingPairRequestDialog
import com.example.ui.components.ManualConnectDialog
import com.example.ui.components.SearchTopBar
import com.example.ui.components.ShowQrCodeDialog
import com.example.ui.theme.BrandTeal
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onOpenChat: (chatId: Long, contactId: Long) -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStatusViewer: (com.example.data.model.Status) -> Unit,
    onCreateTextStatus: () -> Unit,
    onCreateMediaStatus: () -> Unit,
    onStartCall: (contactId: Long, contactName: String, isVideo: Boolean) -> Unit,
    onDirectNearbyChat: (NearbyDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Chats", "Status", "Calls", "Nearby")

    val filteredChats by mainViewModel.filteredChats.collectAsState()
    val statuses by mainViewModel.statuses.collectAsState()
    val calls by mainViewModel.calls.collectAsState()
    val nearbyDevices by mainViewModel.nearbyDevices.collectAsState()
    val isScanning by mainViewModel.isNearbyScanning.collectAsState()
    val localIpAddress by mainViewModel.localIpAddress.collectAsState()
    val settings by mainViewModel.settings.collectAsState()

    val incomingPairReq by mainViewModel.incomingPairRequest.collectAsState()
    val incomingCallSig by mainViewModel.incomingCallSignal.collectAsState()

    val isSearching by mainViewModel.isSearching.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val searchCategory by mainViewModel.searchCategory.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showManualConnectDialog by remember { mutableStateOf(false) }

    val totalUnread = filteredChats.sumOf { it.chat.unreadCount }

    // Dialogs
    if (showQrDialog) {
        ShowQrCodeDialog(
            deviceId = mainViewModel.myDeviceId,
            displayName = settings?.myName ?: "Local User",
            localIp = localIpAddress,
            onDismiss = { showQrDialog = false }
        )
    }

    if (showManualConnectDialog) {
        ManualConnectDialog(
            onDismiss = { showManualConnectDialog = false },
            onConnect = { ip, port, targetId ->
                mainViewModel.connectToDevice(ip, port, targetId) { success, _ ->
                    if (success) {
                        showManualConnectDialog = false
                    }
                }
            }
        )
    }

    incomingPairReq?.let { request ->
        IncomingPairRequestDialog(
            request = request,
            onAccept = { mainViewModel.acceptPairRequest(request) },
            onDecline = { mainViewModel.declinePairRequest(request) }
        )
    }

    incomingCallSig?.let { signal ->
        if (signal.action.equals("INVITE", ignoreCase = true)) {
            IncomingCallDialog(
                callSignal = signal,
                onAccept = {
                    mainViewModel.clearCallSignal()
                    onStartCall(0L, signal.callerName, signal.callType.equals("VIDEO", ignoreCase = true))
                },
                onDecline = {
                    mainViewModel.clearCallSignal()
                }
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            if (isSearching) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChange = { mainViewModel.setSearchQuery(it) },
                    onCloseSearch = { mainViewModel.setSearching(false) },
                    selectedCategory = searchCategory,
                    onSelectCategory = { mainViewModel.setSearchCategory(it) }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_localchat_logo),
                                contentDescription = "Local Chat Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Local Chat",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandTeal
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { mainViewModel.setSearching(true) }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Show My QR Code") },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showQrDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manual Connect") },
                                    leadingIcon = { Icon(Icons.Default.SettingsEthernet, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showManualConnectDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Chat") },
                                    leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onOpenContacts()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings & Privacy") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onOpenSettings()
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
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    FloatingActionButton(
                        onClick = onOpenContacts,
                        containerColor = BrandTeal,
                        contentColor = Color.White
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = "New Chat")
                    }
                }
                1 -> {
                    // Handled inside StatusTab with 2 FABs
                }
                2 -> {
                    FloatingActionButton(
                        onClick = onOpenContacts,
                        containerColor = BrandTeal,
                        contentColor = Color.White
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "New Call")
                    }
                }
                3 -> {
                    FloatingActionButton(
                        onClick = { mainViewModel.startNearbyScanning() },
                        containerColor = BrandTeal,
                        contentColor = Color.White
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Scan Nearby")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandTeal
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            if (index == 0 && totalUnread > 0) {
                                BadgedBox(badge = { Badge { Text(totalUnread.toString()) } }) {
                                    Text(title, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Text(title, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    ChatsTab(
                        chats = filteredChats,
                        onOpenChat = onOpenChat,
                        onPinChat = { id, pinned -> mainViewModel.togglePinChat(id, pinned) },
                        onMuteChat = { id, muted -> mainViewModel.toggleMuteChat(id, muted) },
                        onDeleteChat = { id -> mainViewModel.deleteChat(id) },
                        onScanNearby = {
                            selectedTab = 3
                            mainViewModel.startNearbyScanning()
                        },
                        onShowQrCode = { showQrDialog = true },
                        onManualConnect = { showManualConnectDialog = true }
                    )
                }
                1 -> {
                    StatusTab(
                        statuses = statuses,
                        onOpenStatusViewer = onOpenStatusViewer,
                        onCreateTextStatus = onCreateTextStatus,
                        onCreateMediaStatus = onCreateMediaStatus
                    )
                }
                2 -> {
                    CallsTab(
                        calls = calls,
                        onStartCall = onStartCall
                    )
                }
                3 -> {
                    NearbyTab(
                        nearbyDevices = nearbyDevices,
                        isScanning = isScanning,
                        localIpAddress = localIpAddress,
                        myDeviceId = mainViewModel.myDeviceId,
                        onStartScan = { mainViewModel.startNearbyScanning() },
                        onToggleConnect = { id -> mainViewModel.toggleConnectNearbyDevice(id) },
                        onDirectChat = onDirectNearbyChat,
                        onShowQrCode = { showQrDialog = true },
                        onManualConnect = { showManualConnectDialog = true }
                    )
                }
            }
        }
    }
}
