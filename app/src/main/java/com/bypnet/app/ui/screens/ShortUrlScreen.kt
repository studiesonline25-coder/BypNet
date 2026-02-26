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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.components.BypNetTextField
import com.bypnet.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * ShortUrl Maker screen — HC's "ShortUrl Maker" tool.
 * Enter a long URL, get a shortened URL using a free API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortUrlScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var longUrl by remember { mutableStateOf("") }
    var shortUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text("ShortUrl Maker", color = TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("URL SHORTENER", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))

            BypNetTextField(
                value = longUrl,
                onValueChange = { longUrl = it },
                label = "Long URL",
                placeholder = "https://example.com/very/long/url/here"
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (longUrl.isNotEmpty()) {
                        isLoading = true
                        scope.launch {
                            shortUrl = try {
                                shortenUrl(longUrl)
                            } catch (e: Exception) {
                                "Error: ${e.message}"
                            }
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusConnected, contentColor = DarkBackground),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                enabled = !isLoading && longUrl.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DarkBackground)
                    Spacer(Modifier.width(8.dp))
                }
                Text("SHORTEN URL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            if (shortUrl.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("RESULT", color = StatusConnected, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = shortUrl,
                    onValueChange = {},
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkCard, unfocusedContainerColor = DarkCard
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Short URL", shortUrl))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.ContentCopy, null, tint = StatusConnected)
                        }
                    }
                )
            }
        }
    }
}

private suspend fun shortenUrl(url: String): String = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(url, "UTF-8")
    val apiUrl = "https://is.gd/create.php?format=simple&url=$encoded"
    val conn = URL(apiUrl).openConnection() as HttpURLConnection
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    try {
        conn.inputStream.bufferedReader().readText().trim()
    } finally {
        conn.disconnect()
    }
}
