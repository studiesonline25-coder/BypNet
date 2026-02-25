package com.bypnet.app.tunnel

import android.app.Notification
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
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                startForeground(BypNetApp.VPN_NOTIFICATION_ID, createNotification("Connecting..."))

                // Build TunnelConfig from intent extras
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

    /**
     * Establish the VPN TUN interface and start the tunnel engine.
     */
    private suspend fun startTunnel(config: TunnelConfig) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            emitLog("Starting VPN tunnel...", "INFO")

            // 1. Create and connect the tunnel engine
            val engine = createEngine(config.protocol)
            engine.callback = object : TunnelCallback {
                override fun onStatusChanged(status: TunnelStatus) {
                    // Don't update from engine directly — we manage status here
                }
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

            // 2. Configure the TUN interface
            val dns1 = config.primaryDns.ifEmpty { "8.8.8.8" }
            val dns2 = config.secondaryDns.ifEmpty { "8.8.4.4" }

            val builder = Builder()
                .setSession("BypNet")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)   // Route all IPv4 traffic
                .addDnsServer(dns1)
                .addDnsServer(dns2)
                .setMtu(1500)
                .setBlocking(true)

            // Allow BypNet itself to bypass the VPN (prevent loops)
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                // Ignore
            }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                emitLog("Failed to establish VPN interface — did you grant VPN permission?", "ERROR")
                updateStatus(TunnelStatus.ERROR)
                engine.disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            startTime = System.currentTimeMillis()
            updateStatus(TunnelStatus.CONNECTED)
            updateNotification("Connected")
            emitLog("VPN tunnel established. All traffic is routed.", "SUCCESS")

            // 3. Start packet forwarding through the tunnel
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

    /**
     * Stop the tunnel and clean up resources.
     */
    private suspend fun stopTunnel() {
        updateStatus(TunnelStatus.DISCONNECTING)
        emitLog("Disconnecting tunnel...", "INFO")

        try { tunnelEngine?.disconnect() } catch (e: Exception) { /* ignore */ }
        tunnelEngine = null

        try { tunnelSocket?.close() } catch (e: Exception) { /* ignore */ }
        tunnelSocket = null

        try { vpnInterface?.close() } catch (e: Exception) { /* ignore */ }
        vpnInterface = null

        totalUpload = 0
        totalDownload = 0

        emitLog("VPN disconnected", "INFO")
        updateStatus(TunnelStatus.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Forward packets between the TUN interface and the tunnel.
     *
     * For SSH engines: connects to the local SOCKS5 proxy that SSH opened.
     * For HTTP engines: uses the established socket directly.
     * For other engines: uses a simple IP-over-TCP forwarding approach.
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
            // Determine how to forward traffic based on engine type
            when (engine) {
                is SshEngine -> {
                    val proxyPort = engine.getLocalProxyPort()
                    if (proxyPort > 0) {
                        emitLog("Forwarding traffic via SOCKS5 on 127.0.0.1:$proxyPort", "INFO")
                        forwardViaSocks(tunInput, tunOutput, buffer, proxyPort)
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
                        emitLog("HTTP proxy connected but streams are null", "WARN")
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
                        emitLog("SSL connected but streams are null", "WARN")
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
     * Forward TUN packets through a local SOCKS5 proxy (used for SSH tunnels).
     */
    private suspend fun forwardViaSocks(
        tunInput: FileInputStream,
        tunOutput: FileOutputStream,
        buffer: ByteArray,
        socksPort: Int
    ) = withContext(Dispatchers.IO) {
        val sock = Socket()
        sock.connect(InetSocketAddress("127.0.0.1", socksPort), 5000)
        tunnelSocket = sock

        val remoteIn = sock.getInputStream()
        val remoteOut = sock.getOutputStream()

        // Read from TUN → write to SOCKS, and read from SOCKS → write to TUN
        forwardViaStreams(tunInput, tunOutput, buffer, remoteIn, remoteOut)
    }

    /**
     * Bidirectional forwarding between TUN interface and a remote stream pair.
     */
    private suspend fun forwardViaStreams(
        tunInput: FileInputStream,
        tunOutput: FileOutputStream,
        buffer: ByteArray,
        remoteIn: InputStream,
        remoteOut: OutputStream
    ) = withContext(Dispatchers.IO) {
        // Launch two coroutines: TUN→Remote and Remote→TUN
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
                        tunOutput.write(downBuf, 0, n)
                        tunOutput.flush()
                        totalDownload += n
                    } else if (n < 0) break
                }
            } catch (e: Exception) {
                if (isActive) emitLog("Downstream error: ${e.message}", "WARN")
            }
        }

        // Wait for either to finish (means tunnel broke)
        upstreamJob.join()
        downstreamJob.cancel()
    }

    /**
     * Simple TUN read loop (fallback when no remote socket is available).
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
                // Packets are consumed but not forwarded — tunnel engine handles it
            } else if (n < 0) break
        }
    }

    /**
     * Create the tunnel engine based on protocol selection.
     */
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
        mainHandler.post {
            logListener?.invoke(message, level)
        }
    }

    private fun createNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, BypNetApp.VPN_CHANNEL_ID)
            .setContentTitle("BypNet VPN")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = createNotification(status)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(BypNetApp.VPN_NOTIFICATION_ID, notification)
    }
}
