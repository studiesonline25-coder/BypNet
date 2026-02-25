package com.bypnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String
)

enum class LogLevel(val tag: String, val color: androidx.compose.ui.graphics.Color) {
    INFO("INF", Cyan400),
    WARN("WRN", StatusConnecting),
    ERROR("ERR", StatusDisconnected),
    SUCCESS("OK", StatusConnected),
    DEBUG("DBG", GradientPurpleStart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen() {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Sample logs for demo
    var logs by remember {
        mutableStateOf(
            listOf(
                LogEntry(level = LogLevel.INFO, message = "BypNet v1.0 initialized"),
                LogEntry(level = LogLevel.DEBUG, message = "Tunnel engine loaded"),
                LogEntry(level = LogLevel.INFO, message = "Protocols: SSH, SSL, HTTP, V2Ray, SS, WG, Trojan"),
                LogEntry(level = LogLevel.SUCCESS, message = "Database initialized"),
                LogEntry(level = LogLevel.INFO, message = "Ready to connect"),
            )
        )
    }

    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            // Auto-scroll toggle
            IconButton(
                onClick = { autoScroll = !autoScroll },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (autoScroll) Icons.Filled.VerticalAlignBottom
                    else Icons.Filled.VerticalAlignCenter,
                    contentDescription = "Auto scroll",
                    tint = if (autoScroll) Cyan400 else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Clear logs
            IconButton(
                onClick = { logs = emptyList() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Divider(color = DarkBorder, thickness = 0.5.dp)

        // Log list
        if (logs.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.TextSnippet,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No logs yet",
                        color = TextTertiary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Connection logs will appear here",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(logs) { entry ->
                    LogEntryRow(entry = entry, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
fun LogEntryRow(
    entry: LogEntry,
    dateFormat: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp)
    ) {
        // Timestamp
        Text(
            text = dateFormat.format(Date(entry.timestamp)),
            color = TextTertiary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Level tag
        Text(
            text = entry.level.tag,
            color = entry.level.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(entry.level.color.copy(alpha = 0.1f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Message
        Text(
            text = entry.message,
            color = TextPrimary.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp
        )
    }
}
