package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.config.SessionManager
import com.bypnet.app.ui.theme.*

/**
 * Simple Maker — HC's payload generator (full-screen).
 * Matches HTTP Custom exactly: MERGER/SPLIT, Rotation, Request Method,
 * Injection Method, checkboxes, User Agent, EOL, Remote Proxy, Generate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMakerScreen(onBack: () -> Unit = {}) {
    // Mode
    var merger by remember { mutableStateOf(true) }
    var rotation by remember { mutableStateOf(false) }
    var splitDelay by remember { mutableStateOf(false) }

    // Host
    var host by remember { mutableStateOf("") }

    // Request method
    var requestMethod by remember { mutableStateOf("ACL") }
    var requestExpanded by remember { mutableStateOf(false) }
    val requestMethods = listOf("ACL", "CONNECT", "GET", "POST", "HEAD", "PUT", "DELETE", "OPTIONS", "TRACE", "PATCH")

    // Injection method
    var injectionMethod by remember { mutableStateOf("Normal") }
    var injectionExpanded by remember { mutableStateOf(false) }
    val injectionMethods = listOf("Normal", "Front Inject", "Back Inject", "Split")

    // Checkboxes
    var keepAlive by remember { mutableStateOf(false) }
    var onlineHost by remember { mutableStateOf(false) }
    var reverseProxy by remember { mutableStateOf(false) }
    var forwardHost by remember { mutableStateOf(false) }
    var realRequest by remember { mutableStateOf(false) }
    var dualRequest by remember { mutableStateOf(false) }

    // User Agent
    var userAgent by remember { mutableStateOf("None") }
    var uaExpanded by remember { mutableStateOf(false) }
    val userAgents = listOf("None", "Android", "Chrome", "Firefox", "Safari", "Edge", "Opera", "Custom")

    // EOL
    var sumEol by remember { mutableStateOf("Default 2") }

    // Remote Proxy
    var remoteProxy by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        // Top bar with back arrow
        TopAppBar(
            title = { Text("Simple Maker", color = TextPrimary, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── MERGER / SPLIT radio ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = merger,
                    onClick = { merger = true },
                    colors = RadioButtonDefaults.colors(selectedColor = StatusConnected, unselectedColor = TextTertiary)
                )
                Text("MERGER", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Spacer(Modifier.width(32.dp))

                RadioButton(
                    selected = !merger,
                    onClick = { merger = false },
                    colors = RadioButtonDefaults.colors(selectedColor = StatusConnected, unselectedColor = TextTertiary)
                )
                Text("SPLIT", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Rotation / Split Delay
            Row(modifier = Modifier.fillMaxWidth()) {
                HcCheckbox("Rotation", rotation, { rotation = it }, Modifier.weight(1f))
                HcCheckbox("Split Delay", splitDelay, { splitDelay = it }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            // ── Host ──
            HcUnderlineField("Host (ex. eprodev.org)", host, { host = it })

            Spacer(Modifier.height(12.dp))

            // ── Request Method + Injection Method (side by side) ──
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Request method", color = TextTertiary, fontSize = 11.sp)
                    HcDropdown(requestMethod, requestMethods, requestExpanded, { requestExpanded = it }) {
                        requestMethod = it
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Injection Method", color = TextTertiary, fontSize = 11.sp)
                    HcDropdown(injectionMethod, injectionMethods, injectionExpanded, { injectionExpanded = it }) {
                        injectionMethod = it
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Checkboxes grid ──
            Row(modifier = Modifier.fillMaxWidth()) {
                HcCheckbox("Keep Alive", keepAlive, { keepAlive = it }, Modifier.weight(1f))
                HcCheckbox("Online Host", onlineHost, { onlineHost = it }, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                HcCheckbox("Reverse Proxy", reverseProxy, { reverseProxy = it }, Modifier.weight(1f))
                HcCheckbox("Forward Host", forwardHost, { forwardHost = it }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            // ── Payload section ──
            Text("Payload", color = TextTertiary, fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth()) {
                HcCheckbox("Real Request", realRequest, { realRequest = it }, Modifier.weight(1f))
                HcCheckbox("Dual Request", dualRequest, { dualRequest = it }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            // ── User Agent ──
            Text("User Agent", color = TextTertiary, fontSize = 11.sp)
            HcDropdown(userAgent, userAgents, uaExpanded, { uaExpanded = it }) { userAgent = it }

            Spacer(Modifier.height(12.dp))

            // ── Sum EOL ──
            Text("Sum EOL (End Of Line)", color = TextTertiary, fontSize = 11.sp)
            HcUnderlineField("", sumEol, { sumEol = it })

            Spacer(Modifier.height(12.dp))

            // ── Remote Proxy ──
            HcUnderlineField("Remote Proxy", remoteProxy, { remoteProxy = it })

            Spacer(Modifier.height(20.dp))

            // ── GENERATE PAYLOAD ──
            OutlinedButton(
                onClick = {
                    val generatedPayload = generateFullPayload(
                        merger, host, requestMethod, injectionMethod,
                        keepAlive, onlineHost, reverseProxy, forwardHost,
                        realRequest, dualRequest, userAgent, sumEol, remoteProxy
                    )
                    SessionManager.payload = generatedPayload
                    if (remoteProxy.isNotEmpty()) {
                        val parts = remoteProxy.split(":", limit = 2)
                        SessionManager.proxyHost = parts.getOrElse(0) { "" }
                        SessionManager.proxyPort = parts.getOrElse(1) { "8080" }
                    }
                },
                border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusConnected),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("GENERATE PAYLOAD", color = StatusConnected, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ── HC-style underline text field ──
@Composable
fun HcUnderlineField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { if (label.isNotEmpty()) Text(label, color = TextTertiary, fontSize = 14.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            cursorColor = StatusConnected
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    )
}

// ── HC-style dropdown ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HcDropdown(
    selected: String, options: List<String>,
    expanded: Boolean, onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { onExpandedChange(!expanded) }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkCard, unfocusedContainerColor = DarkCard,
                focusedTrailingIconColor = StatusConnected, unfocusedTrailingIconColor = TextSecondary
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(DarkSurface)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = if (opt == selected) StatusConnected else TextPrimary, fontSize = 13.sp) },
                    onClick = { onSelect(opt); onExpandedChange(false) }
                )
            }
        }
    }
}

// ── Payload generator ──
private fun generateFullPayload(
    merger: Boolean, host: String, method: String, injection: String,
    keepAlive: Boolean, onlineHost: Boolean, reverseProxy: Boolean, forwardHost: Boolean,
    realRequest: Boolean, dualRequest: Boolean, userAgent: String, eol: String, proxy: String
): String {
    val crlf = "[crlf]"
    val sb = StringBuilder()

    val hostVal = host.ifEmpty { "[host]" }
    val portVal = "[port]"

    when (injection) {
        "Front Inject" -> {
            sb.appendLine("GET / HTTP/1.1${crlf}Host: $hostVal$crlf$crlf")
            sb.appendLine("$method $hostVal:$portVal HTTP/1.1$crlf")
        }
        "Back Inject" -> {
            sb.appendLine("$method $hostVal:$portVal HTTP/1.1$crlf")
            sb.appendLine("GET / HTTP/1.1${crlf}Host: $hostVal$crlf$crlf")
        }
        "Split" -> {
            sb.appendLine("$method $hostVal:$portVal HTTP/1.1$crlf")
            sb.appendLine("[split]")
            sb.appendLine("Host: $hostVal$crlf")
        }
        else -> { // Normal
            sb.appendLine("$method $hostVal:$portVal HTTP/1.1$crlf")
            sb.appendLine("Host: $hostVal$crlf")
        }
    }

    if (keepAlive) sb.appendLine("Connection: Keep-Alive$crlf")
    if (forwardHost) sb.appendLine("X-Forwarded-Host: $hostVal$crlf")
    if (onlineHost) sb.appendLine("X-Online-Host: $hostVal$crlf")
    if (reverseProxy) sb.appendLine("X-Forward-For: $hostVal$crlf")

    if (userAgent != "None") {
        val ua = when (userAgent) {
            "Android" -> "Dalvik/2.1.0 (Linux; U; Android 12)"
            "Chrome" -> "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120.0.0.0"
            "Firefox" -> "Mozilla/5.0 (Android 12; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0"
            "Safari" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15"
            "Edge" -> "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120.0.0.0 Edg/120.0.0.0"
            "Opera" -> "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120.0.0.0 OPR/80.0.0.0"
            else -> userAgent
        }
        sb.appendLine("User-Agent: $ua$crlf")
    }

    sb.appendLine(crlf)
    return sb.toString().trim()
}
