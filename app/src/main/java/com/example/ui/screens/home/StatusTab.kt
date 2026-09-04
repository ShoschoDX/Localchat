package com.example.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Status
import com.example.ui.components.LocalAvatar
import com.example.ui.theme.BrandTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusTab(
    statuses: List<Status>,
    onOpenStatusViewer: (Status) -> Unit,
    onCreateTextStatus: () -> Unit,
    onCreateMediaStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val myStatus = statuses.firstOrNull { it.isMine }
    val otherStatuses = statuses.filter { !it.isMine }
    val recentUpdates = otherStatuses.filter { !it.isViewed }
    val viewedUpdates = otherStatuses.filter { it.isViewed }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // My Status Item
            item {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStatus != null) {
                                onOpenStatusViewer(myStatus)
                            } else {
                                onCreateTextStatus()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LocalAvatar(
                            name = myStatus?.authorName ?: "My Status",
                            avatarUri = myStatus?.authorAvatarUri,
                            size = 54.dp,
                            hasStatusUpdate = myStatus != null
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (myStatus != null) {
                                    "Tap to view your 24h status update"
                                } else {
                                    "Tap to add a local status update"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recent updates header & list
            if (recentUpdates.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent updates",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(recentUpdates) { status ->
                    StatusRowItem(status = status, onClick = { onOpenStatusViewer(status) })
                }
            }

            // Viewed updates header & list
            if (viewedUpdates.isNotEmpty()) {
                item {
                    Text(
                        text = "Viewed updates",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(viewedUpdates) { status ->
                    StatusRowItem(status = status, onClick = { onOpenStatusViewer(status) })
                }
            }
        }

        // Floating Action Buttons for text & camera status
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = onCreateTextStatus,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "New text status")
            }

            FloatingActionButton(
                onClick = onCreateMediaStatus,
                containerColor = BrandTeal,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "New photo status")
            }
        }
    }
}

@Composable
private fun StatusRowItem(
    status: Status,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocalAvatar(
                name = status.authorName,
                avatarUri = status.authorAvatarUri,
                avatarColorHex = status.authorColorHex,
                size = 50.dp,
                hasStatusUpdate = true
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.authorName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(status.timestamp))
                Text(
                    text = "Today, $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
