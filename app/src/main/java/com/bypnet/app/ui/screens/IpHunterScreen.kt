package com.bypnet.app.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*
import kotlinx.coroutines.*
import java.net.Inet4Address
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpHunterScreen() {
    var targetPrefix by remember { mutableStateOf("") }
    var isHunting by remember { mutableStateOf(false) }
    var currentIp by remember { mutableStateOf("---") }
    var attempts by remember { mutableStateOf(0) }
    var found by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var hasPermission by remember { mutableStateOf<Boolean?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var huntJob by remember { mutableStateOf<Job?>(null) }

    // Check WRITE_SECURE_SETTINGS permission on launch
    LaunchedEffect(Unit) {
        hasPermission = checkAirplanePermission(context)
        currentIp = getDeviceIp() ?: "No IP"
    }

    // Auto-scroll log to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("IP Hunter", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Description
            Text(
                text = "Hunt for a specific IP by cycling airplane mode. " +
                        "Enter a target IP prefix and the app will toggle " +
                        "flight mode on/off until the ISP assigns a matching IP.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission status
            if (hasPermission == false) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StatusDisconnected.copy(alpha = 0.1f))
                        .border(1.dp, StatusDisconnected.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.Warning, null, tint = StatusDisconnected, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant permission via ADB:\nadb shell pm grant com.bypnet.app android.permission.WRITE_SECURE_SETTINGS",
                        color = StatusDisconnected,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Target IP input
            Text("Target IP Prefix", color = Cyan400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = targetPrefix,
                onValueChange = { targetPrefix = it },
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Cyan400),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (targetPrefix.isEmpty()) {
                            Text("e.g. 102.68. or 41.90.64.12", color = TextTertiary, fontSize = 14.sp)
                        }
                        inner()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Current IP & Attempts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Current IP", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = currentIp,
                        color = if (found) StatusConnected else Cyan400,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Attempts", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = "$attempts",
                        color = Cyan400,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start / Stop button
            Button(
                onClick = {
                    if (isHunting) {
                        // Stop hunting
                        huntJob?.cancel()
                        isHunting = false
                        logs = logs + "⏹ Hunting stopped by user"
                    } else {
                        // Start hunting
                        isHunting = true
                        found = false
                        attempts = 0
                        logs = listOf("▶ Starting IP hunt for prefix: $targetPrefix")

                        huntJob = scope.launch {
                            val canToggle = checkAirplanePermission(context)
                            if (!canToggle) {
                                logs = logs + "✗ Cannot toggle airplane mode — permission not granted"
                                logs = logs + "Run: adb shell pm grant com.bypnet.app android.permission.WRITE_SECURE_SETTINGS"
                                isHunting = false
                                hasPermission = false
                                return@launch
                            }

                            while (isActive && !found) {
                                attempts++
                                logs = logs + "#$attempts → Enabling airplane mode..."

                                // Enable airplane mode
                                setAirplaneMode(context, true)
                                delay(2000)

                                // Disable airplane mode
                                logs = logs + "#$attempts → Disabling airplane mode..."
                                setAirplaneMode(context, false)

                                // Wait for connectivity to come back
                                logs = logs + "#$attempts → Waiting for network..."
                                var waitCount = 0
                                while (isActive && waitCount < 30) {
                                    delay(1000)
                                    val ip = getDeviceIp()
                                    if (ip != null) {
                                        currentIp = ip
                                        break
                                    }
                                    waitCount++
                                }

                                val ip = getDeviceIp()
                                if (ip == null) {
                                    logs = logs + "#$attempts → ✗ No IP acquired, retrying..."
                                    continue
                                }
                                currentIp = ip

                                if (targetPrefix.isNotEmpty() && ip.startsWith(targetPrefix)) {
                                    found = true
                                    isHunting = false
                                    logs = logs + "#$attempts → $ip ✓ MATCH FOUND!"
                                } else {
                                    logs = logs + "#$attempts → $ip (no match)"
                                }

                                // Rate-limit cycles
                                if (!found && isActive) delay(1000)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHunting) StatusDisconnected else Cyan400,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = targetPrefix.isNotEmpty() || isHunting
            ) {
                Icon(
                    imageVector = if (isHunting) Icons.Filled.Stop else Icons.Filled.FlightTakeoff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHunting) "STOP HUNTING" else "START HUNTING",
                    fontWeight = FontWeight.Bold
                )
            }

            // Found banner
            if (found) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusConnected.copy(alpha = 0.1f))
                        .border(1.dp, StatusConnected.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = StatusConnected, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IP Found: $currentIp", color = StatusConnected, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hunt Log
            Text("Hunt Log", color = Cyan400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                items(logs) { line ->
                    Text(
                        text = line,
                        color = when {
                            "MATCH" in line -> StatusConnected
                            "no match" in line -> TextSecondary
                            "✗" in line -> StatusDisconnected
                            else -> TextPrimary
                        },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Check if we have the WRITE_SECURE_SETTINGS permission needed
 * for toggling airplane mode programmatically.
 */
private fun checkAirplanePermission(context: Context): Boolean {
    return try {
        // Try reading airplane_mode_on — if we can write it we have permission
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON)
        // Attempt a no-op write to verify we have write permission
        val current = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON)
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, current)
        true
    } catch (e: SecurityException) {
        false
    } catch (e: Exception) {
        false
    }
}

/**
 * Toggle airplane mode on or off using Settings.Global.
 * Requires WRITE_SECURE_SETTINGS permission granted via ADB.
 */
private fun setAirplaneMode(context: Context, enable: Boolean) {
    try {
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            if (enable) 1 else 0
        )
        // Broadcast the change so the system actually toggles radios
        val intent = android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED)
        intent.putExtra("state", enable)
        context.sendBroadcast(intent)
    } catch (e: Exception) {
        // Permission not granted
    }
}

/**
 * Get the current IPv4 address of the device from mobile data interface.
 */
private fun getDeviceIp(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (intf in interfaces) {
            // Prefer mobile data interfaces (rmnet, ccmni, wwan)
            val name = intf.name.lowercase()
            if (name.startsWith("lo") || name.startsWith("dummy")) continue

            for (addr in intf.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val ip = addr.hostAddress
                    if (ip != null && !ip.startsWith("127.") && !ip.startsWith("169.254.")) {
                        return ip
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Ignore
    }
    return null
}
