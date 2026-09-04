package com.example.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.repository.BackupRepository
import com.example.ui.components.LocalAvatar
import com.example.ui.theme.BrandTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings?,
    backupRepo: BackupRepository,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPinDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }

    // SAF Create Document for Export Backup
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        scope.launch {
            val res = backupRepo.createBackup(targetUri = uri)
            res.fold(
                onSuccess = { msg -> snackbarHostState.showSnackbar(msg) },
                onFailure = { err -> snackbarHostState.showSnackbar("Backup failed: ${err.localizedMessage}") }
            )
        }
    }

    // SAF Open Document for Import Backup
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val res = backupRepo.restoreBackup(it)
                res.fold(
                    onSuccess = { msg -> snackbarHostState.showSnackbar(msg) },
                    onFailure = { err -> snackbarHostState.showSnackbar("Restore failed: ${err.localizedMessage}") }
                )
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Profile Card Header
            item {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenProfile)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LocalAvatar(
                            name = settings?.myName ?: "Me",
                            avatarUri = settings?.myAvatarUri,
                            size = 60.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings?.myName ?: "Me",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = settings?.myAbout ?: "Using Local Chat",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Privacy & App Lock
            item {
                SettingsCategoryTitle("Privacy & Security")
                SettingsRowWithSwitch(
                    icon = Icons.Default.Lock,
                    title = "App Lock PIN",
                    subtitle = if (settings?.appLockEnabled == true) "4-digit PIN protection active" else "Protect app when opened",
                    checked = settings?.appLockEnabled ?: false,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinDialog = true
                        } else {
                            settings?.let { onUpdateSettings(it.copy(appLockEnabled = false, pinCode = "")) }
                        }
                    }
                )
            }

            // Notifications
            item {
                SettingsCategoryTitle("Notifications")
                SettingsRowWithSwitch(
                    icon = Icons.Default.Notifications,
                    title = "Conversation alerts",
                    subtitle = "Play local tone for new messages",
                    checked = settings?.soundEnabled ?: true,
                    onCheckedChange = { checked ->
                        settings?.let { onUpdateSettings(it.copy(soundEnabled = checked)) }
                    }
                )
            }

            // Storage & Data Usage
            item {
                SettingsCategoryTitle("Storage & Data")
                SettingsRowAction(
                    icon = Icons.Default.DataUsage,
                    title = "Storage & Network stats",
                    subtitle = "0 KB sent to cloud servers • 100% Local"
                ) {
                    showStorageDialog = true
                }
            }

            // Backup & Restore
            item {
                SettingsCategoryTitle("Local Backup & Restore")
                SettingsRowAction(
                    icon = Icons.Default.Backup,
                    title = "Export Chat Backup (ZIP)",
                    subtitle = "Package local SQLite database and media files"
                ) {
                    exportBackupLauncher.launch("LocalChatBackup_${System.currentTimeMillis()}.zip")
                }
                SettingsRowAction(
                    icon = Icons.Default.Restore,
                    title = "Import & Restore Backup",
                    subtitle = "Restore chats from a previous Local Chat ZIP file"
                ) {
                    restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }
            }

            // About Local Chat
            item {
                SettingsCategoryTitle("About")
                SettingsRowAction(
                    icon = Icons.Default.Shield,
                    title = "Local Chat Privacy Guarantee",
                    subtitle = "Version 1.0.0 • No servers, no accounts"
                ) {
                    showAboutDialog = true
                }
            }
        }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit security code to lock Local Chat:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pin.length == 4) {
                            settings?.let { onUpdateSettings(it.copy(appLockEnabled = true, pinCode = pin)) }
                            showPinDialog = false
                            scope.launch { snackbarHostState.showSnackbar("App lock PIN activated") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Text("Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Storage Dialog
    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Local Storage & Network") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Internet data sent: 0 KB (No cloud server)", fontWeight = FontWeight.Bold, color = BrandTeal)
                    Text("• Local database size: ~48 KB (Room SQLite)")
                    Text("• Local media folder: app/files/LocalChat/Media")
                    Text("• Privacy audit: Zero tracking SDKs or telemetry")
                }
            },
            confirmButton = {
                Button(onClick = { showStorageDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)) {
                    Text("OK")
                }
            }
        )
    }

    // About Local Chat Guarantee Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Local Chat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Local Chat is a modern Android messaging application designed for absolute privacy.")
                    Text("• No cloud server\n• No Firebase\n• No external database\n• Works completely offline in Airplane Mode\n• Direct P2P Wi-Fi discovery\n• Encrypted local backups")
                    Text("“Your conversations, photos, videos, and files never leave your device.”", fontWeight = FontWeight.SemiBold, color = BrandTeal)
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = BrandTeal,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsRowWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = BrandTeal, checkedTrackColor = BrandTeal.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun SettingsRowAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
