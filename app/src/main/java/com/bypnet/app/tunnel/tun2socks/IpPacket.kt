package com.bypnet.app.tunnel.tun2socks

/**
 * IPv4 packet parser and builder.
 *
 * IPv4 Header (20 bytes minimum):
 * ┌─────────┬─────────┬──────────────────┬──────────────────────────┐
 * │ Version │  IHL    │  Total Length     │ Identification           │
 * │ (4 bit) │ (4 bit) │  (16 bit)        │ (16 bit)                 │
 * ├─────────┴─────────┼──────────────────┼──────────────────────────┤
 * │ Flags + Fragment   │ TTL (8 bit)      │ Protocol (8 bit)         │
 * ├────────────────────┼──────────────────┼──────────────────────────┤
 * │ Header Checksum    │ Source IP (32 bit)                          │
 * ├────────────────────┼─────────────────────────────────────────────┤
 * │ Destination IP     │                                             │
 * └────────────────────┴─────────────────────────────────────────────┘
 */
class IpPacket(private val data: ByteArray, private val length: Int) {

    val version: Int get() = (data[0].toInt() ushr 4) and 0x0F
    val headerLength: Int get() = (data[0].toInt() and 0x0F) * 4
    val totalLength: Int get() = readU16(2)
    val identification: Int get() = readU16(4)
    val ttl: Int get() = data[8].toInt() and 0xFF
    val protocol: Int get() = data[9].toInt() and 0xFF
    val headerChecksum: Int get() = readU16(10)

    val sourceIp: Int get() = readI32(12)
    val destIp: Int get() = readI32(16)

    val sourceIpString: String get() = intToIpString(sourceIp)
    val destIpString: String get() = intToIpString(destIp)

    /** Payload bytes after the IP header */
    val payloadOffset: Int get() = headerLength
    val payloadLength: Int get() = totalLength - headerLength

    val isValid: Boolean get() = version == 4 && length >= 20 && headerLength >= 20 && totalLength <= length
    val isTcp: Boolean get() = protocol == PROTO_TCP
    val isUdp: Boolean get() = protocol == PROTO_UDP

    /** Make a copy of the raw packet data */
    fun copyData(): ByteArray = data.copyOfRange(0, length)

    private fun readU16(offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun readI32(offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or
        ((data[offset + 1].toInt() and 0xFF) shl 16) or
        ((data[offset + 2].toInt() and 0xFF) shl 8) or
        (data[offset + 3].toInt() and 0xFF)

    companion object {
        const val PROTO_TCP = 6
        const val PROTO_UDP = 17
        const val PROTO_ICMP = 1

        fun intToIpString(ip: Int): String =
            "${(ip ushr 24) and 0xFF}.${(ip ushr 16) and 0xFF}.${(ip ushr 8) and 0xFF}.${ip and 0xFF}"

        fun ipStringToInt(ip: String): Int {
            val parts = ip.split(".")
            return ((parts[0].toInt() and 0xFF) shl 24) or
                   ((parts[1].toInt() and 0xFF) shl 16) or
                   ((parts[2].toInt() and 0xFF) shl 8) or
                   (parts[3].toInt() and 0xFF)
        }

        fun writeU16(buf: ByteArray, offset: Int, value: Int) {
            buf[offset] = ((value ushr 8) and 0xFF).toByte()
            buf[offset + 1] = (value and 0xFF).toByte()
        }

        fun writeI32(buf: ByteArray, offset: Int, value: Int) {
            buf[offset] = ((value ushr 24) and 0xFF).toByte()
            buf[offset + 1] = ((value ushr 16) and 0xFF).toByte()
            buf[offset + 2] = ((value ushr 8) and 0xFF).toByte()
            buf[offset + 3] = (value and 0xFF).toByte()
        }

        /**
         * Calculate IP header checksum.
         * The checksum field itself should be zeroed before calculation.
         */
        fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
            var sum = 0L
            var i = 0
            val len = if (length % 2 == 0) length else length - 1
            while (i < len) {
                sum += ((data[offset + i].toInt() and 0xFF) shl 8) or (data[offset + i + 1].toInt() and 0xFF)
                i += 2
            }
            if (length % 2 != 0) {
                sum += (data[offset + length - 1].toInt() and 0xFF) shl 8
            }
            while (sum ushr 16 != 0L) {
                sum = (sum and 0xFFFF) + (sum ushr 16)
            }
            return (sum.toInt().inv()) and 0xFFFF
        }

        /**
         * Build a minimal IPv4 header.
         * Returns a 20-byte header with checksum computed.
         */
        fun buildHeader(
            totalLength: Int,
            protocol: Int,
            srcIp: Int,
            dstIp: Int,
            identification: Int = 0,
            ttl: Int = 64
        ): ByteArray {
            val header = ByteArray(20)
            header[0] = 0x45.toByte() // Version=4, IHL=5 (20 bytes)
            header[1] = 0x00         // DSCP/ECN
            writeU16(header, 2, totalLength)
            writeU16(header, 4, identification)
            writeU16(header, 6, 0x4000) // Don't Fragment flag
            header[8] = ttl.toByte()
            header[9] = protocol.toByte()
            // Checksum = 0 initially
            writeU16(header, 10, 0)
            writeI32(header, 12, srcIp)
            writeI32(header, 16, dstIp)
            // Compute checksum
            val checksum = calculateChecksum(header, 0, 20)
            writeU16(header, 10, checksum)
            return header
        }
    }
}
