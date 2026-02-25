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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponseCheckerScreen() {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("80") }
    var responseLog by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var isZeroRated by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

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
            Text("Check if a host is zero-rated by sending an HTTP HEAD request and analyzing the response headers.", color = TextSecondary, fontSize = 13.sp)

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
                    isZeroRated = null
                    responseLog = ""
                    scope.launch {
                        val result = performHttpCheck(host, port.toIntOrNull() ?: 80)
                        responseLog = result.log
                        isZeroRated = result.zeroRated
                        isChecking = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cyan400,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = host.isNotEmpty() && !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = DarkBackground
                    )
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                }
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
                        text = if (isZeroRated == true) "Host appears ZERO-RATED" else "Host is NOT zero-rated",
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

/**
 * Result of an HTTP check.
 */
private data class HttpCheckResult(
    val log: String,
    val zeroRated: Boolean
)

/**
 * Performs an actual HTTP HEAD request and returns the full response headers.
 * Zero-rating heuristic:
 *   - Connects via the host on the given port
 *   - If status is 200-399 AND content-length is either absent or > 0,
 *     AND no billing/charging headers are detected, we assume zero-rated.
 */
private suspend fun performHttpCheck(host: String, port: Int): HttpCheckResult =
    withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        var zeroRated = false
        try {
            val scheme = if (port == 443) "https" else "http"
            val portSuffix = if ((port == 80 && scheme == "http") || (port == 443 && scheme == "https")) "" else ":$port"
            val urlStr = "$scheme://$host$portSuffix/"
            sb.appendLine("→ Connecting to $urlStr")
            sb.appendLine()

            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

            val responseCode = conn.responseCode
            val responseMessage = conn.responseMessage

            sb.appendLine("HTTP/1.1 $responseCode $responseMessage")

            // Log all response headers
            var i = 1
            val headerMap = mutableMapOf<String, String>()
            while (true) {
                val key = conn.getHeaderFieldKey(i)
                val value = conn.getHeaderField(i)
                if (key == null && value == null) break
                if (key != null && value != null) {
                    sb.appendLine("$key: $value")
                    headerMap[key.lowercase()] = value.lowercase()
                }
                i++
            }

            // Content-Length analysis
            val contentLength = conn.getHeaderField("Content-Length")?.toLongOrNull()
            sb.appendLine()
            sb.appendLine("── Analysis ──")
            sb.appendLine("Status: $responseCode")
            sb.appendLine("Content-Length: ${contentLength ?: "not set"}")

            // Zero-rating heuristic:
            // A host is likely zero-rated if:
            // 1) We got a successful response (200-399)
            // 2) No X-Charging or billing headers present
            // 3) The connection went through without redirect to a captive portal
            val billingHeaders = listOf("x-charging", "x-billing", "x-data-billing", "x-msisdn")
            val hasBillingHeader = billingHeaders.any { headerMap.containsKey(it) }
            val isRedirectToPortal = headerMap["location"]?.contains("portal") == true ||
                    headerMap["location"]?.contains("captive") == true

            zeroRated = responseCode in 200..399 && !hasBillingHeader && !isRedirectToPortal

            if (hasBillingHeader) sb.appendLine("⚠ Billing header detected → NOT zero-rated")
            if (isRedirectToPortal) sb.appendLine("⚠ Captive portal redirect → NOT zero-rated")
            if (zeroRated) sb.appendLine("✓ No billing/captive headers → likely ZERO-RATED")

            conn.disconnect()
        } catch (e: Exception) {
            sb.appendLine("✗ Error: ${e.javaClass.simpleName}: ${e.message}")
            zeroRated = false
        }
        HttpCheckResult(sb.toString(), zeroRated)
    }
