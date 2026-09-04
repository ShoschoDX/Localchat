package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AppSettings
import com.example.data.model.CallLog
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.Contact
import com.example.data.model.Conversation
import com.example.data.model.ConversationWithMessages
import com.example.data.model.ConversationWithParticipant
import com.example.data.model.Message
import com.example.data.model.Status
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): Contact?

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactByIdFlow(id: Long): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getContactByDeviceId(deviceId: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>): List<Long>

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactCount(): Int
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChatById(id: Long): Chat?

    @Query("SELECT * FROM chats WHERE contactId = :contactId LIMIT 1")
    suspend fun getChatByContactId(contactId: Long): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat): Long

    @Update
    suspend fun updateChat(chat: Chat)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatAsRead(chatId: Long)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun setChatPinned(chatId: Long, isPinned: Boolean)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun setChatMuted(chatId: Long, isMuted: Boolean)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: Long)

    @Transaction
    @Query("SELECT * FROM chats WHERE id = :id")
    fun getConversationWithMessages(id: Long): Flow<ConversationWithMessages?>

    @Transaction
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getConversationsWithParticipants(): Flow<List<ConversationWithParticipant>>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun setStarred(messageId: Long, isStarred: Boolean)

    @Query("UPDATE messages SET text = :newText, isEdited = 1 WHERE id = :messageId")
    suspend fun editMessage(messageId: Long, newText: String)

    @Query("UPDATE messages SET isDeleted = 1, text = 'This message was deleted' WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun hardDeleteMessage(messageId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: Long)

    @Query("SELECT * FROM messages WHERE isStarred = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE text LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND attachmentPath IS NOT NULL AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getMediaForChat(chatId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE status IN ('WAITING_FOR_CONNECTION', 'SENDING') ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<Message>

    @Query("SELECT * FROM messages WHERE receiverDeviceId = :deviceId AND status IN ('WAITING_FOR_CONNECTION', 'SENDING') ORDER BY timestamp ASC")
    suspend fun getPendingMessagesForDevice(deviceId: String): List<Message>

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: com.example.data.model.MessageStatus)

    @Query("UPDATE messages SET transferProgress = :progress WHERE id = :messageId")
    suspend fun updateTransferProgress(messageId: Long, progress: Float?)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastSeenTimestamp DESC")
    fun getAllDevices(): Flow<List<com.example.data.model.PairedDevice>>

    @Query("SELECT * FROM devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): com.example.data.model.PairedDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: com.example.data.model.PairedDevice)

    @Delete
    suspend fun deleteDevice(device: com.example.data.model.PairedDevice)

    @Query("DELETE FROM devices WHERE deviceId = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers WHERE isCompleted = 0 ORDER BY id ASC")
    fun getActiveTransfers(): Flow<List<com.example.data.model.FileTransfer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: com.example.data.model.FileTransfer)

    @Update
    suspend fun updateTransfer(transfer: com.example.data.model.FileTransfer)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransfer(id: String)
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses WHERE expiresAt > :now ORDER BY isMine DESC, timestamp DESC")
    fun getActiveStatuses(now: Long = System.currentTimeMillis()): Flow<List<Status>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: Status): Long

    @Query("UPDATE statuses SET isViewed = 1 WHERE id = :statusId")
    suspend fun markStatusViewed(statusId: Long)

    @Query("DELETE FROM statuses WHERE id = :statusId")
    suspend fun deleteStatus(statusId: Long)

    @Query("DELETE FROM statuses WHERE expiresAt <= :now")
    suspend fun purgeExpiredStatuses(now: Long = System.currentTimeMillis())
}

@Dao
interface CallDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(callLog: CallLog): Long

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCalls()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsSync(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}

/** DAO alias for ChatMessage operations */
typealias ChatMessageDao = MessageDao

/** DAO alias for Conversation operations */
typealias ConversationDao = ChatDao

/** DAO alias for UserProfile operations */
typealias UserProfileDao = ContactDao
