package com.bypnet.app.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

/**
 * BNID screen — BypNet's device ID tool (HC's HWID equivalent).
 * Shows device hardware ID / Android ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BnidScreen() {
    val context = LocalContext.current
    val androidId = remember {
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "Unknown"
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("BNID", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BYPNET DEVICE ID", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(16.dp))

            Text("Your Device BNID", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = androidId,
                onValueChange = {},
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkCard, unfocusedContainerColor = DarkCard
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("BNID", androidId))
                    Toast.makeText(context, "BNID copied!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusConnected, contentColor = DarkBackground),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("COPY BNID", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text("DEVICE INFO", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))

            DeviceInfoRow("Model", android.os.Build.MODEL)
            DeviceInfoRow("Manufacturer", android.os.Build.MANUFACTURER)
            DeviceInfoRow("Android", "API ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})")
            DeviceInfoRow("Board", android.os.Build.BOARD)
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
