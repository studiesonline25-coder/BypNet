package com.bypnet.app.tunnel.ssl

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.TunnelEngine
import com.bypnet.app.tunnel.TunnelStatus
import com.bypnet.app.tunnel.payload.PayloadProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.*

/**
 * SSL/TLS Tunnel Engine.
 * Creates an SSL tunnel through an HTTP proxy with custom payload injection,
 * or a direct SSL connection with SNI support.
 */
class SslEngine : TunnelEngine() {

    private var sslSocket: SSLSocket? = null
    private var proxySocket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val CONNECTION_TIMEOUT = 15000
        private const val BUFFER_SIZE = 32768
    }

    override suspend fun connect(config: TunnelConfig) = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.CONNECTING)
            log("Initiating SSL/TLS connection to ${config.serverHost}:${config.serverPort}...")

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(createTrustAllManager()), java.security.SecureRandom())

            val sslSocketFactory = sslContext.socketFactory

            if (config.proxyHost.isNotEmpty() && config.proxyPort > 0) {
                // Connect through HTTP proxy first
                log("Connecting through proxy ${config.proxyHost}:${config.proxyPort}")

                val proxy = Socket()
                proxy.connect(
                    InetSocketAddress(config.proxyHost, config.proxyPort),
                    CONNECTION_TIMEOUT
                )
                proxySocket = proxy

                // Send payload to proxy
                val payload = if (config.payload.isNotEmpty()) {
                    PayloadProcessor.process(
                        template = config.payload,
                        host = config.serverHost,
                        port = config.serverPort,
                        sni = config.sni,
                        cookies = config.cookies
                    )
                } else {
                    PayloadProcessor.process(
                        template = PayloadProcessor.defaultConnectPayload(),
                        host = config.serverHost,
                        port = config.serverPort,
                        sni = config.sni
                    )
                }

                log("Sending payload to proxy...")
                proxy.getOutputStream().write(payload.toByteArray())
                proxy.getOutputStream().flush()

                // Read proxy response
                val response = ByteArray(BUFFER_SIZE)
                val bytesRead = proxy.getInputStream().read(response)
                val responseStr = String(response, 0, bytesRead)

                log("Proxy response: ${responseStr.trim().split("\n").first()}")

                if (!responseStr.contains("200")) {
                    reportError("Proxy rejected connection: $responseStr")
                    return@withContext
                }

                // Upgrade to SSL over the proxy socket
                log("Upgrading connection to SSL/TLS...")
                val ssl = sslSocketFactory.createSocket(
                    proxy,
                    config.sni.ifEmpty { config.serverHost },
                    config.serverPort,
                    true
                ) as SSLSocket

                configureSsl(ssl, config)
                ssl.startHandshake()
                sslSocket = ssl

            } else {
                // Direct SSL connection
                log("Direct SSL connection...")
                val ssl = sslSocketFactory.createSocket() as SSLSocket
                configureSsl(ssl, config)
                ssl.connect(
                    InetSocketAddress(config.serverHost, config.serverPort),
                    CONNECTION_TIMEOUT
                )
                ssl.startHandshake()
                sslSocket = ssl
            }

            sslSocket?.let {
                inputStream = it.inputStream
                outputStream = it.outputStream
                log("SSL/TLS handshake completed successfully!", "SUCCESS")
                log("Protocol: ${it.session.protocol}, Cipher: ${it.session.cipherSuite}")
                updateStatus(TunnelStatus.CONNECTED)
            }

        } catch (e: SSLException) {
            reportError("SSL error: ${e.message}", e)
        } catch (e: Exception) {
            reportError("Connection failed: ${e.message}", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            updateStatus(TunnelStatus.DISCONNECTING)
            log("Closing SSL/TLS connection...")

            inputStream?.close()
            outputStream?.close()
            sslSocket?.close()
            proxySocket?.close()

            inputStream = null
            outputStream = null
            sslSocket = null
            proxySocket = null

            log("SSL/TLS disconnected", "SUCCESS")
            updateStatus(TunnelStatus.DISCONNECTED)
        } catch (e: Exception) {
            reportError("Error closing SSL: ${e.message}", e)
        }
    }

    private fun configureSsl(socket: SSLSocket, config: TunnelConfig) {
        val sni = config.sni.ifEmpty { config.serverHost }

        // Set SNI
        val sslParams = SSLParameters()
        sslParams.serverNames = listOf(SNIHostName(sni))
        socket.sslParameters = sslParams

        // Enable all supported protocols
        socket.enabledProtocols = socket.supportedProtocols
    }

    private fun createTrustAllManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<java.security.cert.X509Certificate>?,
                authType: String?
            ) {}

            override fun checkServerTrusted(
                chain: Array<java.security.cert.X509Certificate>?,
                authType: String?
            ) {}

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
    }

    fun getInputStream(): InputStream? = inputStream
    fun getOutputStream(): OutputStream? = outputStream
}
