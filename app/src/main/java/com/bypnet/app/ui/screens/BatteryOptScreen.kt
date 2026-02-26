package com.bypnet.app.ui.screens

import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

/**
 * Battery Optimization screen — HC's "Battery Optimization" tool.
 * Request exclusion from Android battery optimization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptScreen() {
    val context = LocalContext.current
    val pm = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager }
    var isIgnoring by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName)) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("Battery Optimization", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Icon(
                if (isIgnoring) Icons.Filled.BatteryChargingFull else Icons.Filled.BatterySaver,
                null,
                tint = if (isIgnoring) StatusConnected else StatusConnecting,
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                if (isIgnoring) "Battery optimization is DISABLED for BypNet"
                else "Battery optimization is ENABLED for BypNet",
                color = if (isIgnoring) StatusConnected else StatusConnecting,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Disabling battery optimization ensures the VPN service runs uninterrupted in the background. " +
                "Android may kill the VPN connection to save battery if optimization is enabled.",
                color = TextTertiary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            if (!isIgnoring) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                        // Re-check after user returns
                        isIgnoring = pm.isIgnoringBatteryOptimizations(context.packageName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusConnected, contentColor = DarkBackground),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.BatteryChargingFull, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DISABLE BATTERY OPTIMIZATION", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                        isIgnoring = pm.isIgnoringBatteryOptimizations(context.packageName)
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusConnected),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("OPEN BATTERY SETTINGS", color = StatusConnected, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
