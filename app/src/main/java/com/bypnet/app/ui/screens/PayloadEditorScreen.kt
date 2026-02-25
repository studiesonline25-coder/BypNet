package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadEditorScreen() {
    var payload = com.bypnet.app.config.SessionManager.payload
    var proxyHost = com.bypnet.app.config.SessionManager.proxyHost
    var proxyPort = com.bypnet.app.config.SessionManager.proxyPort
    var autoReplace by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("Payload & Proxy", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Payload Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Payload", color = Cyan400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto Replace", color = TextSecondary, fontSize = 12.sp)
                    Checkbox(
                        checked = autoReplace,
                        onCheckedChange = { autoReplace = it },
                        colors = CheckboxDefaults.colors(checkedColor = Cyan400),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 250.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = payload,
                    onValueChange = { 
                        payload = it
                        com.bypnet.app.config.SessionManager.payload = it
                    },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(Cyan400),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Variable hints
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("[host]", "[port]", "[crlf]", "[sni]", "[cookie]").forEach { tag ->
                    Text(
                        text = tag,
                        color = Cyan400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Cyan400.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Proxy Section
            Text("Remote Proxy", color = Cyan400, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    value = proxyHost,
                    onValueChange = { 
                        proxyHost = it
                        com.bypnet.app.config.SessionManager.proxyHost = it
                    },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(Cyan400),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (proxyHost.isEmpty()) Text("Proxy Host", color = TextTertiary, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(2f)
                )
                BasicTextField(
                    value = proxyPort,
                    onValueChange = { 
                        proxyPort = it
                        com.bypnet.app.config.SessionManager.proxyPort = it
                    },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(Cyan400),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkCard)
                                .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (proxyPort.isEmpty()) Text("Port", color = TextTertiary, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
