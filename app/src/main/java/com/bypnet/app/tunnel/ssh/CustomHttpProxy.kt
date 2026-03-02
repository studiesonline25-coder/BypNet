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

    override fun connect(socketFactory: SocketFactory?, host: String, port: Int, timeout: Int) {
        logger("Connecting to HTTP proxy ${config.proxyHost}:${config.proxyPort}...", "INFO")
        try {
            if (socketFactory == null) {
                socket = Socket()
                socket?.connect(InetSocketAddress(config.proxyHost, config.proxyPort), timeout)
                inputStream = socket?.inputStream
                outputStream = socket?.outputStream
            } else {
                socket = socketFactory.createSocket(config.proxyHost, config.proxyPort)
                inputStream = socketFactory.getInputStream(socket)
                outputStream = socketFactory.getOutputStream(socket)
            }

            // Generate payload (only if using a real HTTP Proxy)
            val isDirectPayload = config.proxyHost == config.serverHost && config.proxyPort == config.serverPort

            if (!isDirectPayload) {
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

                logger("Injecting payload to HTTP proxy...", "INFO")
                outputStream?.write(payloadString.toByteArray())
                outputStream?.flush()

                // Read proxy response carefully byte-by-byte so we don't consume the SSH banner
                val inStream = inputStream ?: throw Exception("InputStream is null")
                var line = readLineFromStream(inStream)
                logger("Proxy Response: $line", "SUCCESS")

                if (!line.contains("200") && !line.contains("101")) {
                    throw Exception("Proxy rejected request with status: $line")
                }

                // Consume all headers until \r\n\r\n
                while (true) {
                    val header = readLineFromStream(inStream)
                    if (header.isEmpty()) break
                }
            } else {
                logger("Direct payload connection established, waiting for SSH banner...", "SUCCESS")
                // In Direct Payload WITHOUT an HTTP proxy, we do NOT inject an HTTP payload upfront.
                // Doing so would corrupt the SSH protocol handshake and cause "Connection closed by foreign host".
                // We let JSch handle the native SSH handshake.
            }
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    private fun readLineFromStream(inputStream: InputStream): String {
        val sb = StringBuilder()
        var c: Int
        while (true) {
            c = inputStream.read()
            if (c == -1) break
            if (c == '\n'.code) {
                if (sb.isNotEmpty() && sb.last() == '\r') {
                    sb.deleteCharAt(sb.length - 1)
                }
                break
            }
            sb.append(c.toChar())
        }
        return sb.toString()
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
