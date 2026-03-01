package com.bypnet.app.proxy

import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * ShareNetProxy runs a local HTTP/SOCKS5 proxy on 0.0.0.0:7071.
 * This allows other devices connected to the Android Hotspot to use
 * the VPN connection by setting their WiFi Proxy settings.
 */
object ShareNetProxy {
    private var serverSocket: ServerSocket? = null
    private var proxyJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isRunning = false
        private set

    fun start(port: Int = 7071): Boolean {
        if (isRunning) return true
        return try {
            serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            isRunning = true
            proxyJob = scope.launch {
                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (isActive) break
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stop() {
        isRunning = false
        proxyJob?.cancel()
        proxyJob = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    private suspend fun handleClient(client: Socket) = withContext(Dispatchers.IO) {
        try {
            val rawInput = client.getInputStream()
            val output = client.getOutputStream()
            val input = PushbackInputStream(rawInput, 1)

            val firstByte = input.read()
            if (firstByte == -1) {
                client.close()
                return@withContext
            }

            if (firstByte == 0x05) {
                // SOCKS5
                handleSocks5(client, input, output)
            } else {
                // HTTP
                input.unread(firstByte)
                handleHttp(client, input, output)
            }
        } catch (e: Exception) {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private suspend fun handleSocks5(client: Socket, input: InputStream, output: OutputStream) = coroutineScope {
        try {
            val nMethods = input.read()
            val methods = ByteArray(nMethods)
            input.read(methods)

            // No auth
            output.write(byteArrayOf(0x05, 0x00))
            output.flush()

            val reqVer = input.read()
            val cmd = input.read()
            val rsv = input.read()
            val atyp = input.read()

            if (cmd != 0x01) { // CONNECT only
                output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
                return@coroutineScope
            }

            val destHost: String = when (atyp) {
                0x01 -> {
                    val addr = ByteArray(4)
                    input.read(addr)
                    "${addr[0].toInt() and 0xFF}.${addr[1].toInt() and 0xFF}.${addr[2].toInt() and 0xFF}.${addr[3].toInt() and 0xFF}"
                }
                0x03 -> {
                    val len = input.read()
                    val domain = ByteArray(len)
                    input.read(domain)
                    String(domain)
                }
                0x04 -> {
                    val addr = ByteArray(16)
                    input.read(addr)
                    InetAddress.getByAddress(addr).hostAddress ?: ""
                }
                else -> return@coroutineScope
            }

            val portHi = input.read()
            val portLo = input.read()
            val destPort = (portHi shl 8) or portLo

            var destSocket: Socket? = null
            try {
                destSocket = Socket(destHost, destPort)
                output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
                pipe(input, output, destSocket.getInputStream(), destSocket.getOutputStream())
            } catch (e: Exception) {
                output.write(byteArrayOf(0x05, 0x01, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
            } finally {
                destSocket?.close()
            }
        } catch (e: Exception) {}
    }

    private suspend fun handleHttp(client: Socket, input: InputStream, output: OutputStream) = coroutineScope {
        try {
            val reqLine = readLine(input)
            if (reqLine.isEmpty()) return@coroutineScope

            val parts = reqLine.split(" ")
            if (parts.size < 3) return@coroutineScope

            val method = parts[0]
            val url = parts[1]

            if (method.equals("CONNECT", ignoreCase = true)) {
                val hostParts = url.split(":")
                val host = hostParts[0]
                val port = if (hostParts.size > 1) hostParts[1].toIntOrNull() ?: 443 else 443

                // Consume headers
                while (true) {
                    val line = readLine(input)
                    if (line.isEmpty()) break
                }

                var destSocket: Socket? = null
                try {
                    destSocket = Socket(host, port)
                    output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                    output.flush()
                    pipe(input, output, destSocket.getInputStream(), destSocket.getOutputStream())
                } catch (e: Exception) {
                    output.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray())
                    output.flush()
                } finally {
                    destSocket?.close()
                }
            } else {
                // Very basic HTTP intercept (not fully compliant, but works for simple GET/POST)
                output.write("HTTP/1.1 501 Not Implemented\r\n\r\n".toByteArray())
                output.flush()
            }
        } catch (e: Exception) {}
    }

    private fun readLine(input: InputStream): String {
        val sb = StringBuilder()
        var c: Int
        while (true) {
            c = input.read()
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

    private suspend fun pipe(in1: InputStream, out1: OutputStream, in2: InputStream, out2: OutputStream) = coroutineScope {
        val up = launch {
            try {
                val buf = ByteArray(16384)
                while (isActive) {
                    val n = in1.read(buf)
                    if (n < 0) break
                    out2.write(buf, 0, n)
                    out2.flush()
                }
            } catch (_: Exception) {}
        }
        val down = launch {
            try {
                val buf = ByteArray(16384)
                while (isActive) {
                    val n = in2.read(buf)
                    if (n < 0) break
                    out1.write(buf, 0, n)
                    out1.flush()
                }
            } catch (_: Exception) {}
        }
        while (up.isActive && down.isActive) {
            delay(100)
        }
        up.cancel()
        down.cancel()
    }
}
