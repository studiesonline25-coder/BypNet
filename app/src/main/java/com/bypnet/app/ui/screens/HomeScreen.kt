package com.bypnet.app.ui.screens

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.bypnet.app.ui.components.ConnectionState
import com.bypnet.app.ui.theme.*

/**
 * HomeScreen — exact replica of HTTP Custom's SSH tab.
 *
 * Layout:
 *   ip:port@user:pass  (underline text field)
 *   ☐ Use Payload    ☐ SSL
 *   ☐ Enhanced       ☐ SlowDns
 *   ☑ Enable DNS     ☐ UDP Custom
 *   ☐ Psiphon        ☐ V2ray
 *   [ CONNECT ]      (outlined green button)
 */
@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // ── Connection State ──
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    val isLocked = SessionManager.configLocked

    // SSH Config
    var sshConfig by remember { mutableStateOf(SessionManager.sshConfig) }

    // Checkboxes (matching HTTP Custom exactly)
    var usePayload by remember { mutableStateOf(true) }
    var enableSsl by remember { mutableStateOf(false) }
    var enhanced by remember { mutableStateOf(false) }
    var slowDns by remember { mutableStateOf(false) }
    var enableDns by remember { mutableStateOf(true) }
    var udpCustom by remember { mutableStateOf(false) }
    var psiphon by remember { mutableStateOf(false) }
    var v2ray by remember { mutableStateOf(false) }

    // Speed/timer
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
        startVpnService(context, sshConfig, enableSsl, v2ray, enableDns)
    }

    fun connect() {
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) vpnLauncher.launch(vpnIntent)
        else startVpnService(context, sshConfig, enableSsl, v2ray, enableDns)
    }

    fun disconnect() {
        context.startService(Intent(context, BypNetVpnService::class.java).apply {
            action = BypNetVpnService.ACTION_DISCONNECT
        })
    }

    // ── UI ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Locked Config Banner ──
        if (isLocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(StatusConnected.copy(alpha = 0.1f))
                    .border(1.dp, StatusConnected.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Icon(Icons.Filled.Lock, null, tint = StatusConnected, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "🔒 ${SessionManager.configName.ifEmpty { "Locked Config" }}",
                    color = StatusConnected, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── SSH Config Field (underline style like HTTP Custom) ──
        OutlinedTextField(
            value = if (isLocked) "••••••••••••••••" else sshConfig,
            onValueChange = {
                if (!isLocked) { sshConfig = it; SessionManager.sshConfig = it }
            },
            placeholder = {
                Text("ip:port@user:pass", color = TextTertiary, fontSize = 14.sp)
            },
            enabled = !isLocked,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StatusConnected,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextSecondary,
                disabledBorderColor = DarkBorder,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = StatusConnected
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp) // Flat underline style
        )

        Spacer(Modifier.height(12.dp))

        // ── Checkbox Grid (2 columns, exactly like HTTP Custom) ──
        Row(modifier = Modifier.fillMaxWidth()) {
            HcCheckbox("Use Payload", usePayload, { usePayload = it }, Modifier.weight(1f))
            HcCheckbox("SSL", enableSsl, { enableSsl = it }, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            HcCheckbox("Enhanced", enhanced, { enhanced = it }, Modifier.weight(1f))
            HcCheckbox("SlowDns", slowDns, { slowDns = it }, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            HcCheckbox("Enable DNS", enableDns, { enableDns = it }, Modifier.weight(1f))
            HcCheckbox("UDP Custom", udpCustom, { udpCustom = it }, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            HcCheckbox("Psiphon", psiphon, { psiphon = it }, Modifier.weight(1f))
            HcCheckbox("V2ray", v2ray, { v2ray = it }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // ── CONNECT / DISCONNECT Button (outlined green, like HC) ──
        if (connectionState == ConnectionState.CONNECTING) {
            OutlinedButton(
                onClick = { disconnect() },
                border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusConnecting),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = StatusConnecting
                )
                Spacer(Modifier.width(10.dp))
                Text("CONNECTING...", color = StatusConnecting, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            OutlinedButton(
                onClick = {
                    if (connectionState == ConnectionState.DISCONNECTED) connect() else disconnect()
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (connectionState == ConnectionState.CONNECTED) StatusDisconnected else StatusConnected
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    if (connectionState == ConnectionState.DISCONNECTED) "CONNECT" else "DISCONNECT",
                    color = if (connectionState == ConnectionState.CONNECTED) StatusDisconnected else StatusConnected,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // ── Speed Display (shown when connected) ──
        if (connectionState == ConnectionState.CONNECTED) {
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("↑ Upload", color = TextTertiary, fontSize = 10.sp)
                    Text(formatSpeed(uploadSpeed), color = StatusConnected, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(formatBytes(totalUp), color = TextTertiary, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Timer", color = TextTertiary, fontSize = 10.sp)
                    Text(formatDuration(elapsedMs), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("↓ Download", color = TextTertiary, fontSize = 10.sp)
                    Text(formatSpeed(downloadSpeed), color = StatusConnected, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(formatBytes(totalDown), color = TextTertiary, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── HTTP Custom-style Checkbox ──
@Composable
fun HcCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = StatusConnected,
                uncheckedColor = TextTertiary,
                checkmarkColor = DarkBackground
            ),
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 13.sp
        )
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
    sshConfig: String,
    enableSsl: Boolean,
    v2ray: Boolean,
    enableDns: Boolean
) {
    val parsed = parseSshConfig(sshConfig)
    val protocol = when {
        v2ray -> "V2RAY"
        enableSsl -> "SSL"
        else -> "SSH"
    }

    val intent = Intent(context, BypNetVpnService::class.java).apply {
        action = BypNetVpnService.ACTION_CONNECT
        putExtra(BypNetVpnService.EXTRA_PROTOCOL, protocol)
        putExtra(BypNetVpnService.EXTRA_SERVER_HOST, parsed[0])
        putExtra(BypNetVpnService.EXTRA_SERVER_PORT, parsed[1].toIntOrNull() ?: 22)
        putExtra(BypNetVpnService.EXTRA_USERNAME, parsed[2])
        putExtra(BypNetVpnService.EXTRA_PASSWORD, parsed[3])
        putExtra(BypNetVpnService.EXTRA_SNI, SessionManager.sni)
        putExtra(BypNetVpnService.EXTRA_PAYLOAD, SessionManager.payload)
        putExtra(BypNetVpnService.EXTRA_PROXY_HOST, SessionManager.proxyHost)
        putExtra(BypNetVpnService.EXTRA_PROXY_PORT, SessionManager.proxyPort.toIntOrNull() ?: 8080)
        putExtra(BypNetVpnService.EXTRA_DNS1, if (enableDns) SessionManager.dns1 else "8.8.8.8")
        putExtra(BypNetVpnService.EXTRA_DNS2, if (enableDns) SessionManager.dns2 else "8.8.4.4")
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
