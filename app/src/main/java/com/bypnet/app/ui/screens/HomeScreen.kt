package com.bypnet.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.components.BypNetTextField
import com.bypnet.app.ui.components.ConnectionState
import com.bypnet.app.ui.theme.*

@Composable
fun HomeScreen() {
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    connectionState = if (connectionState == ConnectionState.DISCONNECTED) {
                        ConnectionState.CONNECTING
                    } else {
                        ConnectionState.DISCONNECTED
                    }
                },
                containerColor = if (connectionState == ConnectionState.DISCONNECTED) Cyan400 else StatusDisconnected,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (connectionState == ConnectionState.DISCONNECTED)
                        Icons.Filled.PlayArrow else Icons.Filled.Stop,
                    contentDescription = "Connect",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── SSH Configuration (TOP) ──
            SectionHeader("SSH Configuration")
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BypNetTextField(
                    value = sshHost,
                    onValueChange = { sshHost = it },
                    label = "SSH Host",
                    placeholder = "server.example.com",
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BypNetTextField(
                    value = sshUser,
                    onValueChange = { sshUser = it },
                    label = "Username",
                    placeholder = "user",
                    modifier = Modifier.weight(1f)
                )
                BypNetTextField(
                    value = sshPass,
                    onValueChange = { sshPass = it },
                    label = "Password",
                    placeholder = "••••••",
                    isPassword = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Checkbox Options (BELOW SSH) ──
            SectionHeader("Options")
            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCard.copy(alpha = 0.5f))
                    .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        CompactCheckbox("SSH", enableSsh) {
                            enableSsh = it
                            if (it) enableV2ray = false
                        }
                        CompactCheckbox("SSL/TLS", enableSsl) { enableSsl = it }
                        CompactCheckbox("V2Ray", enableV2ray) {
                            enableV2ray = it
                            if (it) enableSsh = false
                        }
                        CompactCheckbox("Custom Payload", customPayload) { customPayload = it }
                        CompactCheckbox("Keep Alive", keepAlive) { keepAlive = it }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        CompactCheckbox("Enable DNS", forwardDns) { forwardDns = it }
                        CompactCheckbox("DNS Custom", dnsCustom) { dnsCustom = it }
                        CompactCheckbox("UDP Custom", udpCustom) { udpCustom = it }
                        CompactCheckbox("SlowDNS", false) { /* TODO */ }
                        CompactCheckbox("Vpn2Tether", false) { /* TODO */ }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Conditional Fields ──
            if (dnsCustom) {
                SectionHeader("Custom DNS")
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BypNetTextField(
                        value = dns1,
                        onValueChange = { dns1 = it },
                        label = "DNS 1",
                        modifier = Modifier.weight(1f)
                    )
                    BypNetTextField(
                        value = dns2,
                        onValueChange = { dns2 = it },
                        label = "DNS 2",
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

            // Bottom padding for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Cyan400,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun CompactCheckbox(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Cyan400,
                checkmarkColor = DarkBackground,
                uncheckedColor = TextSecondary
            ),
            modifier = Modifier.scale(0.8f)
        )
        Text(
            text = text,
            color = if (checked) TextPrimary else TextSecondary,
            fontSize = 13.sp
        )
    }
}
