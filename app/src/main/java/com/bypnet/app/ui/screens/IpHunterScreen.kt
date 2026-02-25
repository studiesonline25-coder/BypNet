package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpHunterScreen() {
    var targetIp by remember { mutableStateOf("") }
    var targetPrefix by remember { mutableStateOf("") }
    var isHunting by remember { mutableStateOf(false) }
    var currentIp by remember { mutableStateOf("---") }
    var attempts by remember { mutableIntStateOf(0) }
    var found by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<String>()) }

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
                        "Enter a target IP or IP prefix and the app will toggle " +
                        "flight mode on/off until the ISP assigns a matching IP.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target IP input
            Text("Target IP or Prefix", color = Cyan400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                        isHunting = false
                        logs = logs + "⏹ Hunting stopped by user"
                    } else {
                        isHunting = true
                        found = false
                        attempts = 0
                        logs = listOf("▶ Starting IP hunt for prefix: $targetPrefix")
                        // TODO: Actually toggle airplane mode via Settings.System
                        // and poll connectivity for new IP assignment.
                        // Each cycle: enable airplane -> wait -> disable -> get IP -> compare
                        // Requires WRITE_SECURE_SETTINGS or root/ADB grant:
                        //   adb shell pm grant com.bypnet.app android.permission.WRITE_SECURE_SETTINGS

                        // Simulated demo:
                        attempts = 1
                        currentIp = "102.68.34.87"
                        logs = logs + "#1 → 102.68.34.87 (no match)"
                        attempts = 2
                        currentIp = "102.68.12.5"
                        logs = logs + "#2 → 102.68.12.5 (no match)"
                        attempts = 3
                        currentIp = "102.68.64.12"
                        if (targetPrefix.isNotEmpty() && currentIp.startsWith(targetPrefix)) {
                            found = true
                            isHunting = false
                            logs = logs + "#3 → $currentIp ✓ MATCH FOUND!"
                        } else {
                            logs = logs + "#3 → $currentIp (no match)"
                            isHunting = false
                            logs = logs + "⏹ Demo ended (enable ADB permission for real hunting)"
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
