package com.bypnet.app.tunnel.ssh

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.payload.PayloadProcessor
import com.jcraft.jsch.Proxy
import com.jcraft.jsch.SocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class CustomHttpProxy(
    private val config: TunnelConfig,
    private val logger: (String, String) -> Unit
) : Proxy {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override fun connect(socketFactory: SocketFactory, host: String, port: Int, timeout: Int) {
        logger("Connecting to HTTP proxy ${config.proxyHost}:${config.proxyPort}...", "INFO")
        try {
            socket = socketFactory.createSocket(config.proxyHost, config.proxyPort) ?: Socket()
            socket?.connect(InetSocketAddress(config.proxyHost, config.proxyPort), timeout)
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            // Generate payload
            val payloadString = if (config.payload.isNotEmpty()) {
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

            logger("Injecting payload to proxy...", "INFO")
            outputStream?.write(payloadString.toByteArray())
            outputStream?.flush()

            // Read proxy response
            val buffer = ByteArray(8192)
            val bytesRead = inputStream?.read(buffer) ?: -1
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                val statusLine = response.substringBefore("\n").trim()
                logger("Proxy Response: $statusLine", "SUCCESS")

                // Typically proxy returns HTTP/1.0 200 Connection established
                if (!response.contains("200") && !response.contains("101")) {
                    throw Exception("Proxy rejected request with status: $statusLine")
                }
            } else {
                throw Exception("Received empty response from HTTP proxy")
            }
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    override fun getInputStream(): InputStream? = inputStream
    override fun getOutputStream(): OutputStream? = outputStream
    override fun getSocket(): Socket? = socket

    override fun close() {
        try { inputStream?.close() } catch (e: Exception) {}
        try { outputStream?.close() } catch (e: Exception) {}
        try { socket?.close() } catch (e: Exception) {}
    }
}
