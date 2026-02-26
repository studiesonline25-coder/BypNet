package com.bypnet.app.tunnel.ssh

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.TunnelEngine
import com.bypnet.app.tunnel.TunnelStatus
import com.jcraft.jsch.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * SSH Tunnel Engine using JSch.
 *
 * 1. Optionally connects through an HTTP proxy via CustomHttpProxy (payload injection)
 * 2. Establishes an SSH session over the proxy socket
 * 3. Starts a local SOCKS5 proxy that routes through SSH direct-tcpip channels
 */
class SshEngine : TunnelEngine() {

    private var session: Session? = null
    private var socksProxy: LocalSocksProxy? = null
    private val proxyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val DEFAULT_SSH_PORT = 22
        private const val CONNECTION_TIMEOUT = 15000
        private const val KEEPALIVE_INTERVAL = 30000
    }

    override suspend fun connect(config: TunnelConfig) = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            log("Connecting to SSH server ${config.serverHost}:${config.serverPort}...")

            val jsch = JSch()

            val sshSession = jsch.getSession(
                config.username,
                config.serverHost,
                config.serverPort
            ).apply {
                setPassword(config.password)

                val properties = Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "password,keyboard-interactive")
                }
                setConfig(properties)

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
                log("SSH session established!", "SUCCESS")

                // Start local SOCKS5 proxy that routes through SSH
                val proxy = LocalSocksProxy(sshSession)
                proxy.start(proxyScope) { msg, level -> log(msg, level) }
                socksProxy = proxy

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

            socksProxy?.stop()
            socksProxy = null

            session?.let {
                if (it.isConnected) it.disconnect()
            }
            session = null

            log("SSH session disconnected", "SUCCESS")
            updateStatus(TunnelStatus.DISCONNECTED)
        } catch (e: Exception) {
            reportError("Error disconnecting: ${e.message}", e)
        }
    }

    /**
     * Get the local SOCKS5 proxy port.
     */
    fun getLocalSocksPort(): Int = socksProxy?.port ?: 0
}
