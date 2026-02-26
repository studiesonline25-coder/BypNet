package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.config.SessionManager
import com.bypnet.app.ui.components.BypNetTextField
import com.bypnet.app.ui.theme.*

/**
 * VPN Settings screen — HC's "VPN Settings" drawer item.
 * Forward DNS, Keep Alive, UDP Tweak (Buffer/TX/RX), Split Tunneling, Auto-Connect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnSettingsScreen() {
    var forwardDns by remember { mutableStateOf(SessionManager.forwardDns) }
    var keepAlive by remember { mutableStateOf(SessionManager.keepAlive) }
    var keepAliveInterval by remember { mutableStateOf(SessionManager.keepAliveInterval) }
    var splitTunnel by remember { mutableStateOf(SessionManager.splitTunnel) }
    var autoConnect by remember { mutableStateOf(SessionManager.autoConnect) }
    var bufferSize by remember { mutableStateOf(SessionManager.udpBufferSize) }
    var tx by remember { mutableStateOf(SessionManager.udpTx) }
    var rx by remember { mutableStateOf(SessionManager.udpRx) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("VPN Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("CONNECTION", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            HcToggle("Forward DNS", forwardDns) { forwardDns = it; SessionManager.forwardDns = it }
            HcToggle("Keep Alive", keepAlive) { keepAlive = it; SessionManager.keepAlive = it }

            if (keepAlive) {
                Spacer(Modifier.height(6.dp))
                BypNetTextField(
                    value = keepAliveInterval,
                    onValueChange = { keepAliveInterval = it; SessionManager.keepAliveInterval = it },
                    label = "Keep Alive Interval (seconds)",
                    placeholder = "60"
                )
            }
            Spacer(Modifier.height(6.dp))

            HcToggle("Split Tunneling", splitTunnel) { splitTunnel = it; SessionManager.splitTunnel = it }
            HcToggle("Auto-Connect on Boot", autoConnect) { autoConnect = it; SessionManager.autoConnect = it }

            Spacer(Modifier.height(20.dp))
            Text("UDP TWEAK", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            BypNetTextField(
                value = bufferSize,
                onValueChange = { bufferSize = it; SessionManager.udpBufferSize = it },
                label = "Buffer Size",
                placeholder = "64"
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BypNetTextField(
                    value = tx,
                    onValueChange = { tx = it; SessionManager.udpTx = it },
                    label = "TX",
                    placeholder = "30",
                    modifier = Modifier.weight(1f)
                )
                BypNetTextField(
                    value = rx,
                    onValueChange = { rx = it; SessionManager.udpRx = it },
                    label = "RX",
                    placeholder = "30",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun HcToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StatusConnected,
                checkedTrackColor = StatusConnected.copy(alpha = 0.3f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkCard
            )
        )
    }
}
