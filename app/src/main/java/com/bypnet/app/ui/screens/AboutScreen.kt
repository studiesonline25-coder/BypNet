package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

/**
 * About screen — HC's "About" drawer item.
 * App version, developer info, credits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("About", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(StatusConnected.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("B", color = StatusConnected, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            Row {
                Text("Byp", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                Text("Net", color = StatusConnected, fontSize = 24.sp, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic)
            }

            Text("v1.0.0", color = TextSecondary, fontSize = 13.sp)

            Spacer(Modifier.height(24.dp))

            Text(
                "BypNet is an SSH VPN client with custom HTTP header injection, " +
                "supporting SSH, SSL/TLS, WebSocket, and V2Ray tunneling protocols.",
                color = TextTertiary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            HorizontalDivider(color = DarkBorder)

            Spacer(Modifier.height(16.dp))

            AboutInfoRow("Developer", "BypNet Dev Team")
            AboutInfoRow("Config Format", ".byp (encrypted)")
            AboutInfoRow("Min SDK", "API 24 (Android 7.0)")
            AboutInfoRow("Target SDK", "API 34 (Android 14)")

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DarkBorder)
            Spacer(Modifier.height(16.dp))

            Text("FEATURES", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            val features = listOf(
                "SSH & SSL/TLS Tunneling",
                "Custom HTTP Payload Injection",
                "Payload Generator (Simple Maker)",
                "DNS Custom & Slow DNS",
                "SNI Configuration",
                "Cookie Browser",
                "IP Hunter & Response Checker",
                "UDPGW SSH Support",
                "V2Ray & WebSocket Protocols",
                ".byp Config Import/Export (with Lock)"
            )
            features.forEach { feature ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text("•", color = StatusConnected, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(feature, color = TextPrimary, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("© 2026 BypNet Dev Team", color = TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
