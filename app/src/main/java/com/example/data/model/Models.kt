package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String = "",
    val name: String,
    val phone: String = "",
    val avatarUri: String? = null,
    val avatarColorHex: String = "#00A884",
    val about: String = "Available on Local Chat",
    val ipAddress: String? = null,
    val port: Int = 8888,
    val publicKey: String = "",
    val isBlocked: Boolean = false,
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

/** User profile entity alias for local-first peer and contact storage */
typealias UserProfile = Contact

@Entity(tableName = "devices")
data class PairedDevice(
    @PrimaryKey val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8888,
    val publicKey: String = "",
    val isTrusted: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val draftText: String = ""
)

/** Conversation entity alias for local-first chat threads */
typealias Conversation = Chat

data class ChatWithContact(
    val chat: Chat,
    val contact: Contact
)

/** Room 1-to-N relation connecting a Conversation with its local ChatMessages */
data class ConversationWithMessages(
    @Embedded val conversation: Conversation,
    @Relation(
        parentColumn = "id",
        entityColumn = "chatId"
    )
    val messages: List<ChatMessage>
)

/** Room 1-to-1 relation connecting a Conversation with its participant UserProfile */
data class ConversationWithParticipant(
    @Embedded val conversation: Conversation,
    @Relation(
        parentColumn = "contactId",
        entityColumn = "id"
    )
    val participant: UserProfile
)

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, STICKER
}

enum class MessageStatus {
    WAITING_FOR_CONNECTION, SENDING, SENT, DELIVERED, READ
}

@Entity(
    tableName = "messages",
    indices = [Index(value = ["chatId", "timestamp"])]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val senderId: String = "ME", // "ME" or contact deviceId
    val senderDeviceId: String = "ME",
    val receiverDeviceId: String = "",
    val text: String,
    val messageType: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val replyToMessageId: Long? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val attachmentId: String? = null,
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val attachmentSize: String? = null,
    val transferProgress: Float? = null, // 0.0 to 1.0 during active transfer
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false
)

/** ChatMessage entity alias for local-first peer-to-peer message records */
typealias ChatMessage = Message

@Entity(tableName = "transfers")
data class FileTransfer(
    @PrimaryKey val id: String,
    val messageId: Long = 0,
    val fileName: String,
    val filePath: String,
    val totalBytes: Long,
    val transferredBytes: Long = 0,
    val isIncoming: Boolean,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val isPaused: Boolean = false
)

enum class StatusType {
    TEXT, IMAGE, VIDEO
}

@Entity(tableName = "statuses")
data class Status(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val authorAvatarUri: String? = null,
    val authorColorHex: String = "#00A884",
    val type: StatusType = StatusType.TEXT,
    val content: String, // Text message or file path
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000,
    val isViewed: Boolean = false,
    val backgroundColorHex: String = "#005C4B",
    val isMine: Boolean = false
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val contactName: String,
    val avatarColorHex: String = "#00A884",
    val isVideo: Boolean = false,
    val isIncoming: Boolean = true,
    val isMissed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val myName: String = "",
    val myAbout: String = "Available on Local Chat",
    val myAvatarUri: String? = null,
    val deviceId: String = "",
    val themeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    val appLockEnabled: Boolean = false,
    val pinCode: String = "",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val enterKeySends: Boolean = true,
    val isOnboardingCompleted: Boolean = false
)

data class DiscoveredPeer(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8888,
    val about: String = "Available on Local Chat",
    val distanceEstimate: String = "Nearby",
    val isConnected: Boolean = false,
    val isPaired: Boolean = false
)

data class PairRequest(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8888,
    val about: String = "",
    val publicKey: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
