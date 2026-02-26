package com.bypnet.app.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global session manager holding the active configuration state.
 *
 * When a locked .byp config is imported, [configLocked] is true.
 * The UI will hide/mask all sensitive fields so the user cannot see
 * the credentials — but the VPN service can still read the real values
 * from here to establish the connection.
 */
object SessionManager {
    // Payload & Proxy (editable unless locked)
    var payload by mutableStateOf("GET / HTTP/1.1[crlf]Host: [sni][crlf]Connection: Keep-Alive[crlf][crlf]")
    var proxyHost by mutableStateOf("")
    var proxyPort by mutableStateOf("8080")

    // Connection method: SSH, SSH+SSL, SSL/TLS, WebSocket, V2Ray
    var connectionMethod by mutableStateOf("SSH")

    // SSH config (editable unless locked)
    var sshConfig by mutableStateOf("") // ip:port@user:pass

    // SNI and DNS
    var sni by mutableStateOf("")
    var dns1 by mutableStateOf("8.8.8.8")
    var dns2 by mutableStateOf("8.8.4.4")

    // Lock state — when true, the UI hides all sensitive fields
    var configLocked by mutableStateOf(false)
    var configName by mutableStateOf("")

    /**
     * Load a full BypConfig into the session.
     * If the config is locked, fields are populated internally
     * but the UI will mask them.
     */
    fun loadConfig(config: BypConfig) {
        configName = config.name
        configLocked = config.locked

        // Build unified SSH string
        val host = config.server.host
        val port = config.server.port
        val user = config.auth.username
        val pass = config.auth.password
        sshConfig = if (host.isNotEmpty()) "$host:$port@$user:$pass" else ""

        payload = config.payload.ifEmpty { payload }
        sni = config.sni
        proxyHost = config.proxy.host
        proxyPort = if (config.proxy.port > 0) config.proxy.port.toString() else "8080"
        dns1 = config.dns.primary.ifEmpty { "8.8.8.8" }
        dns2 = config.dns.secondary.ifEmpty { "8.8.4.4" }
    }

    /**
     * Clear the session and unlock.
     */
    fun clear() {
        configLocked = false
        configName = ""
        sshConfig = ""
        payload = "GET / HTTP/1.1[crlf]Host: [sni][crlf]Connection: Keep-Alive[crlf][crlf]"
        proxyHost = ""
        proxyPort = "8080"
        sni = ""
        dns1 = "8.8.8.8"
        dns2 = "8.8.4.4"
    }
}
