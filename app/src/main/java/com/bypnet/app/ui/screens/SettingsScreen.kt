package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var primaryDns by remember { mutableStateOf("8.8.8.8") }
    var secondaryDns by remember { mutableStateOf("8.8.4.4") }
    var splitTunnel by remember { mutableStateOf(false) }
    var autoConnect by remember { mutableStateOf(false) }
    var keepAlive by remember { mutableStateOf(true) }
    var showNotification by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = Cyan400,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure your tunneling preferences",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- DNS Section ---
        SettingsSection(title = "DNS Configuration") {
            SettingsInputItem(
                icon = Icons.Filled.Dns,
                title = "Primary DNS",
                value = primaryDns,
                onValueChange = { primaryDns = it },
                iconTint = Cyan400
            )
            SettingsInputItem(
                icon = Icons.Filled.Dns,
                title = "Secondary DNS",
                value = secondaryDns,
                onValueChange = { secondaryDns = it },
                iconTint = Teal400
            )

            // Quick DNS presets
            Text(
                text = "QUICK PRESETS",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                DnsPresetChip("Google", "8.8.8.8", "8.8.4.4") {
                    primaryDns = "8.8.8.8"
                    secondaryDns = "8.8.4.4"
                }
                DnsPresetChip("Cloudflare", "1.1.1.1", "1.0.0.1") {
                    primaryDns = "1.1.1.1"
                    secondaryDns = "1.0.0.1"
                }
                DnsPresetChip("Quad9", "9.9.9.9", "149.112.112.112") {
                    primaryDns = "9.9.9.9"
                    secondaryDns = "149.112.112.112"
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Connection Section ---
        SettingsSection(title = "Connection") {
            SettingsToggleItem(
                icon = Icons.Filled.WifiTethering,
                title = "Split Tunneling",
                subtitle = "Choose which apps use the tunnel",
                isChecked = splitTunnel,
                onCheckedChange = { splitTunnel = it },
                iconTint = ProtocolV2Ray
            )
            SettingsDivider()
            SettingsToggleItem(
                icon = Icons.Filled.PlayCircle,
                title = "Auto-Connect on Boot",
                subtitle = "Start tunnel when device boots",
                isChecked = autoConnect,
                onCheckedChange = { autoConnect = it },
                iconTint = StatusConnected
            )
            SettingsDivider()
            SettingsToggleItem(
                icon = Icons.Filled.Favorite,
                title = "Keep Alive",
                subtitle = "Send periodic pings to maintain connection",
                isChecked = keepAlive,
                onCheckedChange = { keepAlive = it },
                iconTint = StatusDisconnected
            )
            SettingsDivider()
            SettingsToggleItem(
                icon = Icons.Filled.Notifications,
                title = "Show Notification",
                subtitle = "Display persistent VPN notification",
                isChecked = showNotification,
                onCheckedChange = { showNotification = it },
                iconTint = StatusConnecting
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Config Management Section ---
        SettingsSection(title = "Configuration") {
            SettingsActionItem(
                icon = Icons.Filled.FolderOpen,
                title = "Saved Configs",
                subtitle = "Manage your .byp configurations",
                iconTint = Cyan400,
                onClick = { /* TODO */ }
            )
            SettingsDivider()
            SettingsActionItem(
                icon = Icons.Filled.Share,
                title = "Share Connection",
                subtitle = "Share via hotspot or USB tethering",
                iconTint = Teal400,
                onClick = { /* TODO */ }
            )
            SettingsDivider()
            SettingsActionItem(
                icon = Icons.Filled.Speed,
                title = "Speed Test",
                subtitle = "Test your current connection speed",
                iconTint = StatusConnecting,
                onClick = { /* TODO */ }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- About Section ---
        SettingsSection(title = "About") {
            SettingsActionItem(
                icon = Icons.Filled.Info,
                title = "BypNet v1.0.0",
                subtitle = "Advanced Tunneling Client",
                iconTint = TextTertiary,
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title.uppercase(),
            color = Cyan400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
        ) {
            content()
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Cyan400,
                checkedTrackColor = Cyan400.copy(alpha = 0.3f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkCard
            )
        )
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsInputItem(
    icon: ImageVector,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    iconTint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Cyan400,
                fontSize = 13.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan400),
            modifier = Modifier
                .width(140.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkCard)
                .border(0.5.dp, DarkBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = DarkBorder.copy(alpha = 0.5f)
    )
}

@Composable
fun DnsPresetChip(
    name: String,
    primary: String,
    secondary: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Cyan400.copy(alpha = 0.08f))
            .border(0.5.dp, Cyan400.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            color = Cyan400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
