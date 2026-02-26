package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * SSH Settings screen — HC's "SSH Settings" drawer item.
 * Separate fields: Host, Port, Username, Password, Proxy Type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshSettingsScreen() {
    var host by remember { mutableStateOf(SessionManager.sshHost) }
    var port by remember { mutableStateOf(SessionManager.sshPort) }
    var user by remember { mutableStateOf(SessionManager.sshUser) }
    var pass by remember { mutableStateOf(SessionManager.sshPass) }
    var proxyType by remember { mutableStateOf(SessionManager.proxyType) }
    var proxyExpanded by remember { mutableStateOf(false) }
    val proxyTypes = listOf("Direct", "SOCKS4", "SOCKS5")
    val isLocked = SessionManager.configLocked

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("SSH Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("SERVER", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            BypNetTextField(
                value = if (isLocked) "••••••••" else host,
                onValueChange = { if (!isLocked) { host = it; SessionManager.sshHost = it; SessionManager.syncSshToConfig() } },
                label = "Host",
                placeholder = "ssh.example.com",
                enabled = !isLocked
            )
            Spacer(Modifier.height(10.dp))

            BypNetTextField(
                value = if (isLocked) "••••" else port,
                onValueChange = { if (!isLocked) { port = it; SessionManager.sshPort = it; SessionManager.syncSshToConfig() } },
                label = "Port",
                placeholder = "22",
                enabled = !isLocked
            )
            Spacer(Modifier.height(20.dp))

            Text("AUTHENTICATION", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            BypNetTextField(
                value = if (isLocked) "••••••••" else user,
                onValueChange = { if (!isLocked) { user = it; SessionManager.sshUser = it; SessionManager.syncSshToConfig() } },
                label = "Username",
                placeholder = "username",
                enabled = !isLocked
            )
            Spacer(Modifier.height(10.dp))

            BypNetTextField(
                value = if (isLocked) "••••••••" else pass,
                onValueChange = { if (!isLocked) { pass = it; SessionManager.sshPass = it; SessionManager.syncSshToConfig() } },
                label = "Password",
                placeholder = "password",
                enabled = !isLocked
            )
            Spacer(Modifier.height(20.dp))

            Text("PROXY TYPE", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = proxyExpanded,
                onExpandedChange = { proxyExpanded = !proxyExpanded }
            ) {
                OutlinedTextField(
                    value = proxyType, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(proxyExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkCard, unfocusedContainerColor = DarkCard,
                        focusedTrailingIconColor = StatusConnected, unfocusedTrailingIconColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                )
                ExposedDropdownMenu(
                    expanded = proxyExpanded,
                    onDismissRequest = { proxyExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    proxyTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = if (type == proxyType) StatusConnected else TextPrimary) },
                            onClick = { proxyType = type; SessionManager.proxyType = type; proxyExpanded = false }
                        )
                    }
                }
            }
        }
    }
}
