package com.bypnet.app.tunnel

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.bypnet.app.BypNetApp
import com.bypnet.app.MainActivity
import com.bypnet.app.tunnel.ssh.SshEngine
import com.bypnet.app.tunnel.ssl.SslEngine
import com.bypnet.app.tunnel.http.HttpProxyEngine
import com.bypnet.app.tunnel.v2ray.V2RayEngine
import com.bypnet.app.tunnel.shadowsocks.ShadowsocksEngine
import com.bypnet.app.tunnel.wireguard.WireGuardEngine
import com.bypnet.app.tunnel.trojan.TrojanEngine
import com.bypnet.app.tunnel.tun2socks.Tun2Socks
import kotlinx.coroutines.*
import android.os.Handler
import android.os.Looper

/**
 * Core VPN Service for BypNet.
 *
 * Architecture:
 * 1. TUN device captures all device traffic as raw IP packets
 * 2. SSH engine connects to server, starts a local SOCKS5 proxy
 * 3. Tun2Socks reads IP packets from TUN, parses TCP/UDP headers,
 *    and routes each TCP connection through SOCKS5
 * 4. Response packets are reconstructed and written back to TUN
 */
class BypNetVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelEngine: TunnelEngine? = null
    private var tun2socks: Tun2Socks? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Speed tracking
    private var lastUpload: Long = 0
    private var lastDownload: Long = 0
    private var startTime: Long = 0
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

        @Volatile var currentStatus: TunnelStatus = TunnelStatus.DISCONNECTED; private set
        @Volatile var statusListener: ((TunnelStatus) -> Unit)? = null
        @Volatile var logListener: ((String, String) -> Unit)? = null

        // Live speed stats for UI
        @Volatile var uploadSpeed: Long = 0
        @Volatile var downloadSpeed: Long = 0
        @Volatile var totalUpload: Long = 0
        @Volatile var totalDownload: Long = 0
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

                serviceScope.launch { startTunnel(config) }
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch { stopTunnel() }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.launch { stopTunnel() }
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Tunnel Lifecycle ──

    private suspend fun startTunnel(config: TunnelConfig) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            emitLog("Starting VPN tunnel...", "INFO")

            // 1. Create and connect the tunnel engine
            val engine = createEngine(config.protocol)
            engine.callback = object : TunnelCallback {
                override fun onStatusChanged(status: TunnelStatus) {}
                override fun onLog(message: String, level: String) { emitLog(message, level) }
                override fun onSpeedUpdate(uploadBps: Long, downloadBps: Long) {}
                override fun onError(message: String, throwable: Throwable?) { emitLog("ERROR: $message", "ERROR") }
            }
            tunnelEngine = engine

            emitLog("Connecting ${config.protocol} to ${config.serverHost}:${config.serverPort}...", "INFO")
            engine.connect(config)

            if (!engine.isConnected()) {
                emitLog("Tunnel engine failed to connect", "ERROR")
                updateStatus(TunnelStatus.ERROR)
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                return
            }
            emitLog("Tunnel engine connected!", "SUCCESS")

            // 2. Get SOCKS port from SSH engine
            val socksPort = when (engine) {
                is SshEngine -> engine.getLocalSocksPort()
                else -> 0
            }

            if (socksPort <= 0) {
                emitLog("No SOCKS5 proxy available — engine type: ${config.protocol}", "ERROR")
                updateStatus(TunnelStatus.ERROR)
                engine.disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                return
            }
            emitLog("SOCKS5 proxy available on port $socksPort", "INFO")

            // 3. Configure TUN interface
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

            try { builder.addDisallowedApplication(packageName) } catch (_: Exception) {}

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                emitLog("Failed to establish VPN interface", "ERROR")
                updateStatus(TunnelStatus.ERROR)
                engine.disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                return
            }

            // 4. Start Tun2Socks
            startTime = System.currentTimeMillis()
            updateStatus(TunnelStatus.CONNECTED)
            updateNotification("Connected", "↑ 0 B/s  ↓ 0 B/s")
            emitLog("VPN tunnel established. Starting tun2socks...", "SUCCESS")

            startSpeedUpdater()

            val t2s = Tun2Socks(vpnInterface!!, socksPort, this, dns1)
            tun2socks = t2s

            // This blocks until tun2socks stops
            withContext(Dispatchers.IO) {
                t2s.start()
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

        speedUpdateJob?.cancel(); speedUpdateJob = null
        tun2socks?.stop(); tun2socks = null
        try { tunnelEngine?.disconnect() } catch (_: Exception) {}; tunnelEngine = null
        try { vpnInterface?.close() } catch (_: Exception) {}; vpnInterface = null

        uploadSpeed = 0; downloadSpeed = 0; totalUpload = 0; totalDownload = 0

        emitLog("VPN disconnected", "INFO")
        updateStatus(TunnelStatus.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Speed Notification ──

    private fun startSpeedUpdater() {
        speedUpdateJob = serviceScope.launch {
            while (isActive) {
                delay(2000)
                val t2s = tun2socks ?: continue
                val upNow = t2s.totalUpload
                val downNow = t2s.totalDownload
                val upDelta = upNow - lastUpload
                val downDelta = downNow - lastDownload
                lastUpload = upNow; lastDownload = downNow

                uploadSpeed = upDelta / 2
                downloadSpeed = downDelta / 2
                totalUpload = upNow
                totalDownload = downNow

                val elapsed = formatDuration(System.currentTimeMillis() - startTime)
                updateNotification(
                    "Connected · $elapsed",
                    "↑ ${formatSpeed(uploadSpeed)}  ↓ ${formatSpeed(downloadSpeed)}"
                )
            }
        }
    }

    // ── Engine Factory ──

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

    // ── Status & Logging ──

    private fun updateStatus(status: TunnelStatus) {
        currentStatus = status
        mainHandler.post { statusListener?.invoke(status) }
    }

    private fun emitLog(message: String, level: String) {
        LogManager.addLog(message, level)
    }

    // ── Notification ──

    private fun createNotification(status: String, speedInfo: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
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
            builder.setContentText(speedInfo)
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$speedInfo\nTotal: ↑ ${formatBytes(totalUpload)}  ↓ ${formatBytes(totalDownload)}")
            )
        } else {
            builder.setContentText(status)
        }
        return builder.build()
    }

    private fun updateNotification(status: String, speedInfo: String = "") {
        val notification = createNotification(status, speedInfo)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(BypNetApp.VPN_NOTIFICATION_ID, notification)
    }

    // ── Formatting ──

    private fun formatSpeed(bps: Long): String = when {
        bps < 1024 -> "$bps B/s"
        bps < 1024 * 1024 -> "${"%.1f".format(bps / 1024.0)} KB/s"
        else -> "${"%.2f".format(bps / (1024.0 * 1024.0))} MB/s"
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }

    private fun formatDuration(millis: Long): String {
        val s = millis / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
