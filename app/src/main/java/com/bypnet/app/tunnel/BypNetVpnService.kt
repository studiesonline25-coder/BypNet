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
import com.bypnet.app.R
import com.bypnet.app.tunnel.http.HttpProxyEngine
import com.bypnet.app.tunnel.shadowsocks.ShadowsocksEngine
import com.bypnet.app.tunnel.ssh.SshEngine
import com.bypnet.app.tunnel.ssl.SslEngine
import com.bypnet.app.tunnel.trojan.TrojanEngine
import com.bypnet.app.tunnel.v2ray.V2RayEngine
import com.bypnet.app.tunnel.wireguard.WireGuardEngine
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Core VPN Service for BypNet.
 *
 * Extends Android's VpnService to create a TUN interface,
 * route all device traffic through the selected tunnel protocol,
 * and manage the VPN lifecycle with a foreground notification.
 */
class BypNetVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelEngine: TunnelEngine? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Traffic stats
    private var totalUpload: Long = 0
    private var totalDownload: Long = 0
    private var startTime: Long = 0

    companion object {
        const val ACTION_CONNECT = "com.bypnet.app.CONNECT"
        const val ACTION_DISCONNECT = "com.bypnet.app.DISCONNECT"
        const val EXTRA_CONFIG = "tunnel_config"

        @Volatile
        var currentStatus: TunnelStatus = TunnelStatus.DISCONNECTED
            private set

        @Volatile
        var statusListener: ((TunnelStatus) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                startForeground(BypNetApp.VPN_NOTIFICATION_ID, createNotification("Connecting..."))
                // In a real implementation, deserialize TunnelConfig from intent extras
                serviceScope.launch {
                    startTunnel()
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
     * Establish the VPN TUN interface and start packet routing.
     */
    private suspend fun startTunnel() {
        try {
            updateStatus(TunnelStatus.CONNECTING)

            // Configure the TUN interface
            val builder = Builder()
                .setSession("BypNet")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)  // Route all IPv4 traffic
                .addRoute("::", 0)         // Route all IPv6 traffic
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
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
                updateStatus(TunnelStatus.ERROR)
                return
            }

            startTime = System.currentTimeMillis()
            updateStatus(TunnelStatus.CONNECTED)
            updateNotification("Connected")

            // Start packet forwarding
            vpnInterface?.let { fd ->
                forwardPackets(fd)
            }

        } catch (e: Exception) {
            updateStatus(TunnelStatus.ERROR)
        }
    }

    /**
     * Stop the tunnel and clean up resources.
     */
    private suspend fun stopTunnel() {
        updateStatus(TunnelStatus.DISCONNECTING)

        tunnelEngine?.disconnect()
        tunnelEngine = null

        vpnInterface?.close()
        vpnInterface = null

        totalUpload = 0
        totalDownload = 0

        updateStatus(TunnelStatus.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Forward packets between the TUN interface and the tunnel.
     */
    private suspend fun forwardPackets(fd: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val inputStream = FileInputStream(fd.fileDescriptor)
        val outputStream = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32767)

        try {
            while (isActive) {
                // Read packet from TUN (outgoing traffic from apps)
                val bytesRead = inputStream.read(buffer)
                if (bytesRead > 0) {
                    totalUpload += bytesRead
                    // In a full implementation, these packets would be:
                    // 1. Encrypted by the tunnel engine
                    // 2. Sent to the remote server
                    // 3. Responses would be written back to outputStream
                }
            }
        } catch (e: Exception) {
            if (isActive) {
                // Unexpected error
            }
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
            else -> HttpProxyEngine() // Default fallback
        }
    }

    private fun updateStatus(status: TunnelStatus) {
        currentStatus = status
        statusListener?.invoke(status)
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
