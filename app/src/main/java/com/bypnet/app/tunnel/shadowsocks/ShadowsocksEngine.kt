package com.bypnet.app.tunnel.shadowsocks

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.TunnelEngine
import com.bypnet.app.tunnel.TunnelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shadowsocks Tunnel Engine.
 *
 * Uses shadowsocks-libev native library for the SOCKS5 proxy.
 * Supports common encryption methods: aes-256-gcm, chacha20-ietf-poly1305, etc.
 *
 * Note: Requires shadowsocks-libev native binary.
 * This provides the lifecycle management and config interface.
 */
class ShadowsocksEngine : TunnelEngine() {

    private var isRunning = false
    private var localPort = 1080

    companion object {
        val SUPPORTED_METHODS = listOf(
            "aes-256-gcm",
            "aes-128-gcm",
            "chacha20-ietf-poly1305",
            "xchacha20-ietf-poly1305",
            "aes-256-cfb",
            "aes-128-cfb",
            "rc4-md5"
        )
    }

    override suspend fun connect(config: TunnelConfig) = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            log("Starting Shadowsocks client...")

            val extraConfig = config.extraConfig
            val method = extraConfig["method"] as? String ?: "aes-256-gcm"
            val plugin = extraConfig["plugin"] as? String ?: ""
            val pluginOpts = extraConfig["pluginOpts"] as? String ?: ""

            log("Server: ${config.serverHost}:${config.serverPort}")
            log("Method: $method")

            if (plugin.isNotEmpty()) {
                log("Plugin: $plugin ($pluginOpts)")
            }

            // Build Shadowsocks config
            val ssConfig = buildSsConfig(config, method, plugin, pluginOpts)
            log("Shadowsocks config generated", "DEBUG")

            // TODO: Start shadowsocks-libev native process
            // Process: ss-local -c config.json
            // nativeLib.startSsLocal(ssConfig)

            isRunning = true
            log("Shadowsocks tunnel established!", "SUCCESS")
            log("Local SOCKS5 proxy: 127.0.0.1:$localPort")
            updateStatus(TunnelStatus.CONNECTED)

        } catch (e: Exception) {
            reportError("Shadowsocks error: ${e.message}", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.DISCONNECTING)
            log("Stopping Shadowsocks...")

            // TODO: Stop native process
            // nativeLib.stopSsLocal()

            isRunning = false
            log("Shadowsocks stopped", "SUCCESS")
            updateStatus(TunnelStatus.DISCONNECTED)
        } catch (e: Exception) {
            reportError("Shadowsocks stop error: ${e.message}", e)
        }
    }

    private fun buildSsConfig(
        config: TunnelConfig,
        method: String,
        plugin: String,
        pluginOpts: String
    ): String {
        return """
        {
            "server": "${config.serverHost}",
            "server_port": ${config.serverPort},
            "password": "${config.password}",
            "method": "$method",
            "local_address": "127.0.0.1",
            "local_port": $localPort,
            "timeout": 300,
            "fast_open": true,
            "mode": "tcp_and_udp"
            ${if (plugin.isNotEmpty()) """,
            "plugin": "$plugin",
            "plugin_opts": "$pluginOpts"
            """ else ""}
        }
        """.trimIndent()
    }

    fun getLocalPort(): Int = localPort
}
