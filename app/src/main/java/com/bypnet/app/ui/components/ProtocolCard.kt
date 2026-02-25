package com.bypnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

enum class TunnelProtocol(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
) {
    SSH("SSH", Icons.Filled.Terminal, ProtocolSSH, "Secure Shell Tunnel"),
    SSL("SSL/TLS", Icons.Filled.Lock, ProtocolSSL, "SSL/TLS Tunnel"),
    HTTP("HTTP", Icons.Filled.Http, ProtocolHTTP, "HTTP Proxy"),
    V2RAY("V2Ray", Icons.Filled.Bolt, ProtocolV2Ray, "VMess/VLESS"),
    SHADOWSOCKS("Shadowsocks", Icons.Filled.Shield, ProtocolShadowsocks, "SOCKS5 Proxy"),
    WIREGUARD("WireGuard", Icons.Filled.Security, ProtocolWireGuard, "WireGuard Tunnel"),
    TROJAN("Trojan", Icons.Filled.GppGood, ProtocolTrojan, "Trojan Protocol")
}

@Composable
fun ProtocolCard(
    protocol: TunnelProtocol,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (isSelected) protocol.color.copy(alpha = 0.12f)
                else DarkCard
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) protocol.color.copy(alpha = 0.6f)
                else DarkBorder,
                shape = shape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = protocol.icon,
                contentDescription = protocol.displayName,
                tint = if (isSelected) protocol.color else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = protocol.displayName,
                    color = if (isSelected) protocol.color else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ProtocolSelector(
    selectedProtocol: TunnelProtocol,
    onProtocolSelected: (TunnelProtocol) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "PROTOCOL",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Using a flow row manually - 2 per row
        val protocols = TunnelProtocol.entries
        val rows = protocols.chunked(3)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowProtocols ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowProtocols.forEach { protocol ->
                        ProtocolCard(
                            protocol = protocol,
                            isSelected = protocol == selectedProtocol,
                            onClick = { onProtocolSelected(protocol) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if less than 3 in row
                    repeat(3 - rowProtocols.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
