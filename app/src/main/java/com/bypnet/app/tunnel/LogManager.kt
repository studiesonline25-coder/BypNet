package com.bypnet.app.tunnel

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.bypnet.app.ui.screens.LogEntry
import com.bypnet.app.ui.screens.LogLevel

object LogManager {
    val logs = mutableStateListOf<LogEntry>()

    init {
        logs.addAll(
            listOf(
                LogEntry(level = LogLevel.INFO, message = "BypNet initialized"),
                LogEntry(level = LogLevel.INFO, message = "Ready to connect")
            )
        )
    }

    fun addLog(message: String, level: String) {
        val logLevel = when (level.uppercase()) {
            "SUCCESS" -> LogLevel.SUCCESS
            "ERROR" -> LogLevel.ERROR
            "WARN" -> LogLevel.WARN
            "DEBUG" -> LogLevel.DEBUG
            else -> LogLevel.INFO
        }
        Handler(Looper.getMainLooper()).post {
            logs.add(LogEntry(level = logLevel, message = message))
        }
    }

    fun clearLogs() {
        Handler(Looper.getMainLooper()).post {
            val initial = logs.take(2)
            logs.clear()
            logs.addAll(initial)
        }
    }
}
