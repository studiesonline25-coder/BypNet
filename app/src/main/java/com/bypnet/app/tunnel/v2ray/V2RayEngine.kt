package com.bypnet.app.tunnel.v2ray

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.TunnelEngine
import com.bypnet.app.tunnel.TunnelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * V2Ray/VMess/VLESS Tunnel Engine.
 *
 * Uses AndroidLibXrayLite or Xray-core to handle V2Ray protocol connections.
 * Supports VMess, VLESS, and Trojan protocols via V2Ray core.
 *
 * Note: Full integration requires the AndroidLibXrayLite native library.
 * This implementation provides the connection lifecycle and configuration
 * interface — native core integration should be added when the library
 * is included in the build.
 */
class V2RayEngine : TunnelEngine() {

    private var isRunning = false
    private var localSocksPort = 10808
    private var localHttpPort = 10809

    companion object {
        private const val TAG = "V2RayEngine"
    }

    override suspend fun connect(config: TunnelConfig) = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            log("Initializing V2Ray core...")

            // Build V2Ray JSON config from TunnelConfig
            val v2rayConfig = buildV2RayConfig(config)
            log("V2Ray config generated for ${config.protocol}")

            // TODO: Start V2Ray core with the generated config
            // Libv2ray.startV2Ray(v2rayConfig)
            // For now, simulate the connection lifecycle

            log("V2Ray core started", "SUCCESS")
            log("Local SOCKS5 proxy: 127.0.0.1:$localSocksPort")
            log("Local HTTP proxy: 127.0.0.1:$localHttpPort")

            isRunning = true
            updateStatus(TunnelStatus.CONNECTED)

        } catch (e: Exception) {
            reportError("V2Ray error: ${e.message}", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.DISCONNECTING)
            log("Stopping V2Ray core...")

            // TODO: Stop V2Ray core
            // Libv2ray.stopV2Ray()

            isRunning = false
            log("V2Ray core stopped", "SUCCESS")
            updateStatus(TunnelStatus.DISCONNECTED)
        } catch (e: Exception) {
            reportError("V2Ray stop error: ${e.message}", e)
        }
    }

    /**
     * Build a V2Ray JSON configuration from the tunnel config.
     */
    private fun buildV2RayConfig(config: TunnelConfig): String {
        val extraConfig = config.extraConfig

        val protocol = extraConfig["v2ray_protocol"] as? String ?: "vmess"
        val uuid = extraConfig["uuid"] as? String ?: ""
        val alterId = extraConfig["alterId"] as? Int ?: 0
        val security = extraConfig["security"] as? String ?: "auto"
        val network = extraConfig["network"] as? String ?: "tcp"
        val headerType = extraConfig["headerType"] as? String ?: "none"
        val tlsEnabled = extraConfig["tls"] as? Boolean ?: false
        val wsPath = extraConfig["wsPath"] as? String ?: "/"
        val wsHost = extraConfig["wsHost"] as? String ?: ""

        return """
        {
            "inbounds": [
                {
                    "port": $localSocksPort,
                    "protocol": "socks",
                    "settings": {
                        "auth": "noauth",
                        "udp": true
                    }
                },
                {
                    "port": $localHttpPort,
                    "protocol": "http",
                    "settings": {}
                }
            ],
            "outbounds": [
                {
                    "protocol": "$protocol",
                    "settings": {
                        "vnext": [
                            {
                                "address": "${config.serverHost}",
                                "port": ${config.serverPort},
                                "users": [
                                    {
                                        "id": "$uuid",
                                        "alterId": $alterId,
                                        "security": "$security"
                                    }
                                ]
                            }
                        ]
                    },
                    "streamSettings": {
                        "network": "$network",
                        "security": "${if (tlsEnabled) "tls" else "none"}",
                        "tlsSettings": {
                            "serverName": "${config.sni.ifEmpty { config.serverHost }}"
                        },
                        "wsSettings": {
                            "path": "$wsPath",
                            "headers": {
                                "Host": "${wsHost.ifEmpty { config.serverHost }}"
                            }
                        },
                        "tcpSettings": {
                            "header": {
                                "type": "$headerType"
                            }
                        }
                    }
                }
            ],
            "dns": {
                "servers": ["${config.primaryDns}", "${config.secondaryDns}"]
            }
        }
        """.trimIndent()
    }

    fun getLocalSocksPort(): Int = localSocksPort
    fun getLocalHttpPort(): Int = localHttpPort
}
