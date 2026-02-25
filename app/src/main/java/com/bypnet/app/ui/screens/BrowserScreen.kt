package com.bypnet.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bypnet.app.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen() {
    var url by remember { mutableStateOf("https://www.google.com") }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("Cookie Browser") }
    var isLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0) }
    var extractedCookies by remember { mutableStateOf("") }
    var interceptedHeaders by remember { mutableStateOf("") }
    var showCookieDialog by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Browser toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cookie Browser",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Navigate & extract cookies for payloads",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // URL bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCard)
                    .border(0.5.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (currentUrl.startsWith("https"))
                        Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = if (currentUrl.startsWith("https"))
                        StatusConnected else StatusConnecting,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Cyan400),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        webView?.loadUrl(
                            if (url.startsWith("http")) url
                            else "https://$url"
                        )
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = Cyan400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Navigation controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { webView?.goBack() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { webView?.goForward() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { webView?.reload() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Reload",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Extract cookies button
                Button(
                    onClick = {
                        val cookies = CookieManager.getInstance()
                            .getCookie(currentUrl) ?: ""
                        extractedCookies = cookies
                        showCookieDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan400,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Extract Cookies",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Loading progress bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Cyan400,
                trackColor = DarkSurface,
            )
        }

        // WebView
        val context = LocalContext.current
        AndroidView(
            factory = {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    // Enable cookies
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                            isLoading = true
                            pageUrl?.let { currentUrl = it }
                        }

                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            isLoading = false
                            loadingProgress = 100
                            pageUrl?.let {
                                currentUrl = it
                                url = it
                            }
                            view?.title?.let { pageTitle = it }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            // Capture outgoing request headers for Imperva bypass
                            request?.let { req ->
                                val headers = req.requestHeaders
                                if (headers != null && req.url.toString() == currentUrl) {
                                    val sb = StringBuilder()
                                    sb.appendLine("${req.method} ${req.url.path} HTTP/1.1")
                                    sb.appendLine("Host: ${req.url.host}")
                                    for ((key, value) in headers) {
                                        sb.appendLine("$key: $value")
                                    }
                                    // Also grab cookies from CookieManager
                                    val cookies = CookieManager.getInstance().getCookie(req.url.toString())
                                    if (!cookies.isNullOrEmpty()) {
                                        sb.appendLine("Cookie: $cookies")
                                    }
                                    interceptedHeaders = sb.toString()
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loadingProgress = newProgress
                        }
                    }

                    setBackgroundColor(0xFF0D1117.toInt())
                    loadUrl("https://www.google.com")
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Request interception dialog
    if (showCookieDialog) {
        var showHeaders by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showCookieDialog = false },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Intercepted Request", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Captured the exact request the browser sends. " +
                                "Use this to build payloads that bypass Imperva verification.",
                        color = StatusConnecting,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Domain: $currentUrl",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Toggle: Full Headers vs Cookies Only
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        TextButton(
                            onClick = { showHeaders = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (showHeaders) Cyan400 else TextSecondary
                            )
                        ) {
                            Text("Full Request", fontSize = 11.sp, fontWeight = if (showHeaders) FontWeight.Bold else FontWeight.Normal)
                        }
                        TextButton(
                            onClick = { showHeaders = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (!showHeaders) Cyan400 else TextSecondary
                            )
                        ) {
                            Text("Cookies Only", fontSize = 11.sp, fontWeight = if (!showHeaders) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCard)
                            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        val content = if (showHeaders) {
                            interceptedHeaders.ifEmpty { "No request intercepted yet — load a page first" }
                        } else {
                            extractedCookies.ifEmpty { "No cookies found — navigate to the target site first" }
                        }
                        Text(
                            text = content,
                            color = if (content.startsWith("No")) TextTertiary else Cyan400,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                val context = LocalContext.current
                Button(
                    onClick = {
                        val textToCopy = if (showHeaders) interceptedHeaders else extractedCookies
                        if (textToCopy.isNotEmpty()) {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText(
                                if (showHeaders) "BypNet Request" else "BypNet Cookies",
                                textToCopy
                            )
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "✓ Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showCookieDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan400,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = interceptedHeaders.isNotEmpty() || extractedCookies.isNotEmpty()
                ) {
                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (showHeaders) "Copy Request" else "Copy Cookies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCookieDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}
