package com.example.data.p2p

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom

class P2PIdentityManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    companion object {
        private const val KEY_ALIAS = "LocalChatIdentityKey"
        private const val PREFS_NAME = "local_chat_identity_prefs"
        private const val PREF_DEVICE_ID = "device_id"
    }

    /**
     * Retrieves or generates a persistent local identifier in the format LC-XXXXXX (e.g. LC-7F4A92).
     */
    fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(PREF_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        // Generate 6 random uppercase hex characters
        val random = SecureRandom()
        val bytes = ByteArray(3)
        random.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02X".format(it) }
        val newId = "LC-$hex"

        prefs.edit().putString(PREF_DEVICE_ID, newId).apply()
        return newId
    }

    /**
     * Ensures an asymmetric cryptographic key pair exists in Android Keystore
     * for device identity and local end-to-end verification.
     */
    fun ensureKeystoreIdentity() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    "AndroidKeyStore"
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
                    .build()
                kpg.initialize(spec)
                kpg.generateKeyPair()
            }
        } catch (_: Exception) {
            // Fallback gracefully on systems without hardware keystore
        }
    }

    /**
     * Gets the Base64 public key representation to share during peer pairing.
     */
    fun getPublicKeyString(): String {
        return try {
            val cert = keyStore.getCertificate(KEY_ALIAS)
            if (cert != null) {
                Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
            } else {
                getOrCreateDeviceId()
            }
        } catch (_: Exception) {
            getOrCreateDeviceId()
        }
    }

    /**
     * Finds the primary local IPv4 address (e.g. 192.168.1.105) on Wi-Fi, hotspot, or LAN.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val host = addr.hostAddress ?: ""
                        // Pick standard IPv4 address
                        if (host.isNotEmpty() && host.indexOf(':') < 0 && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "127.0.0.1"
    }
}
