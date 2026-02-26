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

    // SSH config (combined format for home screen)
    var sshConfig by mutableStateOf("") // ip:port@user:pass

    // SSH Settings (separate fields — HC's SSH Settings screen)
    var sshHost by mutableStateOf("")
    var sshPort by mutableStateOf("22")
    var sshUser by mutableStateOf("")
    var sshPass by mutableStateOf("")
    var proxyType by mutableStateOf("Direct") // Direct, SOCKS4, SOCKS5

    // SNI
    var sni by mutableStateOf("")
    var sniBugHost by mutableStateOf("")

    // DNS
    var dns1 by mutableStateOf("8.8.8.8")
    var dns2 by mutableStateOf("8.8.4.4")
    var slowDnsEnabled by mutableStateOf(false)
    var slowDnsServer by mutableStateOf("")
    var slowDnsPort by mutableStateOf("53")
    var slowDnsUser by mutableStateOf("")
    var slowDnsPass by mutableStateOf("")

    // VPN Settings
    var forwardDns by mutableStateOf(true)
    var keepAlive by mutableStateOf(true)
    var keepAliveInterval by mutableStateOf("60")
    var splitTunnel by mutableStateOf(false)
    var autoConnect by mutableStateOf(false)
    var udpBufferSize by mutableStateOf("64")
    var udpTx by mutableStateOf("30")
    var udpRx by mutableStateOf("30")

    // UDPGW
    var udpgwServer by mutableStateOf("127.0.0.1")
    var udpgwPort by mutableStateOf("7300")

    // Lock state — when true, the UI hides all sensitive fields
    var configLocked by mutableStateOf(false)
    var configName by mutableStateOf("")

    /**
     * Sync combined SSH config to/from separate fields.
     */
    fun syncSshFromConfig() {
        val atIndex = sshConfig.indexOf('@')
        if (atIndex >= 0) {
            val serverPart = sshConfig.substring(0, atIndex)
            val credPart = sshConfig.substring(atIndex + 1)
            val hp = serverPart.split(":", limit = 2)
            sshHost = hp.getOrElse(0) { "" }
            sshPort = hp.getOrElse(1) { "22" }
            val up = credPart.split(":", limit = 2)
            sshUser = up.getOrElse(0) { "" }
            sshPass = up.getOrElse(1) { "" }
        }
    }

    fun syncSshToConfig() {
        sshConfig = if (sshHost.isNotEmpty()) "$sshHost:$sshPort@$sshUser:$sshPass" else ""
    }

    /**
     * Load a full BypConfig into the session.
     */
    fun loadConfig(config: BypConfig) {
        configName = config.name
        configLocked = config.locked

        val host = config.server.host
        val port = config.server.port
        val user = config.auth.username
        val pass = config.auth.password
        sshConfig = if (host.isNotEmpty()) "$host:$port@$user:$pass" else ""
        sshHost = host
        sshPort = port.toString()
        sshUser = user
        sshPass = pass

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
        sshHost = ""; sshPort = "22"; sshUser = ""; sshPass = ""
        proxyType = "Direct"
        payload = "GET / HTTP/1.1[crlf]Host: [sni][crlf]Connection: Keep-Alive[crlf][crlf]"
        proxyHost = ""; proxyPort = "8080"
        sni = ""; sniBugHost = ""
        dns1 = "8.8.8.8"; dns2 = "8.8.4.4"
        slowDnsEnabled = false; slowDnsServer = ""; slowDnsPort = "53"
        forwardDns = true; keepAlive = true; keepAliveInterval = "60"
        splitTunnel = false; autoConnect = false
        udpBufferSize = "64"; udpTx = "30"; udpRx = "30"
        udpgwServer = "127.0.0.1"; udpgwPort = "7300"
    }
}
