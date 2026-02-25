package com.bypnet.app.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.components.*
import com.bypnet.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var selectedProtocol by remember { mutableStateOf(TunnelProtocol.SSH) }
    var serverHost by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var sni by remember { mutableStateOf("") }
    var payload by remember { mutableStateOf("") }
    var showServerFields by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App branding
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Cyan400, GradientCyanEnd)
                        )
                    )
            ) {
                Text(
                    text = "B",
                    color = DarkBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "BypNet",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tunneling Client v1.0",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            // Status indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (connectionState) {
                            ConnectionState.CONNECTED -> StatusConnected
                            ConnectionState.CONNECTING -> StatusConnecting
                            ConnectionState.DISCONNECTED -> StatusDisconnected
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Connection button
        ConnectionButton(
            state = connectionState,
            onClick = {
                connectionState = when (connectionState) {
                    ConnectionState.DISCONNECTED -> ConnectionState.CONNECTING
                    ConnectionState.CONNECTING -> ConnectionState.DISCONNECTED
                    ConnectionState.CONNECTED -> ConnectionState.DISCONNECTED
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Connection stats
        ConnectionStats(
            uploadSpeed = "0 KB/s",
            downloadSpeed = "0 KB/s",
            duration = "00:00:00"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Protocol selector
        ProtocolSelector(
            selectedProtocol = selectedProtocol,
            onProtocolSelected = { selectedProtocol = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Server configuration section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkCard)
                .border(0.5.dp, DarkBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .then(
                    Modifier.fillMaxWidth()
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Dns,
                contentDescription = null,
                tint = Cyan400,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Server Configuration",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showServerFields = !showServerFields },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (showServerFields) Icons.Filled.ExpandLess
                    else Icons.Filled.ExpandMore,
                    contentDescription = "Toggle",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showServerFields,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                // Server host & port
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BypNetTextField(
                        value = serverHost,
                        onValueChange = { serverHost = it },
                        label = "Host",
                        leadingIcon = Icons.Filled.Computer,
                        placeholder = "server.example.com",
                        modifier = Modifier.weight(2f)
                    )
                    BypNetTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = "Port",
                        placeholder = "443",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Username & password (for SSH/SSL)
                if (selectedProtocol in listOf(
                        TunnelProtocol.SSH,
                        TunnelProtocol.SSL,
                        TunnelProtocol.HTTP
                    )
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BypNetTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Username",
                            leadingIcon = Icons.Filled.Person,
                            placeholder = "user",
                            modifier = Modifier.weight(1f)
                        )
                        BypNetTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            leadingIcon = Icons.Filled.Lock,
                            placeholder = "••••••",
                            isPassword = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // SNI
                BypNetTextField(
                    value = sni,
                    onValueChange = { sni = it },
                    label = "SNI (Server Name Indication)",
                    leadingIcon = Icons.Filled.Language,
                    placeholder = "bug.example.com"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Payload editor
                if (selectedProtocol in listOf(
                        TunnelProtocol.SSH,
                        TunnelProtocol.SSL,
                        TunnelProtocol.HTTP
                    )
                ) {
                    PayloadEditor(
                        payload = payload,
                        onPayloadChange = { payload = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Config management buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { /* TODO: Import config */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Cyan400
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Cyan400.copy(alpha = 0.5f), GradientCyanEnd.copy(alpha = 0.5f))
                    )
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import .byp", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { /* TODO: Export config */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Teal400
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Teal400.copy(alpha = 0.5f), Teal500.copy(alpha = 0.5f))
                    )
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export .byp", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
