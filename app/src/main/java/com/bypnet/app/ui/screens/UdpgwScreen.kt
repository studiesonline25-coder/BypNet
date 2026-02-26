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
 * UDPGW SSH screen — HC's "UDPGW SSH" tool.
 * Configures UDPGW server and port for UDP forwarding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdpgwScreen() {
    var server by remember { mutableStateOf(SessionManager.udpgwServer) }
    var port by remember { mutableStateOf(SessionManager.udpgwPort) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("UDPGW SSH", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("UDPGW CONFIGURATION", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            BypNetTextField(
                value = server,
                onValueChange = { server = it; SessionManager.udpgwServer = it },
                label = "UDPGW Server",
                placeholder = "127.0.0.1"
            )
            Spacer(Modifier.height(10.dp))

            BypNetTextField(
                value = port,
                onValueChange = { port = it; SessionManager.udpgwPort = it },
                label = "UDPGW Port",
                placeholder = "7300"
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "UDPGW (UDP Gateway) allows UDP traffic to be forwarded through the SSH tunnel. " +
                "Default server is 127.0.0.1 and default port is 7300.",
                color = TextTertiary, fontSize = 12.sp
            )
        }
    }
}
