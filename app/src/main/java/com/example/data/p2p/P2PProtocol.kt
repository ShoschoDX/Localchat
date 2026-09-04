package com.example.data.p2p

import org.json.JSONArray
import org.json.JSONObject

object P2PProtocol {
    const val DEFAULT_PORT = 8888
    const val SERVICE_TYPE = "_localchat._tcp"

    // Packet Types
    const val TYPE_DISCOVER_PING = "DISCOVER_PING"
    const val TYPE_DISCOVER_PONG = "DISCOVER_PONG"
    const val TYPE_PAIR_REQUEST = "PAIR_REQUEST"
    const val TYPE_PAIR_RESPONSE = "PAIR_RESPONSE"
    const val TYPE_CHAT_MESSAGE = "CHAT_MESSAGE"
    const val TYPE_MESSAGE_ACK = "MESSAGE_ACK"
    const val TYPE_SYNC_REQUEST = "SYNC_REQUEST"
    const val TYPE_SYNC_RESPONSE = "SYNC_RESPONSE"
    const val TYPE_FILE_START = "FILE_START"
    const val TYPE_FILE_CHUNK = "FILE_CHUNK"
    const val TYPE_FILE_COMPLETE = "FILE_COMPLETE"
    const val TYPE_CALL_SIGNAL = "CALL_SIGNAL"
    const val TYPE_PING = "PING"
    const val TYPE_PONG = "PONG"

    fun createPairRequest(
        deviceId: String,
        name: String,
        about: String,
        ipAddress: String,
        port: Int,
        publicKey: String
    ): String {
        return JSONObject().apply {
            put("type", TYPE_PAIR_REQUEST)
            put("deviceId", deviceId)
            put("name", name)
            put("about", about)
            put("ipAddress", ipAddress)
            put("port", port)
            put("publicKey", publicKey)
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }

    fun createPairResponse(
        accepted: Boolean,
        deviceId: String,
        name: String,
        about: String,
        publicKey: String
    ): String {
        return JSONObject().apply {
            put("type", TYPE_PAIR_RESPONSE)
            put("accepted", accepted)
            put("deviceId", deviceId)
            put("name", name)
            put("about", about)
            put("publicKey", publicKey)
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }

    fun createChatMessage(
        messageId: Long,
        senderDeviceId: String,
        receiverDeviceId: String,
        text: String,
        messageType: String = "TEXT",
        timestamp: Long = System.currentTimeMillis(),
        replyToId: Long? = null,
        replyToText: String? = null,
        attachmentName: String? = null,
        attachmentSize: String? = null,
        attachmentBase64: String? = null
    ): String {
        return JSONObject().apply {
            put("type", TYPE_CHAT_MESSAGE)
            put("messageId", messageId)
            put("senderDeviceId", senderDeviceId)
            put("receiverDeviceId", receiverDeviceId)
            put("text", text)
            put("messageType", messageType)
            put("timestamp", timestamp)
            replyToId?.let { put("replyToId", it) }
            replyToText?.let { put("replyToText", it) }
            attachmentName?.let { put("attachmentName", it) }
            attachmentSize?.let { put("attachmentSize", it) }
            attachmentBase64?.let { put("attachmentBase64", it) }
        }.toString()
    }

    fun createMessageAck(
        messageId: Long,
        status: String, // "DELIVERED" or "READ"
        senderDeviceId: String
    ): String {
        return JSONObject().apply {
            put("type", TYPE_MESSAGE_ACK)
            put("messageId", messageId)
            put("status", status)
            put("senderDeviceId", senderDeviceId)
        }.toString()
    }

    fun createSyncRequest(
        senderDeviceId: String,
        lastKnownTimestamp: Long
    ): String {
        return JSONObject().apply {
            put("type", TYPE_SYNC_REQUEST)
            put("senderDeviceId", senderDeviceId)
            put("lastKnownTimestamp", lastKnownTimestamp)
        }.toString()
    }

    fun createCallSignal(
        callType: String, // "VOICE" or "VIDEO"
        action: String,   // "INVITE", "RINGING", "ACCEPT", "DECLINE", "END"
        callerDeviceId: String,
        callerName: String
    ): String {
        return JSONObject().apply {
            put("type", TYPE_CALL_SIGNAL)
            put("callType", callType)
            put("action", action)
            put("callerDeviceId", callerDeviceId)
            put("callerName", callerName)
        }.toString()
    }
}
