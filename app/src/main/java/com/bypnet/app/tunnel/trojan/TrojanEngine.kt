package com.bypnet.app.tunnel.trojan

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.TunnelEngine
import com.bypnet.app.tunnel.TunnelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SNIHostName

/**
 * Trojan Protocol Tunnel Engine.
 *
 * Trojan works by mimicking HTTPS traffic. It establishes a TLS connection
 * to the server and authenticates with a password hash.
 * Unlike V2Ray's Trojan, this is a standalone implementation.
 */
class TrojanEngine : TunnelEngine() {

    private var sslSocket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val CONNECTION_TIMEOUT = 15000
        private const val TROJAN_DEFAULT_PORT = 443
    }

    override suspend fun connect(config: TunnelConfig) = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            log("Connecting to Trojan server ${config.serverHost}:${config.serverPort}...")

            val sni = config.sni.ifEmpty { config.serverHost }

            // Create SSL context
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, java.security.SecureRandom())

            val socket = sslContext.socketFactory.createSocket() as SSLSocket

            // Configure SNI
            val sslParams = SSLParameters()
            sslParams.serverNames = listOf(SNIHostName(sni))
            socket.sslParameters = sslParams
            socket.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")

            log("Connecting with SNI: $sni")

            socket.connect(
                InetSocketAddress(config.serverHost, config.serverPort),
                CONNECTION_TIMEOUT
            )
            socket.startHandshake()

            log("TLS handshake completed: ${socket.session.protocol}")

            // Send Trojan authentication
            val passwordHash = sha224(config.password)
            val trojanRequest = buildTrojanRequest(passwordHash, config)

            socket.outputStream.write(trojanRequest)
            socket.outputStream.flush()

            sslSocket = socket
            inputStream = socket.inputStream
            outputStream = socket.outputStream

            log("Trojan tunnel established!", "SUCCESS")
            updateStatus(TunnelStatus.CONNECTED)

        } catch (e: Exception) {
            reportError("Trojan error: ${e.message}", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.DISCONNECTING)
            log("Closing Trojan connection...")

            inputStream?.close()
            outputStream?.close()
            sslSocket?.close()

            inputStream = null
            outputStream = null
            sslSocket = null

            log("Trojan disconnected", "SUCCESS")
            updateStatus(TunnelStatus.DISCONNECTED)
        } catch (e: Exception) {
            reportError("Trojan disconnect error: ${e.message}", e)
        }
    }

    /**
     * Build a Trojan protocol request.
     * Format: hex(SHA224(password)) + CRLF + CMD + ATYP + DST.ADDR + DST.PORT + CRLF + PAYLOAD
     */
    private fun buildTrojanRequest(passwordHash: String, config: TunnelConfig): ByteArray {
        val buffer = mutableListOf<Byte>()

        // Password hash (hex string)
        buffer.addAll(passwordHash.toByteArray().toList())

        // CRLF
        buffer.add(0x0D)
        buffer.add(0x0A)

        // CMD: 1 = CONNECT
        buffer.add(0x01)

        // ATYP: 3 = Domain name
        buffer.add(0x03)

        // Domain length + domain
        val domain = config.serverHost.toByteArray()
        buffer.add(domain.size.toByte())
        buffer.addAll(domain.toList())

        // Port (big-endian)
        buffer.add((config.serverPort shr 8).toByte())
        buffer.add((config.serverPort and 0xFF).toByte())

        // CRLF
        buffer.add(0x0D)
        buffer.add(0x0A)

        return buffer.toByteArray()
    }

    /**
     * SHA-224 hash (truncated SHA-256).
     */
    private fun sha224(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-224")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
