package com.example.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.Contact
import com.example.data.model.Status
import com.example.ui.screens.call.ActiveCallDialog
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chat.MediaGalleryScreen
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.lock.AppLockScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.status.CreateStatusScreen
import com.example.ui.screens.status.StatusViewerScreen
import com.example.ui.viewmodel.MainViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CONTACTS = "contacts"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val STATUS_CREATE = "status_create"
    const val CHAT = "chat/{chatId}/{contactId}"
    const val MEDIA_GALLERY = "media_gallery/{contactName}/{chatId}"
    const val STATUS_VIEWER = "status_viewer/{statusId}"

    fun chat(chatId: Long, contactId: Long) = "chat/$chatId/$contactId"
    fun mediaGallery(contactName: String, chatId: Long) = "media_gallery/$contactName/$chatId"
    fun statusViewer(statusId: Long) = "status_viewer/$statusId"
}

data class ActiveCallState(
    val contactId: Long,
    val contactName: String,
    val avatarColorHex: String = "#00A884",
    val isVideo: Boolean = false
)

@Composable
fun LocalChatNavGraph(
    mainViewModel: MainViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val settings by mainViewModel.settings.collectAsState()
    val isLocked by mainViewModel.isAppLocked.collectAsState()
    val statuses by mainViewModel.statuses.collectAsState()

    var activeCall by remember { mutableStateOf<ActiveCallState?>(null) }

    // Media status photo picker
    val statusPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = mainViewModel.mediaHelper.copyUriToLocal(
                it,
                mainViewModel.mediaHelper.imagesDir,
                "status_${System.currentTimeMillis()}.jpg"
            )
            mainViewModel.createMediaStatus(localPath, "Photo story", isVideo = false)
        }
    }

    // If App Lock is enabled and locked, show lock screen
    if (settings?.appLockEnabled == true && isLocked) {
        AppLockScreen(
            correctPin = settings?.pinCode,
            onUnlocked = { mainViewModel.unlockApp() }
        )
        return
    }

    val startDestination = if (settings?.isOnboardingCompleted == true) Routes.HOME else Routes.ONBOARDING

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = { name ->
                    mainViewModel.updateProfile(name, "Using Local Chat offline")
                    mainViewModel.completeOnboarding()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                mainViewModel = mainViewModel,
                onOpenChat = { chatId, contactId ->
                    navController.navigate(Routes.chat(chatId, contactId))
                },
                onOpenContacts = {
                    navController.navigate(Routes.CONTACTS)
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onOpenStatusViewer = { status ->
                    navController.navigate(Routes.statusViewer(status.id))
                },
                onCreateTextStatus = {
                    navController.navigate(Routes.STATUS_CREATE)
                },
                onCreateMediaStatus = {
                    statusPhotoPicker.launch("image/*")
                },
                onStartCall = { contactId, contactName, isVideo ->
                    activeCall = ActiveCallState(contactId, contactName, isVideo = isVideo)
                },
                onDirectNearbyChat = { peer ->
                    mainViewModel.addContact(peer.name, peer.ipAddress, "Discovered via Local LAN") { contactId ->
                        navController.navigate(Routes.chat(chatId = 0, contactId = contactId))
                    }
                }
            )
        }

        composable(Routes.CONTACTS) {
            val contacts by mainViewModel.contacts.collectAsState()
            ContactsScreen(
                contacts = contacts,
                onBack = { navController.popBackStack() },
                onSelectContact = { contact ->
                    val existingChat = mainViewModel.chats.value.firstOrNull { it.chat.contactId == contact.id }
                    val chatId = existingChat?.chat?.id ?: 0L
                    navController.navigate(Routes.chat(chatId, contact.id)) {
                        popUpTo(Routes.CONTACTS) { inclusive = true }
                    }
                },
                onAddContact = { name, phone, about ->
                    mainViewModel.addContact(name, phone, about) { newContactId ->
                        navController.navigate(Routes.chat(0L, newContactId)) {
                            popUpTo(Routes.CONTACTS) { inclusive = true }
                        }
                    }
                },
                onDeleteContact = { contact ->
                    mainViewModel.deleteContact(contact)
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("contactId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
            val contact = mainViewModel.contacts.collectAsState().value.firstOrNull { it.id == contactId }

            ChatScreen(
                chatId = chatId,
                contactId = contactId,
                onBack = { navController.popBackStack() },
                onStartCall = { isVideo ->
                    activeCall = ActiveCallState(
                        contactId = contactId,
                        contactName = contact?.name ?: "Contact",
                        avatarColorHex = contact?.avatarColorHex ?: "#00A884",
                        isVideo = isVideo
                    )
                },
                onOpenMediaGallery = {
                    navController.navigate(Routes.mediaGallery(contact?.name ?: "Chat", chatId))
                }
            )
        }

        composable(
            route = Routes.MEDIA_GALLERY,
            arguments = listOf(
                navArgument("contactName") { type = NavType.StringType },
                navArgument("chatId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val contactName = backStackEntry.arguments?.getString("contactName") ?: "Chat"
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val messages by mainViewModel.chatRepo.getMessages(chatId).collectAsState(emptyList())

            MediaGalleryScreen(
                contactName = contactName,
                messages = messages,
                onBack = { navController.popBackStack() },
                onOpenMessageMedia = { /* Handled in full viewer inside chat */ }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                settings = settings,
                onBack = { navController.popBackStack() },
                onSaveProfile = { name, about, avatarUri ->
                    mainViewModel.updateProfile(name, about, avatarUri)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                backupRepo = mainViewModel.backupRepo,
                onBack = { navController.popBackStack() },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onUpdateSettings = { newSettings ->
                    mainViewModel.updateSettings(newSettings)
                }
            )
        }

        composable(Routes.STATUS_CREATE) {
            CreateStatusScreen(
                onBack = { navController.popBackStack() },
                onCreateStatus = { text, bgColor ->
                    mainViewModel.createTextStatus(text, bgColor)
                }
            )
        }

        composable(
            route = Routes.STATUS_VIEWER,
            arguments = listOf(
                navArgument("statusId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val statusId = backStackEntry.arguments?.getLong("statusId") ?: 0L
            val status = statuses.firstOrNull { it.id == statusId }
            if (status != null) {
                StatusViewerScreen(
                    status = status,
                    onBack = { navController.popBackStack() },
                    onDelete = { mainViewModel.deleteStatus(statusId) }
                )
            } else {
                navController.popBackStack()
            }
        }
    }

    // Active Call Dialog overlay
    activeCall?.let { call ->
        ActiveCallDialog(
            contactName = call.contactName,
            avatarColorHex = call.avatarColorHex,
            isVideo = call.isVideo,
            onEndCall = { duration ->
                mainViewModel.logCall(
                    contactId = call.contactId,
                    contactName = call.contactName,
                    avatarColorHex = call.avatarColorHex,
                    isVideo = call.isVideo,
                    isIncoming = false,
                    isMissed = false,
                    durationSeconds = duration
                )
                activeCall = null
            }
        )
    }
}
