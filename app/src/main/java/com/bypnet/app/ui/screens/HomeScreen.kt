package com.bypnet.app.ui.screens

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.config.SessionManager
import com.bypnet.app.tunnel.BypNetVpnService
import com.bypnet.app.tunnel.TunnelStatus
import com.bypnet.app.ui.components.BypNetTextField
import com.bypnet.app.ui.components.ConnectionState
import com.bypnet.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // ── State ──
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    val isLocked = SessionManager.configLocked

    // Connection method
    var connectionMethod by remember { mutableStateOf(SessionManager.connectionMethod) }
    var methodExpanded by remember { mutableStateOf(false) }
    val connectionMethods = listOf("SSH", "SSH + SSL", "SSL/TLS", "WebSocket", "V2Ray")

    // SSH Config
    var sshConfig by remember { mutableStateOf(SessionManager.sshConfig) }

    // SNI
    var sni by remember { mutableStateOf(SessionManager.sni) }

    // Remote Proxy
    var proxyHost by remember { mutableStateOf(SessionManager.proxyHost) }
    var proxyPort by remember { mutableStateOf(SessionManager.proxyPort) }

    // DNS
    var dns1 by remember { mutableStateOf(SessionManager.dns1) }
    var dns2 by remember { mutableStateOf(SessionManager.dns2) }
    var dnsEnabled by remember { mutableStateOf(false) }

    // Speed/timer (read from VPN service)
    var uploadSpeed by remember { mutableStateOf(0L) }
    var downloadSpeed by remember { mutableStateOf(0L) }
    var totalUp by remember { mutableStateOf(0L) }
    var totalDown by remember { mutableStateOf(0L) }
    var elapsedMs by remember { mutableStateOf(0L) }

    // Speed ticker when connected
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            val startTime = System.currentTimeMillis()
            while (true) {
                uploadSpeed = BypNetVpnService.uploadSpeed
                downloadSpeed = BypNetVpnService.downloadSpeed
                totalUp = BypNetVpnService.totalUpload
                totalDown = BypNetVpnService.totalDownload
                elapsedMs = System.currentTimeMillis() - startTime
                kotlinx.coroutines.delay(1000)
            }
        } else {
            uploadSpeed = 0; downloadSpeed = 0; totalUp = 0; totalDown = 0; elapsedMs = 0
        }
    }

    // Listen to VPN status
    DisposableEffect(Unit) {
        BypNetVpnService.statusListener = { status ->
            connectionState = when (status) {
                TunnelStatus.DISCONNECTED -> ConnectionState.DISCONNECTED
                TunnelStatus.CONNECTING -> ConnectionState.CONNECTING
                TunnelStatus.CONNECTED -> ConnectionState.CONNECTED
                TunnelStatus.DISCONNECTING -> ConnectionState.CONNECTING
                TunnelStatus.ERROR -> ConnectionState.DISCONNECTED
            }
        }
        connectionState = when (BypNetVpnService.currentStatus) {
            TunnelStatus.CONNECTED -> ConnectionState.CONNECTED
            TunnelStatus.CONNECTING, TunnelStatus.DISCONNECTING -> ConnectionState.CONNECTING
            else -> ConnectionState.DISCONNECTED
        }
        onDispose { BypNetVpnService.statusListener = null }
    }

    // VPN permission launcher
    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        startVpnService(context, connectionMethod, sshConfig, sni, proxyHost, proxyPort, dns1, dns2)
    }

    fun connect() {
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) vpnLauncher.launch(vpnIntent)
        else startVpnService(context, connectionMethod, sshConfig, sni, proxyHost, proxyPort, dns1, dns2)
    }

    fun disconnect() {
        context.startService(Intent(context, BypNetVpnService::class.java).apply {
            action = BypNetVpnService.ACTION_DISCONNECT
        })
    }

    // Whether to show SSH fields
    val showSsh = connectionMethod in listOf("SSH", "SSH + SSL")
    val showSsl = connectionMethod in listOf("SSH + SSL", "SSL/TLS")
    val showProxy = connectionMethod in listOf("SSH", "SSH + SSL", "SSL/TLS", "WebSocket")

    // ── UI ──
    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ── Locked Config Banner ──
            if (isLocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Cyan400.copy(alpha = 0.1f))
                        .border(1.dp, Cyan400.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Cyan400, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "🔒 Locked Config" + if (SessionManager.configName.isNotEmpty()) ": ${SessionManager.configName}" else "",
                            color = Cyan400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text("Details hidden. Tap connect to use.", color = TextTertiary, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Connection Method Dropdown ──
            SectionHeader("CONNECTION METHOD")
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = !methodExpanded }
            ) {
                OutlinedTextField(
                    value = connectionMethod,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan400,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTrailingIconColor = Cyan400,
                        unfocusedTrailingIconColor = TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                ExposedDropdownMenu(
                    expanded = methodExpanded,
                    onDismissRequest = { methodExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    connectionMethods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method, color = if (method == connectionMethod) Cyan400 else TextPrimary) },
                            onClick = {
                                connectionMethod = method
                                SessionManager.connectionMethod = method
                                methodExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    when (method) {
                                        "SSH" -> Icons.Filled.Terminal
                                        "SSH + SSL" -> Icons.Filled.Security
                                        "SSL/TLS" -> Icons.Filled.Lock
                                        "WebSocket" -> Icons.Filled.Cable
                                        "V2Ray" -> Icons.Filled.Shield
                                        else -> Icons.Filled.VpnKey
                                    },
                                    null,
                                    tint = if (method == connectionMethod) Cyan400 else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── SSH Configuration ──
            if (showSsh) {
                SectionHeader("SSH SERVER")
                Spacer(Modifier.height(4.dp))
                BypNetTextField(
                    value = if (isLocked) "••••••••••••••••" else sshConfig,
                    onValueChange = {
                        if (!isLocked) {
                            sshConfig = it; SessionManager.sshConfig = it
                        }
                    },
                    label = "SSH",
                    placeholder = "ip:port@username:password",
                    enabled = !isLocked
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── SSL/TLS (SNI) ──
            if (showSsl) {
                SectionHeader("SSL/TLS")
                Spacer(Modifier.height(4.dp))
                BypNetTextField(
                    value = if (isLocked) "••••••••" else sni,
                    onValueChange = {
                        if (!isLocked) {
                            sni = it; SessionManager.sni = it
                        }
                    },
                    label = "SNI (Server Name Indication)",
                    placeholder = "bug.example.com",
                    enabled = !isLocked
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Remote Proxy ──
            if (showProxy) {
                SectionHeader("REMOTE PROXY")
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BypNetTextField(
                        value = proxyHost,
                        onValueChange = { proxyHost = it; SessionManager.proxyHost = it },
                        label = "Proxy Host",
                        placeholder = "proxy.example.com",
                        modifier = Modifier.weight(2f)
                    )
                    BypNetTextField(
                        value = proxyPort,
                        onValueChange = { proxyPort = it; SessionManager.proxyPort = it },
                        label = "Port",
                        placeholder = "8080",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── DNS ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeader("DNS")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = dnsEnabled,
                    onCheckedChange = { dnsEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Cyan400,
                        checkedTrackColor = Cyan400.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkCard
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
            if (dnsEnabled) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BypNetTextField(
                        value = dns1,
                        onValueChange = { dns1 = it; SessionManager.dns1 = it },
                        label = "DNS 1",
                        placeholder = "8.8.8.8",
                        modifier = Modifier.weight(1f)
                    )
                    BypNetTextField(
                        value = dns2,
                        onValueChange = { dns2 = it; SessionManager.dns2 = it },
                        label = "DNS 2",
                        placeholder = "8.8.4.4",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            // ── Speed + Timer (visible when connected) ──
            if (connectionState == ConnectionState.CONNECTED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Cyan400.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Upload
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ArrowUpward, null, tint = StatusConnected, modifier = Modifier.size(16.dp))
                                Text(formatSpeed(uploadSpeed), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(formatBytes(totalUp), color = TextTertiary, fontSize = 10.sp)
                            }
                            // Timer
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Timer, null, tint = Cyan400, modifier = Modifier.size(16.dp))
                                Text(formatDuration(elapsedMs), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("elapsed", color = TextTertiary, fontSize = 10.sp)
                            }
                            // Download
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ArrowDownward, null, tint = Cyan400, modifier = Modifier.size(16.dp))
                                Text(formatSpeed(downloadSpeed), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(formatBytes(totalDown), color = TextTertiary, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Connect / Disconnect Button ──
            val btnColor = when (connectionState) {
                ConnectionState.DISCONNECTED -> Cyan400
                ConnectionState.CONNECTING -> StatusConnecting
                ConnectionState.CONNECTED -> StatusDisconnected
            }
            Button(
                onClick = {
                    when (connectionState) {
                        ConnectionState.DISCONNECTED -> connect()
                        ConnectionState.CONNECTED -> disconnect()
                        ConnectionState.CONNECTING -> disconnect()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = DarkBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (connectionState == ConnectionState.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = DarkBackground
                    )
                } else {
                    Icon(
                        if (connectionState == ConnectionState.DISCONNECTED) Icons.Filled.PlayArrow else Icons.Filled.Stop,
                        null, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    when (connectionState) {
                        ConnectionState.DISCONNECTED -> "CONNECT"
                        ConnectionState.CONNECTING -> "CONNECTING..."
                        ConnectionState.CONNECTED -> "DISCONNECT"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // ── Server Info (when connected) ──
            if (connectionState == ConnectionState.CONNECTED && sshConfig.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusConnected)
                    )
                    Spacer(Modifier.width(6.dp))
                    val serverHost = sshConfig.split("@").firstOrNull()?.split(":")?.firstOrNull() ?: ""
                    Text(
                        "Connected to $serverHost",
                        color = StatusConnected,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Helpers ──

private fun parseSshConfig(config: String): Array<String> {
    val atIndex = config.indexOf('@')
    val serverPart: String
    val credPart: String
    if (atIndex >= 0) {
        serverPart = config.substring(0, atIndex)
        credPart = config.substring(atIndex + 1)
    } else {
        serverPart = config
        credPart = ""
    }
    val hostPort = serverPart.split(":", limit = 2)
    val host = hostPort.getOrElse(0) { "" }
    val port = hostPort.getOrElse(1) { "22" }
    val userPass = credPart.split(":", limit = 2)
    val user = userPass.getOrElse(0) { "" }
    val pass = userPass.getOrElse(1) { "" }
    return arrayOf(host, port, user, pass)
}

private fun startVpnService(
    context: android.content.Context,
    connectionMethod: String,
    sshConfig: String,
    sni: String,
    proxyHost: String,
    proxyPort: String,
    dns1: String,
    dns2: String
) {
    val parsed = parseSshConfig(sshConfig)
    val protocol = when (connectionMethod) {
        "SSH" -> "SSH"
        "SSH + SSL" -> "SSH"
        "SSL/TLS" -> "SSL"
        "WebSocket" -> "HTTP"
        "V2Ray" -> "V2RAY"
        else -> "SSH"
    }

    val intent = Intent(context, BypNetVpnService::class.java).apply {
        action = BypNetVpnService.ACTION_CONNECT
        putExtra(BypNetVpnService.EXTRA_PROTOCOL, protocol)
        putExtra(BypNetVpnService.EXTRA_SERVER_HOST, parsed[0])
        putExtra(BypNetVpnService.EXTRA_SERVER_PORT, parsed[1].toIntOrNull() ?: 22)
        putExtra(BypNetVpnService.EXTRA_USERNAME, parsed[2])
        putExtra(BypNetVpnService.EXTRA_PASSWORD, parsed[3])
        putExtra(BypNetVpnService.EXTRA_SNI, sni)
        putExtra(BypNetVpnService.EXTRA_PAYLOAD, SessionManager.payload)
        putExtra(BypNetVpnService.EXTRA_PROXY_HOST, proxyHost)
        putExtra(BypNetVpnService.EXTRA_PROXY_PORT, proxyPort.toIntOrNull() ?: 8080)
        putExtra(BypNetVpnService.EXTRA_DNS1, dns1)
        putExtra(BypNetVpnService.EXTRA_DNS2, dns2)
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun formatSpeed(bps: Long): String = when {
    bps < 1024 -> "$bps B/s"
    bps < 1024 * 1024 -> "${"%.1f".format(bps / 1024.0)} KB/s"
    else -> "${"%.2f".format(bps / (1024.0 * 1024.0))} MB/s"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

private fun formatDuration(millis: Long): String {
    val s = millis / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Cyan400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

@Composable
fun CompactCheckbox(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Cyan400, uncheckedColor = TextTertiary, checkmarkColor = DarkBackground),
            modifier = Modifier.size(32.dp)
        )
        Text(text, color = TextPrimary, fontSize = 12.sp)
    }
}
