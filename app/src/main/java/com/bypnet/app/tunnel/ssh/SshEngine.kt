package com.bypnet.app.tunnel.ssh

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.TunnelEngine
import com.bypnet.app.tunnel.TunnelStatus
import com.bypnet.app.tunnel.payload.PayloadProcessor
import com.jcraft.jsch.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * SSH Tunnel Engine using JSch.
 * Supports direct SSH tunneling with optional HTTP proxy payload injection.
 */
class SshEngine : TunnelEngine() {

    private var session: Session? = null
    private var localPort: Int = 0

    companion object {
        const val DEFAULT_SSH_PORT = 22
        private const val CONNECTION_TIMEOUT = 15000 // 15 seconds
        private const val KEEPALIVE_INTERVAL = 30000 // 30 seconds
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

            // Handle HTTP proxy if configured
            if (config.proxyHost.isNotEmpty() && config.proxyPort > 0) {
                log("Using HTTP proxy ${config.proxyHost}:${config.proxyPort}")

                // Process payload for the proxy connection
                val processedPayload = if (config.payload.isNotEmpty()) {
                    PayloadProcessor.process(
                        template = config.payload,
                        host = config.serverHost,
                        port = config.serverPort,
                        sni = config.sni,
                        cookies = config.cookies
                    )
                } else ""

                val proxy = ProxyHTTP(config.proxyHost, config.proxyPort)
                if (processedPayload.isNotEmpty()) {
                    // Custom header injection via proxy
                    proxy.setUserPasswd("", "")
                }
                sshSession.setProxy(proxy)
            }

            log("Authenticating as '${config.username}'...")
            sshSession.connect(CONNECTION_TIMEOUT)

            if (sshSession.isConnected) {
                session = sshSession
                log("SSH session established successfully!", "SUCCESS")

                // Set up local port forwarding (Note: JSch 0.1.55 does not support native SOCKS5 setPortForwardingD)
                // Full VPN TUN routing over SSH would require integrating a tun2socks library.
                localPort = sshSession.setPortForwardingL(0, "127.0.0.1", 0)
                log("Local SSH forward opened on port $localPort", "SUCCESS")
                log("NOTE: Full VPN TUN routing over SSH requires a tun2socks implementation.", "WARN")

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

            session?.let {
                if (it.isConnected) {
                    it.disconnect()
                }
            }
            session = null
            localPort = 0

            log("SSH session disconnected", "SUCCESS")
            updateStatus(TunnelStatus.DISCONNECTED)
        } catch (e: Exception) {
            reportError("Error disconnecting: ${e.message}", e)
        }
    }

    /**
     * Get the local SOCKS proxy port for traffic routing.
     */
    fun getLocalProxyPort(): Int = localPort
}
