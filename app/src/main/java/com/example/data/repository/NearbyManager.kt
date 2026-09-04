package com.example.data.repository

import android.content.Context
import com.example.data.p2p.P2PConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NearbyDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val isConnected: Boolean = false,
    val signalStrength: Int = 85,
    val about: String = "Available on Local Chat",
    val distanceEstimate: String = "Nearby"
)

class NearbyManager(
    private val context: Context,
    val p2pManager: P2PConnectionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val isDiscovering: StateFlow<Boolean> = p2pManager.isDiscovering
    val localIpAddress: StateFlow<String> = p2pManager.localIp

    val nearbyDevices: StateFlow<List<NearbyDevice>> = combine(
        p2pManager.discoveredPeers,
        p2pManager.connectedDevices
    ) { peers, connectedMap ->
        peers.map { peer ->
            NearbyDevice(
                id = peer.deviceId,
                name = peer.name,
                ipAddress = peer.ipAddress,
                isConnected = connectedMap.containsKey(peer.deviceId),
                signalStrength = if (connectedMap.containsKey(peer.deviceId)) 95 else 75,
                about = peer.about,
                distanceEstimate = peer.distanceEstimate
            )
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startDiscovery() {
        p2pManager.startDiscovery()
    }

    fun stopDiscovery() {
        p2pManager.stopDiscovery()
    }

    fun connectDevice(deviceId: String, ipAddress: String, port: Int = 8888, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        p2pManager.connectToDevice(ipAddress, port, deviceId, onResult)
    }

    fun toggleConnectDevice(deviceId: String) {
        val device = nearbyDevices.value.find { it.id == deviceId }
        if (device != null) {
            if (device.isConnected) {
                // Already connected
            } else {
                p2pManager.connectToDevice(device.ipAddress, 8888, deviceId) { _, _ -> }
            }
        }
    }
}
