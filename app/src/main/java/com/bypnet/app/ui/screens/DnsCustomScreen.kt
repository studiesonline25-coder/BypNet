package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.config.SessionManager
import com.bypnet.app.ui.components.BypNetTextField
import com.bypnet.app.ui.theme.*

/**
 * DNS Custom screen — HC's "DNS Custom" tool.
 * Primary/Secondary DNS + Slow DNS configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsCustomScreen() {
    var dns1 by remember { mutableStateOf(SessionManager.dns1) }
    var dns2 by remember { mutableStateOf(SessionManager.dns2) }
    var slowDns by remember { mutableStateOf(SessionManager.slowDnsEnabled) }
    var slowServer by remember { mutableStateOf(SessionManager.slowDnsServer) }
    var slowPort by remember { mutableStateOf(SessionManager.slowDnsPort) }
    var slowUser by remember { mutableStateOf(SessionManager.slowDnsUser) }
    var slowPass by remember { mutableStateOf(SessionManager.slowDnsPass) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("DNS Custom", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("DNS SERVERS", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            BypNetTextField(
                value = dns1,
                onValueChange = { dns1 = it; SessionManager.dns1 = it },
                label = "Primary DNS",
                placeholder = "8.8.8.8"
            )
            Spacer(Modifier.height(10.dp))

            BypNetTextField(
                value = dns2,
                onValueChange = { dns2 = it; SessionManager.dns2 = it },
                label = "Secondary DNS",
                placeholder = "8.8.4.4"
            )

            Spacer(Modifier.height(20.dp))
            Text("SLOW DNS", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            HcToggle("Enable Slow DNS", slowDns) { slowDns = it; SessionManager.slowDnsEnabled = it }

            if (slowDns) {
                Spacer(Modifier.height(6.dp))
                BypNetTextField(
                    value = slowServer,
                    onValueChange = { slowServer = it; SessionManager.slowDnsServer = it },
                    label = "Server",
                    placeholder = "dns.example.com"
                )
                Spacer(Modifier.height(10.dp))
                BypNetTextField(
                    value = slowPort,
                    onValueChange = { slowPort = it; SessionManager.slowDnsPort = it },
                    label = "Port",
                    placeholder = "53"
                )
                Spacer(Modifier.height(10.dp))
                BypNetTextField(
                    value = slowUser,
                    onValueChange = { slowUser = it; SessionManager.slowDnsUser = it },
                    label = "Username",
                    placeholder = "username"
                )
                Spacer(Modifier.height(10.dp))
                BypNetTextField(
                    value = slowPass,
                    onValueChange = { slowPass = it; SessionManager.slowDnsPass = it },
                    label = "Password",
                    placeholder = "password"
                )
            }
        }
    }
}
