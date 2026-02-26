package com.bypnet.app.tunnel.ssh

import com.jcraft.jsch.ChannelDirectTCPIP
import com.jcraft.jsch.Session
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

/**
 * Lightweight SOCKS5 proxy server that routes connections through
 * JSch SSH direct-tcpip channels.
 *
 * This implements SSH dynamic port forwarding (equivalent to ssh -D).
 * Each incoming SOCKS5 connection is tunneled through the SSH session.
 */
class LocalSocksProxy(private val session: Session) {

    private var serverSocket: ServerSocket? = null
    private var proxyJob: Job? = null
    val port: Int get() = serverSocket?.localPort ?: 0

    /**
     * Start the SOCKS5 proxy on a random available port.
     */
    fun start(scope: CoroutineScope, logFn: (String, String) -> Unit) {
        serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        logFn("SOCKS5 proxy started on 127.0.0.1:${serverSocket!!.localPort}", "SUCCESS")

        proxyJob = scope.launch(Dispatchers.IO) {
            while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                try {
                    val client = serverSocket!!.accept()
                    launch {
                        handleClient(client, logFn)
                    }
                } catch (e: IOException) {
                    if (isActive) break
                }
            }
        }
    }

    /**
     * Stop the proxy.
     */
    fun stop() {
        proxyJob?.cancel()
        proxyJob = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    /**
     * Handle a single SOCKS5 client connection.
     */
    private suspend fun handleClient(
        client: java.net.Socket,
        logFn: (String, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val input = client.getInputStream()
            val output = client.getOutputStream()

            // ── SOCKS5 handshake ──
            // Client greeting: VER | NMETHODS | METHODS
            val ver = input.read()
            if (ver != 0x05) {
                client.close()
                return@withContext
            }
            val nMethods = input.read()
            val methods = ByteArray(nMethods)
            input.read(methods)

            // Reply: no auth required
            output.write(byteArrayOf(0x05, 0x00))
            output.flush()

            // ── SOCKS5 connect request ──
            // VER | CMD | RSV | ATYP | DST.ADDR | DST.PORT
            val reqVer = input.read()     // 0x05
            val cmd = input.read()         // 0x01 = CONNECT
            val rsv = input.read()         // reserved
            val atyp = input.read()        // address type

            if (cmd != 0x01) {
                // Only CONNECT is supported
                output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
                client.close()
                return@withContext
            }

            val destHost: String
            when (atyp) {
                0x01 -> { // IPv4
                    val addr = ByteArray(4)
                    input.read(addr)
                    destHost = "${addr[0].toInt() and 0xFF}.${addr[1].toInt() and 0xFF}.${addr[2].toInt() and 0xFF}.${addr[3].toInt() and 0xFF}"
                }
                0x03 -> { // Domain name
                    val len = input.read()
                    val domain = ByteArray(len)
                    input.read(domain)
                    destHost = String(domain)
                }
                0x04 -> { // IPv6
                    val addr = ByteArray(16)
                    input.read(addr)
                    destHost = InetAddress.getByAddress(addr).hostAddress ?: ""
                }
                else -> {
                    client.close()
                    return@withContext
                }
            }

            val portHi = input.read()
            val portLo = input.read()
            val destPort = (portHi shl 8) or portLo

            // Open SSH direct-tcpip channel to the destination
            try {
                val channel = session.openChannel("direct-tcpip") as ChannelDirectTCPIP
                channel.setHost(destHost)
                channel.setPort(destPort)
                channel.connect(10000)

                // Send SOCKS5 success reply
                output.write(byteArrayOf(
                    0x05, 0x00, 0x00, 0x01,
                    0, 0, 0, 0,  // BND.ADDR
                    0, 0          // BND.PORT
                ))
                output.flush()

                // Bidirectional pipe: client ↔ SSH channel
                val chIn = channel.inputStream
                val chOut = channel.outputStream

                val upstream = launch {
                    try {
                        val buf = ByteArray(16384)
                        while (isActive) {
                            val n = input.read(buf)
                            if (n < 0) break
                            chOut.write(buf, 0, n)
                            chOut.flush()
                        }
                    } catch (_: Exception) {}
                }

                val downstream = launch {
                    try {
                        val buf = ByteArray(16384)
                        while (isActive) {
                            val n = chIn.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            output.flush()
                        }
                    } catch (_: Exception) {}
                }

                // Wait for either direction to finish
                select(upstream, downstream)
                upstream.cancel()
                downstream.cancel()
                channel.disconnect()
            } catch (e: Exception) {
                // Send SOCKS5 failure reply
                output.write(byteArrayOf(0x05, 0x01, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
            }

            client.close()
        } catch (_: Exception) {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private suspend fun select(a: Job, b: Job) {
        // Wait until either job completes
        while (a.isActive && b.isActive) {
            delay(100)
        }
    }
}
