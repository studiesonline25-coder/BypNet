package com.bypnet.app.tunnel.tun2socks

import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket

/**
 * Relays DNS (UDP port 53) queries through the SOCKS5 proxy.
 *
 * Since SOCKS5 doesn't natively support UDP well, we resolve DNS
 * by opening a direct UDP socket (protected from VPN) to the
 * configured DNS server.
 */
class DnsRelay(
    private val protectSocket: (Socket) -> Boolean,
    private val protectDgram: (DatagramSocket) -> Boolean,
    private val tunWriter: FileOutputStream,
    private val dnsServer: String = "8.8.8.8",
    private val scope: CoroutineScope
) {
    /**
     * Handle an incoming UDP DNS packet from TUN.
     *
     * @param srcIp     Original source IP (the local app)
     * @param srcPort   Original source port
     * @param dstIp     Destination IP (usually a DNS server)
     * @param dstPort   Destination port (53)
     * @param payload   The raw DNS query bytes
     */
    fun handleDnsQuery(
        srcIp: Int, srcPort: Int,
        dstIp: Int, dstPort: Int,
        payload: ByteArray
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val dnsSocket = DatagramSocket()
                protectDgram(dnsSocket)
                dnsSocket.soTimeout = 5000

                // Forward query to real DNS server
                val serverAddr = InetAddress.getByName(dnsServer)
                val request = DatagramPacket(payload, payload.size, serverAddr, 53)
                dnsSocket.send(request)

                // Receive response
                val responseBuf = ByteArray(4096)
                val response = DatagramPacket(responseBuf, responseBuf.size)
                dnsSocket.receive(response)
                dnsSocket.close()

                val dnsResponse = responseBuf.copyOfRange(0, response.length)

                // Build UDP response packet and write to TUN
                val udpPacket = buildUdpPacket(
                    srcIp = dstIp, srcPort = dstPort,  // Swap: response FROM dns TO local
                    dstIp = srcIp, dstPort = srcPort,
                    payload = dnsResponse
                )
                synchronized(tunWriter) {
                    tunWriter.write(udpPacket)
                    tunWriter.flush()
                }
            } catch (_: Exception) {
                // DNS query failed silently — app will retry
            }
        }
    }

    companion object {
        /**
         * Build a complete IP+UDP packet.
         */
        fun buildUdpPacket(
            srcIp: Int, srcPort: Int,
            dstIp: Int, dstPort: Int,
            payload: ByteArray
        ): ByteArray {
            val udpHeaderLen = 8
            val udpLength = udpHeaderLen + payload.size
            val totalLength = 20 + udpLength

            val packet = ByteArray(totalLength)

            // IP header
            val ipHeader = IpPacket.buildHeader(totalLength, IpPacket.PROTO_UDP, srcIp, dstIp)
            System.arraycopy(ipHeader, 0, packet, 0, 20)

            // UDP header at offset 20
            val u = 20
            IpPacket.writeU16(packet, u, srcPort)
            IpPacket.writeU16(packet, u + 2, dstPort)
            IpPacket.writeU16(packet, u + 4, udpLength)
            IpPacket.writeU16(packet, u + 6, 0) // Checksum = 0 (optional for IPv4 UDP)

            // Payload
            System.arraycopy(payload, 0, packet, u + udpHeaderLen, payload.size)

            return packet
        }
    }
}
