package com.bypnet.app.tunnel.tun2socks

/**
 * TCP segment parser and builder.
 *
 * TCP Header (20 bytes minimum):
 * ┌──────────────────┬──────────────────┐
 * │ Source Port       │ Dest Port        │
 * ├──────────────────┴──────────────────┤
 * │ Sequence Number (32 bit)            │
 * ├─────────────────────────────────────┤
 * │ Acknowledgment Number (32 bit)      │
 * ├──────┬────────┬─────────────────────┤
 * │ Offs │ Flags  │ Window Size         │
 * ├──────┴────────┼─────────────────────┤
 * │ Checksum      │ Urgent Pointer      │
 * └───────────────┴─────────────────────┘
 */
class TcpPacket(
    private val data: ByteArray,
    private val ipHeaderLen: Int,
    private val packetLen: Int
) {
    private val off = ipHeaderLen

    val sourcePort: Int get() = readU16(off)
    val destPort: Int get() = readU16(off + 2)
    val sequenceNumber: Long get() = readU32(off + 4)
    val ackNumber: Long get() = readU32(off + 8)
    val dataOffset: Int get() = ((data[off + 12].toInt() ushr 4) and 0x0F) * 4
    val flags: Int get() = data[off + 13].toInt() and 0x3F
    val windowSize: Int get() = readU16(off + 14)

    val isSyn: Boolean get() = (flags and FLAG_SYN) != 0
    val isAck: Boolean get() = (flags and FLAG_ACK) != 0
    val isFin: Boolean get() = (flags and FLAG_FIN) != 0
    val isRst: Boolean get() = (flags and FLAG_RST) != 0
    val isPsh: Boolean get() = (flags and FLAG_PSH) != 0

    /** TCP payload offset within the full packet */
    val payloadOffset: Int get() = ipHeaderLen + dataOffset
    val payloadLength: Int get() = packetLen - payloadOffset

    fun getPayload(): ByteArray? {
        if (payloadLength <= 0) return null
        return data.copyOfRange(payloadOffset, payloadOffset + payloadLength)
    }

    private fun readU16(offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun readU32(offset: Int): Long =
        (((data[offset].toLong() and 0xFF) shl 24) or
         ((data[offset + 1].toLong() and 0xFF) shl 16) or
         ((data[offset + 2].toLong() and 0xFF) shl 8) or
         (data[offset + 3].toLong() and 0xFF)) and 0xFFFFFFFFL

    companion object {
        const val FLAG_FIN = 0x01
        const val FLAG_SYN = 0x02
        const val FLAG_RST = 0x04
        const val FLAG_PSH = 0x08
        const val FLAG_ACK = 0x10
        const val FLAG_URG = 0x20

        /**
         * Build a complete TCP+IP response packet.
         *
         * @param srcIp  Source IP (the remote server from TUN's perspective)
         * @param srcPort Source port
         * @param dstIp  Dest IP (the local device)
         * @param dstPort Dest port
         * @param seqNum Our sequence number
         * @param ackNum Acknowledgment number
         * @param flags  TCP flags (SYN, ACK, FIN, RST, PSH)
         * @param payload Optional data payload
         * @return Complete IP+TCP packet ready to write to TUN
         */
        fun buildPacket(
            srcIp: Int, srcPort: Int,
            dstIp: Int, dstPort: Int,
            seqNum: Long, ackNum: Long,
            flags: Int,
            payload: ByteArray? = null,
            windowSize: Int = 65535
        ): ByteArray {
            val tcpHeaderLen = 20
            val payloadLen = payload?.size ?: 0
            val totalLen = 20 + tcpHeaderLen + payloadLen // IP header + TCP header + payload

            // Build IP header
            val packet = ByteArray(totalLen)
            val ipHeader = IpPacket.buildHeader(totalLen, IpPacket.PROTO_TCP, srcIp, dstIp)
            System.arraycopy(ipHeader, 0, packet, 0, 20)

            // Build TCP header at offset 20
            val t = 20 // TCP header start
            IpPacket.writeU16(packet, t, srcPort)
            IpPacket.writeU16(packet, t + 2, dstPort)

            // Sequence number (32-bit)
            packet[t + 4] = ((seqNum ushr 24) and 0xFF).toByte()
            packet[t + 5] = ((seqNum ushr 16) and 0xFF).toByte()
            packet[t + 6] = ((seqNum ushr 8) and 0xFF).toByte()
            packet[t + 7] = (seqNum and 0xFF).toByte()

            // Ack number (32-bit)
            packet[t + 8] = ((ackNum ushr 24) and 0xFF).toByte()
            packet[t + 9] = ((ackNum ushr 16) and 0xFF).toByte()
            packet[t + 10] = ((ackNum ushr 8) and 0xFF).toByte()
            packet[t + 11] = (ackNum and 0xFF).toByte()

            // Data offset (5 = 20 bytes) + reserved
            packet[t + 12] = (5 shl 4).toByte()
            // Flags
            packet[t + 13] = flags.toByte()
            // Window size
            IpPacket.writeU16(packet, t + 14, windowSize)
            // Checksum = 0 initially
            IpPacket.writeU16(packet, t + 16, 0)
            // Urgent pointer = 0
            IpPacket.writeU16(packet, t + 18, 0)

            // Copy payload
            if (payload != null && payloadLen > 0) {
                System.arraycopy(payload, 0, packet, t + tcpHeaderLen, payloadLen)
            }

            // Calculate TCP checksum with pseudo-header
            val tcpChecksum = calculateTcpChecksum(packet, srcIp, dstIp, tcpHeaderLen + payloadLen)
            IpPacket.writeU16(packet, t + 16, tcpChecksum)

            return packet
        }

        /**
         * TCP checksum includes a pseudo-header:
         * srcIP(4) + dstIP(4) + zero(1) + protocol(1) + TCP length(2)
         */
        private fun calculateTcpChecksum(packet: ByteArray, srcIp: Int, dstIp: Int, tcpLength: Int): Int {
            var sum = 0L

            // Pseudo-header
            sum += ((srcIp ushr 16) and 0xFFFF).toLong()
            sum += (srcIp and 0xFFFF).toLong()
            sum += ((dstIp ushr 16) and 0xFFFF).toLong()
            sum += (dstIp and 0xFFFF).toLong()
            sum += IpPacket.PROTO_TCP.toLong()
            sum += tcpLength.toLong()

            // TCP header + data (starting at offset 20)
            val tcpOffset = 20
            var i = 0
            val len = if (tcpLength % 2 == 0) tcpLength else tcpLength - 1
            while (i < len) {
                sum += ((packet[tcpOffset + i].toInt() and 0xFF) shl 8) or
                       (packet[tcpOffset + i + 1].toInt() and 0xFF)
                i += 2
            }
            if (tcpLength % 2 != 0) {
                sum += (packet[tcpOffset + tcpLength - 1].toInt() and 0xFF) shl 8
            }

            while (sum ushr 16 != 0L) {
                sum = (sum and 0xFFFF) + (sum ushr 16)
            }
            return (sum.toInt().inv()) and 0xFFFF
        }
    }
}
