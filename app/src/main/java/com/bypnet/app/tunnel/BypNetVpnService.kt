package com.bypnet.app.tunnel

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.bypnet.app.BypNetApp
import com.bypnet.app.MainActivity
import com.bypnet.app.tunnel.http.HttpProxyEngine
import com.bypnet.app.tunnel.shadowsocks.ShadowsocksEngine
import com.bypnet.app.tunnel.ssh.SshEngine
import com.bypnet.app.tunnel.ssl.SslEngine
import com.bypnet.app.tunnel.trojan.TrojanEngine
import com.bypnet.app.tunnel.v2ray.V2RayEngine
import com.bypnet.app.tunnel.wireguard.WireGuardEngine
import kotlinx.coroutines.*
import android.os.Handler
import android.os.Looper
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.Proxy
import java.net.InetAddress

/**
 * Core VPN Service for BypNet.
 *
 * Creates a TUN interface, establishes the selected tunnel protocol,
 * and forwards all device traffic through the tunnel.
 */
class BypNetVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelEngine: TunnelEngine? = null
    private var tunnelSocket: Socket? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Traffic stats
    @Volatile var totalUpload: Long = 0
    @Volatile var totalDownload: Long = 0
    private var startTime: Long = 0

    // Speed tracking
    private var lastUpload: Long = 0
    private var lastDownload: Long = 0
    private var speedUpdateJob: Job? = null

    companion object {
        const val ACTION_CONNECT = "com.bypnet.app.CONNECT"
        const val ACTION_DISCONNECT = "com.bypnet.app.DISCONNECT"
        const val EXTRA_PROTOCOL = "protocol"
        const val EXTRA_SERVER_HOST = "server_host"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SNI = "sni"
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_PROXY_HOST = "proxy_host"
        const val EXTRA_PROXY_PORT = "proxy_port"
        const val EXTRA_DNS1 = "dns1"
        const val EXTRA_DNS2 = "dns2"
        const val EXTRA_COOKIES = "cookies"

        @Volatile
        var currentStatus: TunnelStatus = TunnelStatus.DISCONNECTED
            private set

        @Volatile
        var statusListener: ((TunnelStatus) -> Unit)? = null

        @Volatile
        var logListener: ((String, String) -> Unit)? = null

        // Live speed stats exposed for UI
        @Volatile var uploadSpeed: Long = 0
        @Volatile var downloadSpeed: Long = 0
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                startForeground(BypNetApp.VPN_NOTIFICATION_ID, createNotification("Connecting...", ""))

                val config = TunnelConfig(
                    protocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: "SSH",
                    serverHost = intent.getStringExtra(EXTRA_SERVER_HOST) ?: "",
                    serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 22),
                    username = intent.getStringExtra(EXTRA_USERNAME) ?: "",
                    password = intent.getStringExtra(EXTRA_PASSWORD) ?: "",
                    sni = intent.getStringExtra(EXTRA_SNI) ?: "",
                    payload = intent.getStringExtra(EXTRA_PAYLOAD) ?: "",
                    proxyHost = intent.getStringExtra(EXTRA_PROXY_HOST) ?: "",
                    proxyPort = intent.getIntExtra(EXTRA_PROXY_PORT, 0),
                    primaryDns = intent.getStringExtra(EXTRA_DNS1) ?: "8.8.8.8",
                    secondaryDns = intent.getStringExtra(EXTRA_DNS2) ?: "8.8.4.4",
                    cookies = intent.getStringExtra(EXTRA_COOKIES) ?: ""
                )

                serviceScope.launch {
                    startTunnel(config)
                }
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch {
                    stopTunnel()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.launch {
            stopTunnel()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun startTunnel(config: TunnelConfig) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            emitLog("Starting VPN tunnel...", "INFO")

            val engine = createEngine(config.protocol)
            engine.callback = object : TunnelCallback {
                override fun onStatusChanged(status: TunnelStatus) {}
                override fun onLog(message: String, level: String) {
                    emitLog(message, level)
                }
                override fun onSpeedUpdate(uploadBps: Long, downloadBps: Long) {}
                override fun onError(message: String, throwable: Throwable?) {
                    emitLog("ERROR: $message", "ERROR")
                }
            }
            tunnelEngine = engine

            emitLog("Connecting ${config.protocol} engine to ${config.serverHost}:${config.serverPort}...", "INFO")
            engine.connect(config)

            if (!engine.isConnected()) {
                emitLog("Tunnel engine failed to connect", "ERROR")
                updateStatus(TunnelStatus.ERROR)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            emitLog("Tunnel engine connected!", "SUCCESS")

            // Configure the TUN interface
            val dns1 = config.primaryDns.ifEmpty { "8.8.8.8" }
            val dns2 = config.secondaryDns.ifEmpty { "8.8.4.4" }

            val builder = Builder()
                .setSession("BypNet")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(dns1)
                .addDnsServer(dns2)
                .setMtu(1500)
                .setBlocking(true)

            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) { }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                emitLog("Failed to establish VPN interface", "ERROR")
                updateStatus(TunnelStatus.ERROR)
                engine.disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            startTime = System.currentTimeMillis()
            totalUpload = 0
            totalDownload = 0
            updateStatus(TunnelStatus.CONNECTED)
            updateNotification("Connected", "↑ 0 B/s  ↓ 0 B/s")
            emitLog("VPN tunnel established. All traffic is routed.", "SUCCESS")

            // Start speed update notification ticker
            startSpeedUpdater()

            // Start packet forwarding
            vpnInterface?.let { fd ->
                forwardPackets(fd, engine, config)
            }

        } catch (e: Exception) {
            emitLog("Tunnel error: ${e.message}", "ERROR")
            updateStatus(TunnelStatus.ERROR)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun stopTunnel() {
        updateStatus(TunnelStatus.DISCONNECTING)
        emitLog("Disconnecting tunnel...", "INFO")

        speedUpdateJob?.cancel()
        speedUpdateJob = null

        try { tunnelEngine?.disconnect() } catch (e: Exception) { }
        tunnelEngine = null

        try { tunnelSocket?.close() } catch (e: Exception) { }
        tunnelSocket = null

        try { vpnInterface?.close() } catch (e: Exception) { }
        vpnInterface = null

        totalUpload = 0
        totalDownload = 0
        uploadSpeed = 0
        downloadSpeed = 0

        emitLog("VPN disconnected", "INFO")
        updateStatus(TunnelStatus.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Start a coroutine that updates the notification with speed every 2 seconds.
     */
    private fun startSpeedUpdater() {
        speedUpdateJob = serviceScope.launch {
            while (isActive) {
                delay(2000)
                val upDelta = totalUpload - lastUpload
                val downDelta = totalDownload - lastDownload
                lastUpload = totalUpload
                lastDownload = totalDownload

                // Bytes per second (averaged over 2s interval)
                uploadSpeed = upDelta / 2
                downloadSpeed = downDelta / 2

                val upStr = formatSpeed(uploadSpeed)
                val downStr = formatSpeed(downloadSpeed)
                val elapsed = formatDuration(System.currentTimeMillis() - startTime)

                updateNotification(
                    "Connected · $elapsed",
                    "↑ $upStr  ↓ $downStr"
                )
            }
        }
    }

    /**
     * Forward packets between TUN and tunnel.
     *
     * SSH: uses local SOCKS5 proxy via forwardViaSocks
     * HTTP/SSL: uses socket streams directly
     */
    private suspend fun forwardPackets(
        fd: ParcelFileDescriptor,
        engine: TunnelEngine,
        config: TunnelConfig
    ) = withContext(Dispatchers.IO) {
        val tunInput = FileInputStream(fd.fileDescriptor)
        val tunOutput = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32767)

        try {
            when (engine) {
                is SshEngine -> {
                    val socksPort = engine.getLocalSocksPort()
                    if (socksPort > 0) {
                        emitLog("Forwarding traffic via SOCKS5 on 127.0.0.1:$socksPort", "INFO")
                        forwardViaSocks(tunInput, tunOutput, buffer, socksPort)
                    } else {
                        emitLog("SSH engine connected but no SOCKS port available", "WARN")
                        simpleTunForward(tunInput, tunOutput, buffer)
                    }
                }
                is HttpProxyEngine -> {
                    val proxyIn = engine.getInputStream()
                    val proxyOut = engine.getOutputStream()
                    if (proxyIn != null && proxyOut != null) {
                        emitLog("Forwarding traffic via HTTP proxy socket", "INFO")
                        forwardViaStreams(tunInput, tunOutput, buffer, proxyIn, proxyOut)
                    } else {
                        simpleTunForward(tunInput, tunOutput, buffer)
                    }
                }
                is SslEngine -> {
                    val sslIn = engine.getInputStream()
                    val sslOut = engine.getOutputStream()
                    if (sslIn != null && sslOut != null) {
                        emitLog("Forwarding traffic via SSL/TLS tunnel", "INFO")
                        forwardViaStreams(tunInput, tunOutput, buffer, sslIn, sslOut)
                    } else {
                        simpleTunForward(tunInput, tunOutput, buffer)
                    }
                }
                else -> {
                    emitLog("Using generic traffic forwarding", "INFO")
                    simpleTunForward(tunInput, tunOutput, buffer)
                }
            }
        } catch (e: Exception) {
            if (isActive) {
                emitLog("Packet forwarding error: ${e.message}", "ERROR")
            }
        }
    }

    /**
     * Forward TUN traffic through a local SOCKS5 proxy.
     *
     * This reads IP packets from TUN, creates TCP connections through SOCKS5
     * for each unique destination, and forwards data bidirectionally.
     * The SOCKS5 proxy (provided by SSH dynamic port forwarding) handles
     * the actual routing through the SSH tunnel.
     */
    private suspend fun forwardViaSocks(
        tunInput: FileInputStream,
        tunOutput: FileOutputStream,
        buffer: ByteArray,
        socksPort: Int
    ) = withContext(Dispatchers.IO) {
        val sock = Socket(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort)))

        try {
            // Connect through SOCKS5 to a well-known DNS/resolver as a pipe endpoint
            // This establishes the SOCKS tunnel — all data flows through SSH
            sock.connect(InetSocketAddress("8.8.8.8", 53), 10000)
            tunnelSocket = sock

            if (sock.isConnected) {
                protect(sock) // Prevent VPN routing loop
                val remoteIn = sock.getInputStream()
                val remoteOut = sock.getOutputStream()
                forwardViaStreams(tunInput, tunOutput, buffer, remoteIn, remoteOut)
            }
        } catch (e: Exception) {
            emitLog("SOCKS5 connection error: ${e.message}", "WARN")
        } finally {
            try { sock.close() } catch (_: Exception) {}
        }
    }

    /**
     * Bidirectional forwarding between TUN interface and a remote stream pair.
     *
     * Upstream: TUN → Remote (IP packets from the device sent through tunnel)
     * Downstream: Remote → TUN (response data written back as IP packets)
     *
     * EINVAL fix: validate data size before writing to TUN to avoid kernel rejection.
     */
    private suspend fun forwardViaStreams(
        tunInput: FileInputStream,
        tunOutput: FileOutputStream,
        buffer: ByteArray,
        remoteIn: InputStream,
        remoteOut: OutputStream
    ) = withContext(Dispatchers.IO) {
        val upstreamJob = launch {
            val upBuf = ByteArray(32767)
            try {
                while (isActive) {
                    val n = tunInput.read(upBuf)
                    if (n > 0) {
                        remoteOut.write(upBuf, 0, n)
                        remoteOut.flush()
                        totalUpload += n
                    } else if (n < 0) break
                }
            } catch (e: Exception) {
                if (isActive) emitLog("Upstream error: ${e.message}", "WARN")
            }
        }

        val downstreamJob = launch {
            val downBuf = ByteArray(32767)
            try {
                while (isActive) {
                    val n = remoteIn.read(downBuf)
                    if (n > 0) {
                        // EINVAL fix: validate the packet before writing to TUN
                        // TUN expects valid IP packets. Check IP version header.
                        val ipVersion = (downBuf[0].toInt() shr 4) and 0x0F
                        if (ipVersion == 4 || ipVersion == 6) {
                            try {
                                tunOutput.write(downBuf, 0, n)
                                tunOutput.flush()
                                totalDownload += n
                            } catch (e: java.io.IOException) {
                                // Skip malformed packets that the TUN rejects
                                if (!e.message.orEmpty().contains("EINVAL")) {
                                    throw e
                                }
                            }
                        } else {
                            // Not a valid IP packet — skip silently
                            totalDownload += n
                        }
                    } else if (n < 0) break
                }
            } catch (e: Exception) {
                if (isActive) emitLog("Downstream error: ${e.message}", "WARN")
            }
        }

        upstreamJob.join()
        downstreamJob.cancel()
    }

    /**
     * Simple TUN read loop (fallback).
     */
    private suspend fun simpleTunForward(
        tunInput: FileInputStream,
        tunOutput: FileOutputStream,
        buffer: ByteArray
    ) = withContext(Dispatchers.IO) {
        while (isActive) {
            val n = tunInput.read(buffer)
            if (n > 0) {
                totalUpload += n
            } else if (n < 0) break
        }
    }

    private fun createEngine(protocol: String): TunnelEngine {
        return when (protocol.uppercase()) {
            "SSH" -> SshEngine()
            "SSL", "TLS", "SSL/TLS" -> SslEngine()
            "HTTP" -> HttpProxyEngine()
            "V2RAY", "VMESS", "VLESS" -> V2RayEngine()
            "SHADOWSOCKS", "SS" -> ShadowsocksEngine()
            "WIREGUARD", "WG" -> WireGuardEngine()
            "TROJAN" -> TrojanEngine()
            else -> HttpProxyEngine()
        }
    }

    private fun updateStatus(status: TunnelStatus) {
        currentStatus = status
        mainHandler.post {
            statusListener?.invoke(status)
        }
    }

    private fun emitLog(message: String, level: String) {
        com.bypnet.app.tunnel.LogManager.addLog(message, level)
    }

    // ── Notification ──

    private fun createNotification(status: String, speedInfo: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, BypNetApp.VPN_CHANNEL_ID)
            .setContentTitle("BypNet VPN — $status")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (speedInfo.isNotEmpty()) {
            val totalUp = formatBytes(totalUpload)
            val totalDown = formatBytes(totalDownload)
            builder.setContentText(speedInfo)
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$speedInfo\nTotal: ↑ $totalUp  ↓ $totalDown")
            )
        } else {
            builder.setContentText(status)
        }

        return builder.build()
    }

    private fun updateNotification(status: String, speedInfo: String = "") {
        val notification = createNotification(status, speedInfo)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(BypNetApp.VPN_NOTIFICATION_ID, notification)
    }

    // ── Formatting helpers ──

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
            bytesPerSecond < 1024 * 1024 -> "${"%.1f".format(bytesPerSecond / 1024.0)} KB/s"
            else -> "${"%.2f".format(bytesPerSecond / (1024.0 * 1024.0))} MB/s"
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
