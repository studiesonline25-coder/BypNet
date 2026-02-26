package com.bypnet.app.tunnel.ssh

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.TunnelEngine
import com.bypnet.app.tunnel.TunnelStatus
import com.jcraft.jsch.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

/**
 * SSH Tunnel Engine using JSch.
 *
 * Replicates HTTP Custom tunneling:
 * 1. Optionally connects through an HTTP proxy via CustomHttpProxy (payload injection)
 * 2. Establishes an SSH session over the proxy socket
 * 3. Opens a direct-tcpip channel for bidirectional traffic forwarding
 *
 * The VPN service reads/writes raw IP packets from the TUN interface
 * and forwards them through this engine's input/output streams.
 */
class SshEngine : TunnelEngine() {

    private var session: Session? = null
    private var directChannel: ChannelDirectTCPIP? = null

    companion object {
        const val DEFAULT_SSH_PORT = 22
        private const val CONNECTION_TIMEOUT = 15000 // 15s
        private const val KEEPALIVE_INTERVAL = 30000 // 30s
    }

    override suspend fun connect(config: TunnelConfig) = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            log("Connecting to SSH server ${config.serverHost}:${config.serverPort}...")

            val jsch = JSch()

            // Create session
            val sshSession = jsch.getSession(
                config.username,
                config.serverHost,
                config.serverPort
            ).apply {
                setPassword(config.password)

                // Disable strict host key checking for flexibility
                val properties = Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "password,keyboard-interactive")
                }
                setConfig(properties)

                // Timeouts
                timeout = CONNECTION_TIMEOUT
                setServerAliveInterval(KEEPALIVE_INTERVAL)
                setServerAliveCountMax(3)
            }

            // Handle HTTP proxy if configured (payload injection)
            if (config.proxyHost.isNotEmpty() && config.proxyPort > 0) {
                log("Using HTTP proxy ${config.proxyHost}:${config.proxyPort}")
                val proxy = CustomHttpProxy(config) { msg, level ->
                    log(msg, level)
                }
                sshSession.setProxy(proxy)
            }

            log("Authenticating as '${config.username}'...")
            sshSession.connect(CONNECTION_TIMEOUT)

            if (sshSession.isConnected) {
                session = sshSession
                log("SSH session established successfully!", "SUCCESS")

                // Open a direct-tcpip channel for traffic forwarding
                // This creates a tunnel through the SSH session
                val channel = sshSession.openChannel("direct-tcpip") as ChannelDirectTCPIP
                channel.setHost(config.serverHost)
                channel.setPort(config.serverPort)
                channel.connect(CONNECTION_TIMEOUT)

                if (channel.isConnected) {
                    directChannel = channel
                    log("Direct-tcpip channel opened to ${config.serverHost}:${config.serverPort}", "SUCCESS")
                } else {
                    log("Failed to open direct-tcpip channel", "ERROR")
                    reportError("Direct-tcpip channel failed")
                    return@withContext
                }

                updateStatus(TunnelStatus.CONNECTED)
            } else {
                reportError("Failed to establish SSH session")
            }
        } catch (e: JSchException) {
            reportError("SSH connection failed: ${e.message}", e)
        } catch (e: Exception) {
            reportError("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.DISCONNECTING)
            log("Disconnecting SSH session...")

            try { directChannel?.disconnect() } catch (_: Exception) {}
            directChannel = null

            session?.let {
                if (it.isConnected) {
                    it.disconnect()
                }
            }
            session = null

            log("SSH session disconnected", "SUCCESS")
            updateStatus(TunnelStatus.DISCONNECTED)
        } catch (e: Exception) {
            reportError("Error disconnecting: ${e.message}", e)
        }
    }

    /**
     * Get the input stream from the direct-tcpip channel.
     * Used by VPN service for downstream (remote → TUN).
     */
    fun getInputStream(): InputStream? = directChannel?.inputStream

    /**
     * Get the output stream from the direct-tcpip channel.
     * Used by VPN service for upstream (TUN → remote).
     */
    fun getOutputStream(): OutputStream? = directChannel?.outputStream

    /**
     * Legacy method — no longer used.
     */
    fun getLocalProxyPort(): Int = 0
}
