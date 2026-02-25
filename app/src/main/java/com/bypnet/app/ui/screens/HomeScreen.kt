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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.tunnel.BypNetVpnService
import com.bypnet.app.tunnel.TunnelStatus
import com.bypnet.app.ui.components.BypNetTextField
import com.bypnet.app.ui.components.ConnectionState
import com.bypnet.app.ui.theme.*

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // Connection state synced from VPN service
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }

    // SSH Config
    var sshHost by remember { mutableStateOf("") }
    var sshPort by remember { mutableStateOf("22") }
    var sshUser by remember { mutableStateOf("") }
    var sshPass by remember { mutableStateOf("") }

    // Checkboxes
    var enableSsh by remember { mutableStateOf(true) }
    var enableSsl by remember { mutableStateOf(false) }
    var enableV2ray by remember { mutableStateOf(false) }
    var customPayload by remember { mutableStateOf(true) }
    var dnsCustom by remember { mutableStateOf(false) }
    var forwardDns by remember { mutableStateOf(true) }
    var keepAlive by remember { mutableStateOf(true) }
    var udpCustom by remember { mutableStateOf(false) }

    // DNS fields
    var dns1 by remember { mutableStateOf("8.8.8.8") }
    var dns2 by remember { mutableStateOf("8.8.4.4") }

    // SNI
    var sni by remember { mutableStateOf("") }

    // Listen to VPN service status changes
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
        // Sync initial state
        connectionState = when (BypNetVpnService.currentStatus) {
            TunnelStatus.CONNECTED -> ConnectionState.CONNECTED
            TunnelStatus.CONNECTING, TunnelStatus.DISCONNECTING -> ConnectionState.CONNECTING
            else -> ConnectionState.DISCONNECTED
        }
        onDispose {
            BypNetVpnService.statusListener = null
        }
    }

    // VPN permission request launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // VPN permission granted (or already had it) — start the service
        startVpnService(context, sshHost, sshPort, sshUser, sshPass, sni, dns1, dns2, enableSsh, enableSsl)
    }

    // Function to start connection
    fun connect() {
        // Check if VPN permission is needed
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            // Already have permission — start directly
            startVpnService(context, sshHost, sshPort, sshUser, sshPass, sni, dns1, dns2, enableSsh, enableSsl)
        }
    }

    fun disconnect() {
        val intent = Intent(context, BypNetVpnService::class.java).apply {
            action = BypNetVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ── SSH Configuration ──
            SectionHeader("SSH Configuration")
            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BypNetTextField(
                    value = sshHost,
                    onValueChange = { sshHost = it },
                    label = "Host",
                    placeholder = "0.0.0.0",
                    modifier = Modifier.weight(2f)
                )
                BypNetTextField(
                    value = sshPort,
                    onValueChange = { sshPort = it },
                    label = "Port",
                    placeholder = "22",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BypNetTextField(
                    value = sshUser,
                    onValueChange = { sshUser = it },
                    label = "Username",
                    modifier = Modifier.weight(1f)
                )
                BypNetTextField(
                    value = sshPass,
                    onValueChange = { sshPass = it },
                    label = "Password",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ── Options Grid ──
            SectionHeader("Options")
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCard)
                    .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    CompactCheckbox("SSH", enableSsh, { enableSsh = it }, Modifier.weight(1f))
                    CompactCheckbox("SSL/TLS", enableSsl, { enableSsl = it }, Modifier.weight(1f))
                    CompactCheckbox("V2Ray", enableV2ray, { enableV2ray = it }, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    CompactCheckbox("Custom Payload", customPayload, { customPayload = it }, Modifier.weight(1f))
                    CompactCheckbox("DNS Custom", dnsCustom, { dnsCustom = it }, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    CompactCheckbox("Forward DNS", forwardDns, { forwardDns = it }, Modifier.weight(1f))
                    CompactCheckbox("Keep Alive", keepAlive, { keepAlive = it }, Modifier.weight(1f))
                    CompactCheckbox("UDP Custom", udpCustom, { udpCustom = it }, Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ── Conditional Sections ──
            if (dnsCustom) {
                SectionHeader("DNS")
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BypNetTextField(
                        value = dns1,
                        onValueChange = { dns1 = it },
                        label = "DNS 1",
                        placeholder = "8.8.8.8",
                        modifier = Modifier.weight(1f)
                    )
                    BypNetTextField(
                        value = dns2,
                        onValueChange = { dns2 = it },
                        label = "DNS 2",
                        placeholder = "8.8.4.4",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (enableSsl) {
                SectionHeader("SSL/TLS")
                Spacer(modifier = Modifier.height(4.dp))
                BypNetTextField(
                    value = sni,
                    onValueChange = { sni = it },
                    label = "SNI (Server Name Indication)",
                    placeholder = "bug.example.com"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Connect / Disconnect Button ──
            Button(
                onClick = {
                    when (connectionState) {
                        ConnectionState.DISCONNECTED -> connect()
                        ConnectionState.CONNECTED -> disconnect()
                        ConnectionState.CONNECTING -> disconnect() // Allow cancelling
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (connectionState == ConnectionState.DISCONNECTED) Cyan400 else StatusDisconnected,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = connectionState != ConnectionState.CONNECTING || connectionState == ConnectionState.CONNECTING
            ) {
                if (connectionState == ConnectionState.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DarkBackground
                    )
                } else {
                    Icon(
                        imageVector = if (connectionState == ConnectionState.DISCONNECTED)
                            Icons.Filled.PlayArrow else Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (connectionState) {
                        ConnectionState.DISCONNECTED -> "CONNECT"
                        ConnectionState.CONNECTING -> "CONNECTING..."
                        ConnectionState.CONNECTED -> "DISCONNECT"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Start the VPN service with the current configuration.
 */
private fun startVpnService(
    context: android.content.Context,
    sshHost: String,
    sshPort: String,
    sshUser: String,
    sshPass: String,
    sni: String,
    dns1: String,
    dns2: String,
    enableSsh: Boolean,
    enableSsl: Boolean
) {
    val protocol = when {
        enableSsh -> "SSH"
        enableSsl -> "SSL"
        else -> "SSH"
    }

    val intent = Intent(context, BypNetVpnService::class.java).apply {
        action = BypNetVpnService.ACTION_CONNECT
        putExtra(BypNetVpnService.EXTRA_PROTOCOL, protocol)
        putExtra(BypNetVpnService.EXTRA_SERVER_HOST, sshHost)
        putExtra(BypNetVpnService.EXTRA_SERVER_PORT, sshPort.toIntOrNull() ?: 22)
        putExtra(BypNetVpnService.EXTRA_USERNAME, sshUser)
        putExtra(BypNetVpnService.EXTRA_PASSWORD, sshPass)
        putExtra(BypNetVpnService.EXTRA_SNI, sni)
        putExtra(BypNetVpnService.EXTRA_DNS1, dns1)
        putExtra(BypNetVpnService.EXTRA_DNS2, dns2)
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Cyan400,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun CompactCheckbox(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Cyan400,
                uncheckedColor = TextTertiary,
                checkmarkColor = DarkBackground
            ),
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp
        )
    }
}
