package com.bypnet.app.tunnel.http

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.TunnelEngine
import com.bypnet.app.tunnel.TunnelStatus
import com.bypnet.app.tunnel.payload.PayloadProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * HTTP Proxy Tunnel Engine.
 * Connects through an HTTP proxy using custom payloads/headers.
 * This is the core tunneling method used in HTTP Custom-style apps.
 */
class HttpProxyEngine : TunnelEngine() {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val CONNECTION_TIMEOUT = 15000
        private const val BUFFER_SIZE = 32768
    }

    override suspend fun connect(config: TunnelConfig) = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.CONNECTING)

            val targetHost = config.proxyHost.ifEmpty { config.serverHost }
            val targetPort = if (config.proxyPort > 0) config.proxyPort else config.serverPort

            log("Connecting to HTTP proxy $targetHost:$targetPort...")

            val proxySocket = Socket()
            proxySocket.connect(
                InetSocketAddress(targetHost, targetPort),
                CONNECTION_TIMEOUT
            )
            proxySocket.soTimeout = 0
            proxySocket.keepAlive = true
            proxySocket.tcpNoDelay = true

            socket = proxySocket

            // Process and send payload
            val payload = if (config.payload.isNotEmpty()) {
                PayloadProcessor.process(
                    template = config.payload,
                    host = config.serverHost,
                    port = config.serverPort,
                    sni = config.sni,
                    cookies = config.cookies
                )
            } else {
                PayloadProcessor.process(
                    template = PayloadProcessor.defaultConnectPayload(),
                    host = config.serverHost,
                    port = config.serverPort,
                    sni = config.sni
                )
            }

            log("Sending HTTP payload...")
            log("Payload preview: ${payload.replace("\r\n", "↵").take(100)}...", "DEBUG")

            proxySocket.getOutputStream().write(payload.toByteArray())
            proxySocket.getOutputStream().flush()

            // Read response
            val buffer = ByteArray(BUFFER_SIZE)
            val bytesRead = proxySocket.getInputStream().read(buffer)

            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                val firstLine = response.lines().firstOrNull() ?: ""
                log("Proxy response: $firstLine")

                // Check for HTTP 200 (connection established)
                if (response.contains("200")) {
                    inputStream = proxySocket.getInputStream()
                    outputStream = proxySocket.getOutputStream()

                    log("HTTP proxy tunnel established!", "SUCCESS")
                    updateStatus(TunnelStatus.CONNECTED)
                } else if (response.contains("101")) {
                    // WebSocket upgrade
                    inputStream = proxySocket.getInputStream()
                    outputStream = proxySocket.getOutputStream()

                    log("WebSocket tunnel established!", "SUCCESS")
                    updateStatus(TunnelStatus.CONNECTED)
                } else {
                    reportError("Proxy rejected: $firstLine")
                }
            } else {
                reportError("No response from proxy")
            }

        } catch (e: Exception) {
            reportError("HTTP proxy error: ${e.message}", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.DISCONNECTING)
            log("Closing HTTP proxy tunnel...")

            inputStream?.close()
            outputStream?.close()
            socket?.close()

            inputStream = null
            outputStream = null
            socket = null

            log("HTTP proxy disconnected", "SUCCESS")
            updateStatus(TunnelStatus.DISCONNECTED)
        } catch (e: Exception) {
            reportError("Disconnect error: ${e.message}", e)
        }
    }

    fun getInputStream(): InputStream? = inputStream
    fun getOutputStream(): OutputStream? = outputStream
}
