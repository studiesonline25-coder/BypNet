package com.bypnet.app.tunnel.payload

/**
 * Processes payload templates by replacing variables with actual values.
 *
 * Supported variables:
 * - [host]    → Server host
 * - [port]    → Server port
 * - [sni]     → SNI host
 * - [cookie]  → Extracted cookies
 * - [crlf]    → \r\n
 * - [cr]      → \r
 * - [lf]      → \n
 * - [tab]     → \t
 * - [protocol] → http or https
 * - [netdata] → Network data placeholder (for HTTP Custom compat)
 */
object PayloadProcessor {

    fun process(
        template: String,
        host: String,
        port: Int,
        sni: String = "",
        cookies: String = "",
        customVariables: Map<String, String> = emptyMap()
    ): String {
        if (template.isBlank()) return ""

        var result = template

        // Standard variables
        result = result.replace("[host]", host, ignoreCase = true)
        result = result.replace("[host_port]", "$host:$port", ignoreCase = true)
        result = result.replace("[port]", port.toString(), ignoreCase = true)
        result = result.replace("[sni]", sni.ifEmpty { host }, ignoreCase = true)
        result = result.replace("[cookie]", cookies, ignoreCase = true)

        // Control characters
        result = result.replace("[crlf]", "\r\n", ignoreCase = true)
        result = result.replace("[cr]", "\r", ignoreCase = true)
        result = result.replace("[lf]", "\n", ignoreCase = true)
        result = result.replace("[tab]", "\t", ignoreCase = true)

        // Protocol
        result = result.replace("[protocol]", if (port == 443) "https" else "http", ignoreCase = true)

        // Network data placeholder
        result = result.replace("[netdata]", "", ignoreCase = true)

        // Custom user-defined variables
        customVariables.forEach { (key, value) ->
            result = result.replace("[$key]", value, ignoreCase = true)
        }

        return result
    }

    /**
     * Validates a payload template and returns any unresolved variables.
     */
    fun validate(template: String): List<String> {
        val regex = Regex("\\[([^\\]]+)\\]")
        val knownVariables = setOf(
            "host", "port", "sni", "cookie", "crlf", "cr", "lf",
            "tab", "protocol", "netdata"
        )
        return regex.findAll(template)
            .map { it.groupValues[1].lowercase() }
            .filter { it !in knownVariables }
            .toList()
    }

    /**
     * Generate a default HTTP CONNECT payload.
     */
    fun defaultConnectPayload(): String {
        return "CONNECT [host]:[port] HTTP/1.1[crlf]Host: [sni][crlf][crlf]"
    }

    /**
     * Generate a default GET payload.
     */
    fun defaultGetPayload(): String {
        return "GET / HTTP/1.1[crlf]Host: [sni][crlf]Connection: Keep-Alive[crlf][crlf]"
    }

    /**
     * Generate a payload with cookie injection.
     */
    fun defaultCookiePayload(): String {
        return "GET / HTTP/1.1[crlf]Host: [sni][crlf]Cookie: [cookie][crlf]Connection: Keep-Alive[crlf][crlf]"
    }
}
