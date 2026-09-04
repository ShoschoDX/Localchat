package com.example.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shape tokens ensuring consistent border-radius across all surfaces,
 * containers, cards, dialogs, bottom sheets, and buttons in the application.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Extended shapes specific to chat messaging, voice memos, search bars, and bottom sheets.
 */
@Immutable
data class ChatShapes(
    val outgoingBubble: CornerBasedShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 4.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp
    ),
    val incomingBubble: CornerBasedShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 16.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp
    ),
    val searchBar: CornerBasedShape = RoundedCornerShape(24.dp),
    val bottomSheet: CornerBasedShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    ),
    val avatarBadge: CornerBasedShape = RoundedCornerShape(8.dp),
    val pillButton: CornerBasedShape = RoundedCornerShape(20.dp),
    val mediaThumbnail: CornerBasedShape = RoundedCornerShape(12.dp)
)

val LocalChatShapes = staticCompositionLocalOf { ChatShapes() }
