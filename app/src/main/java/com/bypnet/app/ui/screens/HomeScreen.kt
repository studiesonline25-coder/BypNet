package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.components.BypNetTextField
import com.bypnet.app.ui.components.ConnectionState
import com.bypnet.app.ui.theme.*

@Composable
fun HomeScreen() {
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    
    // Core fields
    var payload by remember { mutableStateOf("CONNECT [host_port] [protocol][crlf]Host: [host][crlf][crlf]") }
    var host by remember { mutableStateOf("") }
    
    // Checkbox toggles
    var enableSsl by remember { mutableStateOf(false) }
    var enableV2ray by remember { mutableStateOf(false) }
    var enableSsh by remember { mutableStateOf(true) }
    var autoReplace by remember { mutableStateOf(true) }
    var customPayload by remember { mutableStateOf(true) }
    var dnsCustom by remember { mutableStateOf(false) }
    var forwardDns by remember { mutableStateOf(true) }
    var udpCustom by remember { mutableStateOf(false) }
    var keepAlive by remember { mutableStateOf(true) }

    // Conditional Fields
    var sni by remember { mutableStateOf("") }
    var sshServer by remember { mutableStateOf("") }
    var sshPort by remember { mutableStateOf("22") }
    var sshUser by remember { mutableStateOf("") }
    var sshPass by remember { mutableStateOf("") }
    var dns1 by remember { mutableStateOf("8.8.8.8") }
    var dns2 by remember { mutableStateOf("8.8.4.4") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    connectionState = if (connectionState == ConnectionState.DISCONNECTED) {
                        ConnectionState.CONNECTING // In a real app, this triggers the VPN service
                    } else {
                        ConnectionState.DISCONNECTED
                    }
                },
                containerColor = if (connectionState == ConnectionState.DISCONNECTED) Cyan400 else StatusDisconnected,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (connectionState == ConnectionState.DISCONNECTED) Icons.Filled.PlayArrow else Icons.Filled.Stop,
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

            // Payload Box (like HTTP Custom)
            if (customPayload) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Payload",
                            color = Cyan400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Auto Replace", color = TextSecondary, fontSize = 12.sp)
                            Checkbox(
                                checked = autoReplace,
                                onCheckedChange = { autoReplace = it },
                                colors = CheckboxDefaults.colors(checkedColor = Cyan400),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 200.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = payload,
                            onValueChange = { payload = it },
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            ),
                            cursorBrush = SolidColor(Cyan400),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Remote Proxy / Host Box
            Text(
                text = "Remote Proxy / Host",
                color = Cyan400,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = host,
                onValueChange = { host = it },
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(Cyan400),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (host.isEmpty()) {
                            Text("IP:Port@Username:Password", color = TextTertiary, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox Grid (The dense HTTP Custom layout)
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
                        CompactCheckbox("Enable DNS", forwardDns) { forwardDns = it }
                        CompactCheckbox("DNS Custom", dnsCustom) { dnsCustom = it }
                        CompactCheckbox("Custom Payload", customPayload) { customPayload = it }
                        CompactCheckbox("V2Ray", enableV2ray) { enableV2ray = it; if (it) enableSsh = false }
                        CompactCheckbox("SSL/TLS", enableSsl) { enableSsl = it }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        CompactCheckbox("SSH", enableSsh) { enableSsh = it; if (it) enableV2ray = false }
                        CompactCheckbox("Keep Alive", keepAlive) { keepAlive = it }
                        CompactCheckbox("UDP Custom", udpCustom) { udpCustom = it }
                        CompactCheckbox("Vpn2Tether", false) { /* TODO */ }
                        CompactCheckbox("SlowDNS", false) { /* TODO */ }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- Conditional Inputs based on checkboxes --

            if (dnsCustom) {
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
                BypNetTextField(
                    value = sni,
                    onValueChange = { sni = it },
                    label = "SNI (Server Name Indication)"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (enableSsh) {
                Text(
                    text = "SSH Configuration",
                    color = Cyan400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BypNetTextField(
                        value = sshServer,
                        onValueChange = { sshServer = it },
                        label = "Host",
                        modifier = Modifier.weight(2f)
                    )
                    BypNetTextField(
                        value = sshPort,
                        onValueChange = { sshPort = it },
                        label = "Port",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                        isPassword = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (enableV2ray) {
                BypNetTextField(
                    value = "",
                    onValueChange = { /* TODO */ },
                    label = "V2Ray Config (JSON)"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Padding for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
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
