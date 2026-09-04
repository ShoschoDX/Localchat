package com.example.data.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.data.model.DiscoveredPeer
import com.example.data.model.PairRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class ConnectedDevice(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val isIncoming: Boolean
)

data class CallSignalEvent(
    val callType: String,
    val action: String,
    val callerDeviceId: String,
    val callerName: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface P2PMessageListener {
    fun onChatMessageReceived(
        messageId: Long,
        senderDeviceId: String,
        receiverDeviceId: String,
        text: String,
        messageType: String,
        timestamp: Long,
        replyToId: Long?,
        replyToText: String?,
        attachmentName: String?,
        attachmentSize: String?,
        attachmentBase64: String?
    )

    fun onMessageAckReceived(messageId: Long, status: String, senderDeviceId: String)
    fun onDevicePaired(deviceId: String, name: String, ip: String, port: Int, about: String, publicKey: String)
}

class P2PConnectionManager(
    private val context: Context,
    private val identityManager: P2PIdentityManager
) {
    companion object {
        private const val TAG = "P2PConnectionManager"
        private const val PORT = 8888
        private const val UDP_PORT = 8888
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    val localDeviceId: String = identityManager.getOrCreateDeviceId()

    private val _localIp = MutableStateFlow(identityManager.getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _connectedDevices = MutableStateFlow<Map<String, ConnectedDevice>>(emptyMap())
    val connectedDevices: StateFlow<Map<String, ConnectedDevice>> = _connectedDevices.asStateFlow()

    private val _incomingPairRequests = MutableStateFlow<PairRequest?>(null)
    val incomingPairRequests: StateFlow<PairRequest?> = _incomingPairRequests.asStateFlow()

    private val _incomingCallSignals = MutableStateFlow<CallSignalEvent?>(null)
    val incomingCallSignals: StateFlow<CallSignalEvent?> = _incomingCallSignals.asStateFlow()

    private val activeSockets = ConcurrentHashMap<String, Socket>()
    private val socketWriters = ConcurrentHashMap<String, BufferedWriter>()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var udpDiscoveryJob: Job? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var messageListener: P2PMessageListener? = null
    var myDisplayName: String = "Local User"
    var myAbout: String = "Available on Local Chat"

    init {
        identityManager.ensureKeystoreIdentity()
        startServer()
    }

    fun setMessageListener(listener: P2PMessageListener) {
        this.messageListener = listener
    }

    /**
     * Starts listening on local TCP socket for incoming peer connections.
     */
    fun startServer() {
        if (serverJob?.isActive == true) return
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(PORT).apply {
                    reuseAddress = true
                }
                _localIp.value = identityManager.getLocalIpAddress()
                Log.d(TAG, "P2P Server listening on port $PORT, IP: ${_localIp.value}")

                while (isActive) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        handleIncomingSocket(socket)
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server socket on $PORT", e)
            }
        }
    }

    private fun handleIncomingSocket(socket: Socket) {
        scope.launch {
            var peerDeviceId: String? = null
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                while (isActive && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue

                    val json = JSONObject(line)
                    val type = json.optString("type")

                    when (type) {
                        P2PProtocol.TYPE_PAIR_REQUEST -> {
                            val deviceId = json.optString("deviceId")
                            val name = json.optString("name")
                            val about = json.optString("about")
                            val pubKey = json.optString("publicKey")
                            val port = json.optInt("port", PORT)
                            val ip = socket.inetAddress.hostAddress ?: ""

                            peerDeviceId = deviceId
                            activeSockets[deviceId] = socket
                            socketWriters[deviceId] = writer

                            _incomingPairRequests.value = PairRequest(
                                deviceId = deviceId,
                                name = name,
                                ipAddress = ip,
                                port = port,
                                about = about,
                                publicKey = pubKey
                            )
                        }

                        P2PProtocol.TYPE_PAIR_RESPONSE -> {
                            val accepted = json.optBoolean("accepted", false)
                            val deviceId = json.optString("deviceId")
                            val name = json.optString("name")
                            val about = json.optString("about")
                            val pubKey = json.optString("publicKey")
                            val ip = socket.inetAddress.hostAddress ?: ""

                            if (accepted) {
                                peerDeviceId = deviceId
                                activeSockets[deviceId] = socket
                                socketWriters[deviceId] = writer
                                updateConnectedDevice(deviceId, name, ip, PORT, isIncoming = false)
                                messageListener?.onDevicePaired(deviceId, name, ip, PORT, about, pubKey)
                            }
                        }

                        P2PProtocol.TYPE_CHAT_MESSAGE -> {
                            val messageId = json.optLong("messageId")
                            val senderDeviceId = json.optString("senderDeviceId")
                            val receiverDeviceId = json.optString("receiverDeviceId")
                            val text = json.optString("text")
                            val msgType = json.optString("messageType", "TEXT")
                            val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                            val replyToId = if (json.has("replyToId")) json.optLong("replyToId") else null
                            val replyToText = json.optString("replyToText", null)
                            val attachmentName = json.optString("attachmentName", null)
                            val attachmentSize = json.optString("attachmentSize", null)
                            val attachmentBase64 = json.optString("attachmentBase64", null)

                            messageListener?.onChatMessageReceived(
                                messageId = messageId,
                                senderDeviceId = senderDeviceId,
                                receiverDeviceId = receiverDeviceId,
                                text = text,
                                messageType = msgType,
                                timestamp = timestamp,
                                replyToId = replyToId,
                                replyToText = replyToText,
                                attachmentName = attachmentName,
                                attachmentSize = attachmentSize,
                                attachmentBase64 = attachmentBase64
                            )

                            // Automatically send DELIVERED ACK back
                            val ackPacket = P2PProtocol.createMessageAck(messageId, "DELIVERED", localDeviceId)
                            sendRawLine(writer, ackPacket)
                        }

                        P2PProtocol.TYPE_MESSAGE_ACK -> {
                            val messageId = json.optLong("messageId")
                            val status = json.optString("status")
                            val senderDeviceId = json.optString("senderDeviceId")
                            messageListener?.onMessageAckReceived(messageId, status, senderDeviceId)
                        }

                        P2PProtocol.TYPE_CALL_SIGNAL -> {
                            val callType = json.optString("callType")
                            val action = json.optString("action")
                            val callerDeviceId = json.optString("callerDeviceId")
                            val callerName = json.optString("callerName")
                            _incomingCallSignals.value = CallSignalEvent(
                                callType = callType,
                                action = action,
                                callerDeviceId = callerDeviceId,
                                callerName = callerName
                            )
                        }

                        P2PProtocol.TYPE_PING -> {
                            sendRawLine(writer, JSONObject().put("type", P2PProtocol.TYPE_PONG).toString())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Socket closed or error: ${e.message}")
            } finally {
                peerDeviceId?.let {
                    activeSockets.remove(it)
                    socketWriters.remove(it)
                    removeConnectedDevice(it)
                }
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Connect directly to a peer by local IP address and port.
     */
    fun connectToDevice(
        ip: String,
        port: Int = PORT,
        targetDeviceId: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 4000)
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                // Send pairing request packet
                val pairPacket = P2PProtocol.createPairRequest(
                    deviceId = localDeviceId,
                    name = myDisplayName.ifBlank { "User" },
                    about = myAbout,
                    ipAddress = identityManager.getLocalIpAddress(),
                    port = PORT,
                    publicKey = identityManager.getPublicKeyString()
                )
                sendRawLine(writer, pairPacket)

                // Launch listener for this socket
                handleIncomingSocket(socket)
                onResult(true, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to $ip:$port", e)
                onResult(false, e.localizedMessage ?: "Connection failed")
            }
        }
    }

    /**
     * Sends a chat message over the active P2P connection to targetDeviceId.
     */
    fun sendChatMessage(
        targetDeviceId: String,
        targetIp: String?,
        messageId: Long,
        text: String,
        messageType: String = "TEXT",
        replyToId: Long? = null,
        replyToText: String? = null,
        attachmentName: String? = null,
        attachmentSize: String? = null,
        attachmentBase64: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        scope.launch {
            val writer = socketWriters[targetDeviceId]
            if (writer != null) {
                val packet = P2PProtocol.createChatMessage(
                    messageId = messageId,
                    senderDeviceId = localDeviceId,
                    receiverDeviceId = targetDeviceId,
                    text = text,
                    messageType = messageType,
                    timestamp = System.currentTimeMillis(),
                    replyToId = replyToId,
                    replyToText = replyToText,
                    attachmentName = attachmentName,
                    attachmentSize = attachmentSize,
                    attachmentBase64 = attachmentBase64
                )
                val success = sendRawLine(writer, packet)
                onResult(success)
            } else if (!targetIp.isNullOrBlank()) {
                // Try auto-reconnecting to peer
                connectToDevice(targetIp, PORT, targetDeviceId) { connected, _ ->
                    if (connected) {
                        scope.launch {
                            delay(400)
                            val w = socketWriters[targetDeviceId]
                            if (w != null) {
                                val packet = P2PProtocol.createChatMessage(
                                    messageId = messageId,
                                    senderDeviceId = localDeviceId,
                                    receiverDeviceId = targetDeviceId,
                                    text = text,
                                    messageType = messageType,
                                    timestamp = System.currentTimeMillis(),
                                    replyToId = replyToId,
                                    replyToText = replyToText,
                                    attachmentName = attachmentName,
                                    attachmentSize = attachmentSize,
                                    attachmentBase64 = attachmentBase64
                                )
                                onResult(sendRawLine(w, packet))
                            } else {
                                onResult(false)
                            }
                        }
                    } else {
                        onResult(false)
                    }
                }
            } else {
                onResult(false)
            }
        }
    }

    /**
     * Sends a call signaling packet (INVITE, ACCEPT, DECLINE, END).
     */
    fun sendCallSignal(
        targetDeviceId: String,
        callType: String,
        action: String
    ) {
        scope.launch {
            val writer = socketWriters[targetDeviceId] ?: return@launch
            val packet = P2PProtocol.createCallSignal(
                callType = callType,
                action = action,
                callerDeviceId = localDeviceId,
                callerName = myDisplayName
            )
            sendRawLine(writer, packet)
        }
    }

    fun clearCallSignal() {
        _incomingCallSignals.value = null
    }

    fun acceptPairRequest(request: PairRequest) {
        scope.launch {
            val writer = socketWriters[request.deviceId]
            if (writer != null) {
                val response = P2PProtocol.createPairResponse(
                    accepted = true,
                    deviceId = localDeviceId,
                    name = myDisplayName.ifBlank { "User" },
                    about = myAbout,
                    publicKey = identityManager.getPublicKeyString()
                )
                sendRawLine(writer, response)
                updateConnectedDevice(request.deviceId, request.name, request.ipAddress, request.port, isIncoming = true)
                messageListener?.onDevicePaired(
                    request.deviceId,
                    request.name,
                    request.ipAddress,
                    request.port,
                    request.about,
                    request.publicKey
                )
            }
            _incomingPairRequests.value = null
        }
    }

    fun declinePairRequest(request: PairRequest) {
        scope.launch {
            val writer = socketWriters[request.deviceId]
            if (writer != null) {
                val response = P2PProtocol.createPairResponse(
                    accepted = false,
                    deviceId = localDeviceId,
                    name = myDisplayName,
                    about = "",
                    publicKey = ""
                )
                sendRawLine(writer, response)
            }
            _incomingPairRequests.value = null
        }
    }

    fun dismissPairRequest() {
        _incomingPairRequests.value = null
    }

    private fun sendRawLine(writer: BufferedWriter, line: String): Boolean {
        return try {
            writer.write(line)
            writer.newLine()
            writer.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendRawLine error: ${e.message}")
            false
        }
    }

    private fun updateConnectedDevice(id: String, name: String, ip: String, port: Int, isIncoming: Boolean) {
        val current = _connectedDevices.value.toMutableMap()
        current[id] = ConnectedDevice(id, name, ip, port, isIncoming)
        _connectedDevices.value = current
    }

    private fun removeConnectedDevice(id: String) {
        val current = _connectedDevices.value.toMutableMap()
        current.remove(id)
        _connectedDevices.value = current
    }

    // =========================================================================
    // PEER DISCOVERY (NSD + UDP BROADCAST BEACON)
    // =========================================================================

    fun startDiscovery() {
        if (_isDiscovering.value) return
        _isDiscovering.value = true
        _localIp.value = identityManager.getLocalIpAddress()

        // 1. Start UDP Broadcast Beacon
        startUdpBeacon()

        // 2. Start Android Network Service Discovery (NSD)
        startNsdDiscovery()
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = null
        stopNsdDiscovery()
    }

    private fun startUdpBeacon() {
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = scope.launch {
            // Listener socket for UDP broadcast replies and discovery pings
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(UDP_PORT))
                    broadcast = true
                }
            } catch (e: Exception) {
                try {
                    socket = DatagramSocket()
                    socket.broadcast = true
                } catch (_: Exception) {
                }
            }

            val udpSocket = socket ?: return@launch

            // Receiver coroutine
            launch {
                val buffer = ByteArray(1024)
                while (isActive && !udpSocket.isClosed) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        udpSocket.receive(packet)
                        val text = String(packet.data, 0, packet.length)
                        val senderIp = packet.address.hostAddress ?: ""

                        if (senderIp == _localIp.value) continue // Skip self

                        val json = JSONObject(text)
                        val type = json.optString("type")
                        val devId = json.optString("deviceId")
                        val devName = json.optString("name")
                        val devAbout = json.optString("about", "Available on Local Chat")
                        val devPort = json.optInt("port", PORT)

                        if (devId.isNotEmpty() && devId != localDeviceId) {
                            addDiscoveredPeer(
                                DiscoveredPeer(
                                    deviceId = devId,
                                    name = devName,
                                    ipAddress = senderIp,
                                    port = devPort,
                                    about = devAbout,
                                    distanceEstimate = "Nearby",
                                    isConnected = activeSockets.containsKey(devId)
                                )
                            )

                            // If this was a ping, respond with pong
                            if (type == P2PProtocol.TYPE_DISCOVER_PING) {
                                val replyJson = JSONObject().apply {
                                    put("type", P2PProtocol.TYPE_DISCOVER_PONG)
                                    put("deviceId", localDeviceId)
                                    put("name", myDisplayName)
                                    put("about", myAbout)
                                    put("port", PORT)
                                }.toString().toByteArray()

                                val replyPacket = DatagramPacket(replyJson, replyJson.size, packet.address, UDP_PORT)
                                try {
                                    udpSocket.send(replyPacket)
                                } catch (_: Exception) {
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            }

            // Periodic broadcast transmitter
            while (isActive) {
                try {
                    val pingJson = JSONObject().apply {
                        put("type", P2PProtocol.TYPE_DISCOVER_PING)
                        put("deviceId", localDeviceId)
                        put("name", myDisplayName)
                        put("about", myAbout)
                        put("port", PORT)
                    }.toString().toByteArray()

                    val broadcastAddr = InetAddress.getByName("255.255.255.255")
                    val broadcastPacket = DatagramPacket(pingJson, pingJson.size, broadcastAddr, UDP_PORT)
                    udpSocket.send(broadcastPacket)
                } catch (_: Exception) {
                }
                delay(3000)
            }
        }
    }

    private fun startNsdDiscovery() {
        try {
            nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

            // Register this device on NSD
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "LocalChat-$localDeviceId"
                serviceType = P2PProtocol.SERVICE_TYPE
                port = PORT
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {}
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }

            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)

            // Discover other devices
            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onDiscoveryStopped(serviceType: String) {}

                override fun onServiceFound(service: NsdServiceInfo) {
                    if (service.serviceName.contains(localDeviceId)) return
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                            val host = resolvedService.host?.hostAddress ?: return
                            if (host == _localIp.value) return
                            val peerId = resolvedService.serviceName.removePrefix("LocalChat-")
                            addDiscoveredPeer(
                                DiscoveredPeer(
                                    deviceId = peerId,
                                    name = "Local User ($peerId)",
                                    ipAddress = host,
                                    port = resolvedService.port,
                                    distanceEstimate = "Local Network",
                                    isConnected = activeSockets.containsKey(peerId)
                                )
                            )
                        }
                    })
                }

                override fun onServiceLost(service: NsdServiceInfo) {}
            }

            nsdManager?.discoverServices(P2PProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (_: Exception) {
        }
    }

    private fun stopNsdDiscovery() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {
        }
        registrationListener = null
        discoveryListener = null
    }

    private fun addDiscoveredPeer(peer: DiscoveredPeer) {
        val current = _discoveredPeers.value.toMutableList()
        val index = current.indexOfFirst { it.deviceId == peer.deviceId || it.ipAddress == peer.ipAddress }
        if (index >= 0) {
            current[index] = peer
        } else {
            current.add(peer)
        }
        _discoveredPeers.value = current
    }
}
