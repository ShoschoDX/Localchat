package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AppSettings
import com.example.data.model.CallLog
import com.example.data.model.Chat
import com.example.data.model.Contact
import com.example.data.model.FileTransfer
import com.example.data.model.Message
import com.example.data.model.PairedDevice
import com.example.data.model.Status

@Database(
    entities = [
        Contact::class,
        Chat::class,
        Message::class,
        Status::class,
        CallLog::class,
        AppSettings::class,
        PairedDevice::class,
        FileTransfer::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LocalChatDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun statusDao(): StatusDao
    abstract fun callDao(): CallDao
    abstract fun settingsDao(): SettingsDao
    abstract fun deviceDao(): DeviceDao
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: LocalChatDatabase? = null

        fun getDatabase(context: Context): LocalChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalChatDatabase::class.java,
                    "local_chat.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
