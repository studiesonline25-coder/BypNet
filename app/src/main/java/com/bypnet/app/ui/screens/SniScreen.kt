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
 * SNI screen — HC's "SNI" utility drawer item.
 * SNI Host + SNI Bug Host fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SniScreen() {
    var sni by remember { mutableStateOf(SessionManager.sni) }
    var bugHost by remember { mutableStateOf(SessionManager.sniBugHost) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("SNI", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("SERVER NAME INDICATION", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            BypNetTextField(
                value = sni,
                onValueChange = { sni = it; SessionManager.sni = it },
                label = "SNI Host",
                placeholder = "bug.example.com"
            )
            Spacer(Modifier.height(16.dp))

            Text("BUG HOST", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            BypNetTextField(
                value = bugHost,
                onValueChange = { bugHost = it; SessionManager.sniBugHost = it },
                label = "SNI Bug Host",
                placeholder = "zero-rated.example.com"
            )
            Spacer(Modifier.height(10.dp))

            Text(
                "Enter a zero-rated or bug host for your carrier. This host will be used in the SNI extension during TLS handshake.",
                color = TextTertiary, fontSize = 12.sp
            )
        }
    }
}
