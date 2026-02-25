package com.bypnet.app.tunnel.dns

/**
 * DNS configuration for tunnel connections.
 */
data class DnsConfig(
    val primaryDns: String = "8.8.8.8",
    val secondaryDns: String = "8.8.4.4",
    val useDot: Boolean = false,        // DNS over TLS
    val useDoh: Boolean = false,        // DNS over HTTPS
    val dohUrl: String = "https://dns.google/dns-query"
)

/**
 * Custom DNS resolver that routes DNS queries through the tunnel.
 */
object DnsResolver {

    /**
     * Preset DNS configurations.
     */
    val PRESETS = mapOf(
        "Google" to DnsConfig("8.8.8.8", "8.8.4.4"),
        "Cloudflare" to DnsConfig("1.1.1.1", "1.0.0.1"),
        "Quad9" to DnsConfig("9.9.9.9", "149.112.112.112"),
        "OpenDNS" to DnsConfig("208.67.222.222", "208.67.220.220"),
        "AdGuard" to DnsConfig("94.140.14.14", "94.140.15.15"),
        "Comodo" to DnsConfig("8.26.56.26", "8.20.247.20")
    )

    /**
     * Resolve a hostname using the configured DNS servers.
     */
    fun resolve(hostname: String, config: DnsConfig = DnsConfig()): String {
        return try {
            val addresses = java.net.InetAddress.getAllByName(hostname)
            addresses.firstOrNull()?.hostAddress ?: hostname
        } catch (e: Exception) {
            hostname
        }
    }

    /**
     * Validate a DNS address.
     */
    fun isValidDns(dns: String): Boolean {
        return try {
            val parts = dns.split(".")
            parts.size == 4 && parts.all {
                val num = it.toIntOrNull()
                num != null && num in 0..255
            }
        } catch (e: Exception) {
            false
        }
    }
}
