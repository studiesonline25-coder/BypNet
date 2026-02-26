package com.bypnet.app.tunnel.tun2socks

import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.io.FileOutputStream

/**
 * Manages a single TCP connection through the SOCKS5 proxy.
 *
 * Lifecycle:
 *   SYN received → connect through SOCKS5 → send SYN-ACK → ESTABLISHED
 *   Data received → forward to SOCKS → read response → send back via TUN
 *   FIN received → close SOCKS → send FIN-ACK
 */
class TcpSession(
    val srcIp: Int,
    val srcPort: Int,
    val dstIp: Int,
    val dstPort: Int,
    private val socksPort: Int,
    private val protectSocket: (Socket) -> Boolean,
    private val tunWriter: FileOutputStream,
    private val scope: CoroutineScope,
    private val onClose: (ConnectionKey) -> Unit,
    private val statsCallback: (upload: Long, download: Long) -> Unit
) {
    enum class State { CONNECTING, SYN_RECEIVED, ESTABLISHED, FIN_WAIT, CLOSED }

    var state: State = State.CONNECTING
        private set

    // TCP sequence numbers
    var mySeqNum: Long = (System.nanoTime() and 0xFFFFFFFFL) // Our ISN
        private set
    var theirSeqNum: Long = 0 // Expected next seq from client
        private set

    private var socksSocket: Socket? = null
    private var remoteIn: InputStream? = null
    private var remoteOut: OutputStream? = null
    private var downstreamJob: Job? = null

    val key: ConnectionKey get() = ConnectionKey(srcIp, srcPort, dstIp, dstPort)

    /**
     * Handle a SYN packet: connect through SOCKS5 and send SYN-ACK.
     */
    fun handleSyn(clientSeqNum: Long) {
        theirSeqNum = clientSeqNum + 1 // SYN consumes 1 sequence number
        state = State.SYN_RECEIVED

        scope.launch(Dispatchers.IO) {
            try {
                val sock = Socket()
                protectSocket(sock)

                // Connect to local SOCKS5 proxy
                sock.connect(InetSocketAddress("127.0.0.1", socksPort), 10000)
                sock.tcpNoDelay = true

                val sOut = sock.getOutputStream()
                val sIn = sock.getInputStream()

                // SOCKS5 greeting: version=5, 1 method, no-auth
                sOut.write(byteArrayOf(0x05, 0x01, 0x00))
                sOut.flush()

                val greetReply = ByteArray(2)
                readFully(sIn, greetReply)
                if (greetReply[0] != 0x05.toByte() || greetReply[1] != 0x00.toByte()) {
                    throw Exception("SOCKS5 auth failed")
                }

                // SOCKS5 CONNECT request with IPv4 address
                val dstIpBytes = byteArrayOf(
                    ((dstIp ushr 24) and 0xFF).toByte(),
                    ((dstIp ushr 16) and 0xFF).toByte(),
                    ((dstIp ushr 8) and 0xFF).toByte(),
                    (dstIp and 0xFF).toByte()
                )
                val connectReq = byteArrayOf(
                    0x05, 0x01, 0x00, 0x01,
                    dstIpBytes[0], dstIpBytes[1], dstIpBytes[2], dstIpBytes[3],
                    ((dstPort ushr 8) and 0xFF).toByte(),
                    (dstPort and 0xFF).toByte()
                )
                sOut.write(connectReq)
                sOut.flush()

                val connReply = ByteArray(10)
                readFully(sIn, connReply)
                if (connReply[1] != 0x00.toByte()) {
                    throw Exception("SOCKS5 connect rejected: ${connReply[1]}")
                }

                // SOCKS5 connected!
                socksSocket = sock
                remoteIn = sIn
                remoteOut = sOut

                // Send SYN-ACK back to TUN
                sendTcpToTun(
                    TcpPacket.FLAG_SYN or TcpPacket.FLAG_ACK,
                    null
                )
                mySeqNum++ // SYN consumes 1 sequence number
                state = State.ESTABLISHED

                // Start reading responses from the remote server
                startDownstream()

            } catch (e: Exception) {
                // Send RST
                sendRst()
                close()
            }
        }
    }

    /**
     * Handle incoming data from the client (TUN → SOCKS).
     */
    fun handleData(payload: ByteArray, clientSeqNum: Long) {
        if (state != State.ESTABLISHED) return

        theirSeqNum = clientSeqNum + payload.size

        scope.launch(Dispatchers.IO) {
            try {
                remoteOut?.write(payload)
                remoteOut?.flush()
                statsCallback(payload.size.toLong(), 0)

                // Send ACK
                sendTcpToTun(TcpPacket.FLAG_ACK, null)
            } catch (e: Exception) {
                sendRst()
                close()
            }
        }
    }

    /**
     * Handle ACK from the client.
     */
    fun handleAck(clientSeqNum: Long, clientAckNum: Long) {
        // Just update tracking — ACKs confirm our data was received
        if (state == State.FIN_WAIT) {
            close()
        }
    }

    /**
     * Handle FIN from the client.
     */
    fun handleFin(clientSeqNum: Long) {
        theirSeqNum = clientSeqNum + 1

        // Send FIN-ACK
        sendTcpToTun(TcpPacket.FLAG_FIN or TcpPacket.FLAG_ACK, null)
        mySeqNum++
        close()
    }

    /**
     * Read data from the remote server (SOCKS) and send back to TUN.
     */
    private fun startDownstream() {
        downstreamJob = scope.launch(Dispatchers.IO) {
            val buf = ByteArray(4096)
            try {
                while (isActive && state == State.ESTABLISHED) {
                    val n = remoteIn?.read(buf) ?: -1
                    if (n < 0) break
                    if (n > 0) {
                        val payload = buf.copyOfRange(0, n)
                        sendTcpToTun(TcpPacket.FLAG_ACK or TcpPacket.FLAG_PSH, payload)
                        mySeqNum += n
                        statsCallback(0, n.toLong())
                    }
                }
            } catch (_: Exception) { }

            // Remote closed — send FIN
            if (state == State.ESTABLISHED) {
                state = State.FIN_WAIT
                sendTcpToTun(TcpPacket.FLAG_FIN or TcpPacket.FLAG_ACK, null)
                mySeqNum++
            }
        }
    }

    /**
     * Build a TCP+IP packet and write it to the TUN device.
     */
    private fun sendTcpToTun(flags: Int, payload: ByteArray?) {
        try {
            val packet = TcpPacket.buildPacket(
                srcIp = dstIp, srcPort = dstPort,   // Swap: response goes FROM remote TO local
                dstIp = srcIp, dstPort = srcPort,
                seqNum = mySeqNum,
                ackNum = theirSeqNum,
                flags = flags,
                payload = payload
            )
            synchronized(tunWriter) {
                tunWriter.write(packet)
                tunWriter.flush()
            }
        } catch (_: Exception) { }
    }

    private fun sendRst() {
        sendTcpToTun(TcpPacket.FLAG_RST or TcpPacket.FLAG_ACK, null)
    }

    fun close() {
        if (state == State.CLOSED) return
        state = State.CLOSED
        downstreamJob?.cancel()
        try { socksSocket?.close() } catch (_: Exception) {}
        socksSocket = null
        remoteIn = null
        remoteOut = null
        onClose(key)
    }

    private fun readFully(input: InputStream, buf: ByteArray) {
        var offset = 0
        while (offset < buf.size) {
            val n = input.read(buf, offset, buf.size - offset)
            if (n < 0) throw Exception("Stream ended")
            offset += n
        }
    }
}

/** Unique key for a TCP connection 4-tuple */
data class ConnectionKey(val srcIp: Int, val srcPort: Int, val dstIp: Int, val dstPort: Int)
