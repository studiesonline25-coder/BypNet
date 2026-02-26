package com.bypnet.app.tunnel.tun2socks

import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Tun2Socks — bridges the TUN interface to a SOCKS5 proxy.
 *
 * The flow:
 * 1. Read raw IP packets from the TUN file descriptor
 * 2. Parse IPv4 + TCP/UDP headers
 * 3. For each new TCP connection (SYN): create a SOCKS5 connection through the proxy
 * 4. For data packets: forward through the existing SOCKS5 connection
 * 5. For DNS (UDP 53): relay through a protected UDP socket
 * 6. Response packets are constructed with proper IP+TCP headers and written back to TUN
 *
 * This is the equivalent of `badvpn-tun2socks` but implemented in pure Kotlin.
 */
class Tun2Socks(
    private val tunFd: ParcelFileDescriptor,
    private val socksPort: Int,
    private val vpnService: VpnService,
    private val dnsServer: String = "8.8.8.8"
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connections = ConcurrentHashMap<ConnectionKey, TcpSession>()
    private lateinit var tunOutput: FileOutputStream
    private lateinit var dnsRelay: DnsRelay

    // Traffic stats
    @Volatile var totalUpload: Long = 0; private set
    @Volatile var totalDownload: Long = 0; private set

    private var running = false

    /**
     * Start the tun2socks bridge. Blocks the calling coroutine.
     */
    fun start() {
        running = true
        val tunInput = FileInputStream(tunFd.fileDescriptor)
        tunOutput = FileOutputStream(tunFd.fileDescriptor)

        dnsRelay = DnsRelay(
            protectSocket = { vpnService.protect(it); true },
            protectDgram = { vpnService.protect(it); true },
            tunWriter = tunOutput,
            dnsServer = dnsServer,
            scope = scope
        )

        val buffer = ByteArray(32767)

        try {
            while (running) {
                val len = tunInput.read(buffer)
                if (len <= 0) {
                    if (len < 0) break
                    continue
                }

                // Copy the packet data (buffer is reused)
                val packetData = buffer.copyOfRange(0, len)
                processPacket(packetData, len)
            }
        } catch (e: Exception) {
            if (running) {
                // Unexpected error
            }
        }
    }

    /**
     * Stop the tun2socks bridge and clean up all connections.
     */
    fun stop() {
        running = false
        scope.cancel()
        connections.values.forEach { it.close() }
        connections.clear()
    }

    /**
     * Process a single IP packet from TUN.
     */
    private fun processPacket(data: ByteArray, len: Int) {
        val ip = IpPacket(data, len)
        if (!ip.isValid) return

        when {
            ip.isTcp -> handleTcp(ip, data, len)
            ip.isUdp -> handleUdp(ip, data, len)
            // ICMP and other protocols are silently dropped
        }
    }

    /**
     * Handle a TCP packet.
     */
    private fun handleTcp(ip: IpPacket, data: ByteArray, len: Int) {
        val tcp = TcpPacket(data, ip.headerLength, len)
        val key = ConnectionKey(ip.sourceIp, tcp.sourcePort, ip.destIp, tcp.destPort)

        if (tcp.isSyn && !tcp.isAck) {
            // New connection — SYN
            // Close any existing session with the same key
            connections.remove(key)?.close()

            val session = TcpSession(
                srcIp = ip.sourceIp,
                srcPort = tcp.sourcePort,
                dstIp = ip.destIp,
                dstPort = tcp.destPort,
                socksPort = socksPort,
                protectSocket = { sock -> vpnService.protect(sock); true },
                tunWriter = tunOutput,
                scope = scope,
                onClose = { k -> connections.remove(k) },
                statsCallback = { up, down ->
                    totalUpload += up
                    totalDownload += down
                }
            )
            connections[key] = session
            session.handleSyn(tcp.sequenceNumber)

        } else if (tcp.isRst) {
            // RST — tear down
            connections.remove(key)?.close()

        } else if (tcp.isFin) {
            // FIN — graceful close
            connections[key]?.handleFin(tcp.sequenceNumber)

        } else if (tcp.isAck) {
            val session = connections[key] ?: return
            val payload = tcp.getPayload()

            if (payload != null && payload.isNotEmpty()) {
                // Data packet
                session.handleData(payload, tcp.sequenceNumber)
            } else {
                // Pure ACK
                session.handleAck(tcp.sequenceNumber, tcp.ackNumber)
            }
        }
    }

    /**
     * Handle a UDP packet (only DNS on port 53).
     */
    private fun handleUdp(ip: IpPacket, data: ByteArray, len: Int) {
        val udpOffset = ip.headerLength
        if (len < udpOffset + 8) return

        val srcPort = ((data[udpOffset].toInt() and 0xFF) shl 8) or (data[udpOffset + 1].toInt() and 0xFF)
        val dstPort = ((data[udpOffset + 2].toInt() and 0xFF) shl 8) or (data[udpOffset + 3].toInt() and 0xFF)
        val udpLength = ((data[udpOffset + 4].toInt() and 0xFF) shl 8) or (data[udpOffset + 5].toInt() and 0xFF)

        // Only handle DNS (port 53)
        if (dstPort != 53) return

        val payloadOffset = udpOffset + 8
        val payloadLength = udpLength - 8
        if (payloadLength <= 0 || payloadOffset + payloadLength > len) return

        val payload = data.copyOfRange(payloadOffset, payloadOffset + payloadLength)
        dnsRelay.handleDnsQuery(ip.sourceIp, srcPort, ip.destIp, dstPort, payload)
    }
}
