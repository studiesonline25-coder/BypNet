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
                    
                    // Enable legacy and widespread SSH encryption methods often used by VPN providers
                    put("kex", "curve25519-sha256,curve25519-sha256@libssh.org,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521,diffie-hellman-group-exchange-sha256,diffie-hellman-group16-sha512,diffie-hellman-group18-sha512,diffie-hellman-group14-sha256,diffie-hellman-group14-sha1,diffie-hellman-group1-sha1,diffie-hellman-group-exchange-sha1")
                    put("server_host_key", "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256,ssh-rsa,ssh-dss")
                    put("cipher.s2c", "aes128-ctr,aes192-ctr,aes256-ctr,aes128-gcm@openssh.com,aes256-gcm@openssh.com,chacha20-poly1305@openssh.com,aes128-cbc,aes192-cbc,aes256-cbc,3des-ctr,3des-cbc,blowfish-cbc")
                    put("cipher.c2s", "aes128-ctr,aes192-ctr,aes256-ctr,aes128-gcm@openssh.com,aes256-gcm@openssh.com,chacha20-poly1305@openssh.com,aes128-cbc,aes192-cbc,aes256-cbc,3des-ctr,3des-cbc,blowfish-cbc")
                    put("mac.s2c", "hmac-sha2-256-etm@openssh.com,hmac-sha2-512-etm@openssh.com,hmac-sha1-etm@openssh.com,hmac-sha2-256,hmac-sha2-512,hmac-sha1,hmac-md5")
                    put("mac.c2s", "hmac-sha2-256-etm@openssh.com,hmac-sha2-512-etm@openssh.com,hmac-sha1-etm@openssh.com,hmac-sha2-256,hmac-sha2-512,hmac-sha1,hmac-md5")
                }
                setConfig(properties)

                timeout = CONNECTION_TIMEOUT
                setServerAliveInterval(KEEPALIVE_INTERVAL)
                setServerAliveCountMax(3)
            }

            val isSsl = config.protocol.equals("SSL", ignoreCase = true) ||
                        config.protocol.equals("TLS", ignoreCase = true) ||
                        config.protocol.equals("SSL/TLS", ignoreCase = true)

            // Handle HTTP/SSL proxy if configured
            if (isSsl) {
                log("Using SSL/TLS Tunnel mode")
                val proxy = CustomSslProxy(config) { msg, level -> log(msg, level) }
                sshSession.setProxy(proxy)
            } else if (config.proxyHost.isNotEmpty() && config.proxyPort > 0) {
                log("Using HTTP proxy ${config.proxyHost}:${config.proxyPort}")
                val proxy = CustomHttpProxy(config) { msg, level -> log(msg, level) }
                sshSession.setProxy(proxy)
            } else if (config.payload.isNotEmpty()) {
                // Direct SSH with payload but without proxy/SSL (e.g. direct SNI injection)
                log("Using Direct SSH with Payload mode")
                val proxy = CustomHttpProxy(config.copy(proxyHost = config.serverHost, proxyPort = config.serverPort)) { msg, level -> log(msg, level) }
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
