package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
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
fun ResponseCheckerScreen() {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("80") }
    var responseLog by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var isZeroRated by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("Response Checker", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Check if a host is zero-rated", color = TextSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // Host input
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    value = host,
                    onValueChange = { host = it },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(Cyan400),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (host.isEmpty()) Text("Host (e.g. www.google.com)", color = TextTertiary, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(2f)
                )
                BasicTextField(
                    value = port,
                    onValueChange = { port = it },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(Cyan400),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (port.isEmpty()) Text("Port", color = TextTertiary, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Check button
            Button(
                onClick = {
                    isChecking = true
                    // TODO: Perform actual HTTP HEAD check and analyse response
                    responseLog = "HTTP/1.1 200 OK\n" +
                            "Server: nginx\n" +
                            "Content-Type: text/html\n" +
                            "Connection: keep-alive\n" +
                            "X-Cache: HIT\n\n" +
                            "→ Host appears to be ZERO-RATED ✓"
                    isZeroRated = true
                    isChecking = false
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cyan400,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = host.isNotEmpty() && !isChecking
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isChecking) "Checking..." else "Check Response", fontWeight = FontWeight.Bold)
            }

            // Result status
            if (isZeroRated != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isZeroRated == true) StatusConnected.copy(alpha = 0.1f)
                            else StatusDisconnected.copy(alpha = 0.1f)
                        )
                        .border(
                            1.dp,
                            if (isZeroRated == true) StatusConnected.copy(alpha = 0.3f)
                            else StatusDisconnected.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (isZeroRated == true) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (isZeroRated == true) StatusConnected else StatusDisconnected,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isZeroRated == true) "Host is ZERO-RATED" else "Host is NOT zero-rated",
                        color = if (isZeroRated == true) StatusConnected else StatusDisconnected,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Response log
            if (responseLog.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Response Log", color = Cyan400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = responseLog,
                        color = TextPrimary.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
