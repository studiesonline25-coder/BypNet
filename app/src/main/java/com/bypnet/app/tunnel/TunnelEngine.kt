package com.bypnet.app.tunnel

/**
 * Connection status for the tunnel engine.
 */
enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Callback interface for tunnel state changes.
 */
interface TunnelCallback {
    fun onStatusChanged(status: TunnelStatus)
    fun onLog(message: String, level: String = "INFO")
    fun onSpeedUpdate(uploadBps: Long, downloadBps: Long)
    fun onError(message: String, throwable: Throwable? = null)
}

/**
 * Abstract base class for all tunnel protocol engines.
 * Each protocol (SSH, SSL, HTTP, V2Ray, etc.) implements this interface.
 */
abstract class TunnelEngine {

    var callback: TunnelCallback? = null
    var status: TunnelStatus = TunnelStatus.DISCONNECTED
        protected set

    /**
     * Connect to the remote server using the provided configuration.
     * This runs on a background thread.
     */
    abstract suspend fun connect(config: TunnelConfig)

    /**
     * Disconnect and clean up resources.
     */
    abstract suspend fun disconnect()

    /**
     * Check if the tunnel is currently connected.
     */
    fun isConnected(): Boolean = status == TunnelStatus.CONNECTED

    /**
     * Update the status and notify the callback.
     */
    protected fun updateStatus(newStatus: TunnelStatus) {
        status = newStatus
        callback?.onStatusChanged(newStatus)
    }

    /**
     * Log a message via the callback.
     */
    protected fun log(message: String, level: String = "INFO") {
        callback?.onLog(message, level)
    }

    /**
     * Report an error via the callback.
     */
    protected fun reportError(message: String, throwable: Throwable? = null) {
        callback?.onError(message, throwable)
        updateStatus(TunnelStatus.ERROR)
    }
}

/**
 * Configuration data for a tunnel connection.
 */
data class TunnelConfig(
    val protocol: String,
    val serverHost: String,
    val serverPort: Int,
    val username: String = "",
    val password: String = "",
    val sni: String = "",
    val payload: String = "",
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val primaryDns: String = "8.8.8.8",
    val secondaryDns: String = "8.8.4.4",
    val cookies: String = "",
    val extraConfig: Map<String, Any> = emptyMap()
)
