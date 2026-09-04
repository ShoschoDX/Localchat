package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.StatusType

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = runCatching { MessageType.valueOf(value) }.getOrDefault(MessageType.TEXT)

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = runCatching { MessageStatus.valueOf(value) }.getOrDefault(MessageStatus.READ)

    @TypeConverter
    fun fromStatusType(value: StatusType): String = value.name

    @TypeConverter
    fun toStatusType(value: String): StatusType = runCatching { StatusType.valueOf(value) }.getOrDefault(StatusType.TEXT)
}
