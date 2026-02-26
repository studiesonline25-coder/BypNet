package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.bypnet.app.config.SessionManager
import com.bypnet.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadEditorScreen() {
    var payload by remember { mutableStateOf(SessionManager.payload) }
    var proxyHost by remember { mutableStateOf(SessionManager.proxyHost) }
    var proxyPort by remember { mutableStateOf(SessionManager.proxyPort) }

    // Tab state: 0 = Raw Editor, 1 = Simple Maker
    var selectedTab by remember { mutableIntStateOf(0) }

    // Simple Maker state
    var requestMethod by remember { mutableStateOf("CONNECT") }
    var methodExpanded by remember { mutableStateOf(false) }
    var injectionMethod by remember { mutableStateOf("Normal") }
    var injectionExpanded by remember { mutableStateOf(false) }
    var makerHost by remember { mutableStateOf("[host]:[port]") }
    var userAgent by remember { mutableStateOf("Android") }
    var uaExpanded by remember { mutableStateOf(false) }
    var extraHeaders by remember { mutableStateOf("") }

    val requestMethods = listOf("CONNECT", "GET", "POST", "HEAD", "PUT", "DELETE")
    val injectionMethods = listOf("Normal", "Front Injection", "Back Injection", "Split Injection")
    val userAgents = mapOf(
        "Android" to "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Chrome" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Firefox" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
        "Safari" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
        "None" to ""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("Payload & Proxy", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        // Tabs: Raw Editor | Simple Maker
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = Cyan400,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Cyan400
                    )
                }
            },
            divider = { HorizontalDivider(color = DarkBorder) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text("RAW EDITOR", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) Cyan400 else TextSecondary, fontSize = 12.sp)
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text("SIMPLE MAKER", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) Cyan400 else TextSecondary, fontSize = 12.sp)
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (selectedTab == 0) {
                // ── Raw Payload Editor ──
                Text("Payload", color = Cyan400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 280.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    BasicTextField(
                        value = payload,
                        onValueChange = { payload = it; SessionManager.payload = it },
                        textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
                        cursorBrush = SolidColor(Cyan400),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Variable tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("[host]", "[port]", "[crlf]", "[sni]", "[cookie]", "[host_port]").forEach { tag ->
                        Text(
                            tag, color = Cyan400, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Cyan400.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Remote Proxy
                Text("REMOTE PROXY", color = Cyan400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProxyField(proxyHost, { proxyHost = it; SessionManager.proxyHost = it }, "Proxy Host", Modifier.weight(2f))
                    ProxyField(proxyPort, { proxyPort = it; SessionManager.proxyPort = it }, "Port", Modifier.weight(1f))
                }

            } else {
                // ── Simple Maker ──
                Text("PAYLOAD GENERATOR", color = Cyan400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(10.dp))

                // Request Method
                Text("Request Method", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = !methodExpanded }) {
                    OutlinedTextField(
                        value = requestMethod, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(methodExpanded) },
                        colors = dropdownColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    )
                    ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }, modifier = Modifier.background(DarkSurface)) {
                        requestMethods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m, color = if (m == requestMethod) Cyan400 else TextPrimary) },
                                onClick = { requestMethod = m; methodExpanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Injection Method
                Text("Injection Method", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(expanded = injectionExpanded, onExpandedChange = { injectionExpanded = !injectionExpanded }) {
                    OutlinedTextField(
                        value = injectionMethod, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(injectionExpanded) },
                        colors = dropdownColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                    ExposedDropdownMenu(expanded = injectionExpanded, onDismissRequest = { injectionExpanded = false }, modifier = Modifier.background(DarkSurface)) {
                        injectionMethods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m, color = if (m == injectionMethod) Cyan400 else TextPrimary) },
                                onClick = { injectionMethod = m; injectionExpanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Host
                Text("Host", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = makerHost,
                    onValueChange = { makerHost = it },
                    colors = dropdownColors(),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // User Agent
                Text("User Agent", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(expanded = uaExpanded, onExpandedChange = { uaExpanded = !uaExpanded }) {
                    OutlinedTextField(
                        value = userAgent, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(uaExpanded) },
                        colors = dropdownColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                    ExposedDropdownMenu(expanded = uaExpanded, onDismissRequest = { uaExpanded = false }, modifier = Modifier.background(DarkSurface)) {
                        userAgents.keys.forEach { ua ->
                            DropdownMenuItem(
                                text = { Text(ua, color = if (ua == userAgent) Cyan400 else TextPrimary) },
                                onClick = { userAgent = ua; uaExpanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Extra Headers
                Text("Extra Headers (optional)", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = extraHeaders,
                    onValueChange = { extraHeaders = it },
                    colors = dropdownColors(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    placeholder = { Text("X-Custom: value[crlf]", color = TextTertiary, fontSize = 13.sp) },
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Generate Button
                Button(
                    onClick = {
                        val generated = generatePayload(requestMethod, injectionMethod, makerHost, userAgent, userAgents, extraHeaders)
                        payload = generated
                        SessionManager.payload = generated
                        selectedTab = 0 // Switch to raw editor to show result
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = DarkBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("GENERATE PAYLOAD", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dropdownColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Cyan400,
    unfocusedBorderColor = DarkBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkCard,
    unfocusedContainerColor = DarkCard,
    focusedTrailingIconColor = Cyan400,
    unfocusedTrailingIconColor = TextSecondary,
    cursorColor = Cyan400
)

@Composable
private fun ProxyField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(Cyan400),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (value.isEmpty()) Text(placeholder, color = TextTertiary, fontSize = 14.sp)
                inner()
            }
        },
        modifier = modifier
    )
}

private fun generatePayload(
    method: String,
    injection: String,
    host: String,
    userAgent: String,
    userAgents: Map<String, String>,
    extraHeaders: String
): String {
    val crlf = "[crlf]"
    val sb = StringBuilder()
    val uaString = userAgents[userAgent] ?: ""

    when (injection) {
        "Normal" -> {
            when (method) {
                "CONNECT" -> sb.append("$method $host HTTP/1.1${crlf}Host: $host$crlf")
                "GET" -> sb.append("$method / HTTP/1.1${crlf}Host: [sni]$crlf")
                "POST" -> sb.append("$method / HTTP/1.1${crlf}Host: [sni]${crlf}Content-Length: 0$crlf")
                else -> sb.append("$method / HTTP/1.1${crlf}Host: [sni]$crlf")
            }
            if (uaString.isNotEmpty()) sb.append("User-Agent: $uaString$crlf")
            sb.append("Connection: Keep-Alive$crlf")
            if (extraHeaders.isNotEmpty()) sb.append("$extraHeaders$crlf")
            sb.append(crlf)
        }
        "Front Injection" -> {
            // Payload before the real request
            sb.append("GET / HTTP/1.1${crlf}Host: [sni]${crlf}$crlf")
            when (method) {
                "CONNECT" -> sb.append("$method $host HTTP/1.1${crlf}Host: $host$crlf")
                else -> sb.append("$method / HTTP/1.1${crlf}Host: [sni]$crlf")
            }
            if (uaString.isNotEmpty()) sb.append("User-Agent: $uaString$crlf")
            sb.append("Connection: Keep-Alive$crlf")
            if (extraHeaders.isNotEmpty()) sb.append("$extraHeaders$crlf")
            sb.append(crlf)
        }
        "Back Injection" -> {
            when (method) {
                "CONNECT" -> sb.append("$method $host HTTP/1.1${crlf}Host: $host$crlf")
                else -> sb.append("$method / HTTP/1.1${crlf}Host: [sni]$crlf")
            }
            if (uaString.isNotEmpty()) sb.append("User-Agent: $uaString$crlf")
            sb.append("Connection: Keep-Alive$crlf")
            if (extraHeaders.isNotEmpty()) sb.append("$extraHeaders$crlf")
            sb.append(crlf)
            // Injected request after
            sb.append("GET / HTTP/1.1${crlf}Host: [sni]${crlf}$crlf")
        }
        "Split Injection" -> {
            // Split the CONNECT across two writes
            sb.append("CONNECT $host HTTP/1.1${crlf}")
            sb.append("[split]")
            sb.append("Host: $host$crlf")
            if (uaString.isNotEmpty()) sb.append("User-Agent: $uaString$crlf")
            sb.append("Connection: Keep-Alive$crlf")
            if (extraHeaders.isNotEmpty()) sb.append("$extraHeaders$crlf")
            sb.append(crlf)
        }
    }

    return sb.toString()
}
