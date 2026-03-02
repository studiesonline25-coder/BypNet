package com.bypnet.app.tunnel.ssh

import com.bypnet.app.tunnel.TunnelConfig
import com.bypnet.app.tunnel.payload.PayloadProcessor
import com.jcraft.jsch.Proxy
import com.jcraft.jsch.SocketFactory
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class CustomSslProxy(
    private val config: TunnelConfig,
    private val logger: (String, String) -> Unit
) : Proxy {
    private var proxySocket: Socket? = null
    private var sslSocket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override fun connect(socketFactory: SocketFactory?, host: String, port: Int, timeout: Int) {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(createTrustAllManager()), java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            if (config.proxyHost.isNotEmpty() && config.proxyPort > 0) {
                // Connect via HTTP Proxy first, then upgrade
                logger("Connecting to HTTP proxy for SSL ${config.proxyHost}:${config.proxyPort}...", "INFO")
                
                val proxy = if (socketFactory == null) {
                    val s = Socket()
                    s.connect(InetSocketAddress(config.proxyHost, config.proxyPort), timeout)
                    s
                } else {
                    socketFactory.createSocket(config.proxyHost, config.proxyPort)
                }
                
                proxySocket = proxy
                val pOut = if (socketFactory == null) proxy.outputStream else socketFactory.getOutputStream(proxy)
                val pIn = if (socketFactory == null) proxy.inputStream else socketFactory.getInputStream(proxy)

                val payload = if (config.payload.isNotEmpty()) {
                    PayloadProcessor.process(config.payload, config.serverHost, config.serverPort, config.sni, config.cookies)
                } else {
                    PayloadProcessor.process(PayloadProcessor.defaultConnectPayload(), config.serverHost, config.serverPort, config.sni)
                }

                logger("Sending SSL proxy payload...", "INFO")
                pOut.write(payload.toByteArray())
                pOut.flush()

                // Read proxy response carefully byte-by-byte so we don't consume subsequent SSL/SSH data
                var line = readLineFromStream(pIn)
                logger("Proxy Response: $line", "SUCCESS")

                if (!line.contains("200") && !line.contains("101")) {
                    throw Exception("Proxy rejected request with status: $line")
                }

                // Consume all headers until \r\n\r\n
                while (true) {
                    val header = readLineFromStream(pIn)
                    if (header.isEmpty()) break
                }

                logger("Upgrading connection to SSL/TLS...", "INFO")
                val sniHost = if (config.sni.isNotEmpty()) config.sni else config.serverHost
                sslSocket = sslSocketFactory.createSocket(proxy, sniHost, config.serverPort, true) as SSLSocket
                sslSocket?.startHandshake()

                inputStream = sslSocket?.inputStream
                outputStream = sslSocket?.outputStream

            } else {
                // Direct SSL connection
                logger("Initiating direct SSL/TLS connection to ${config.serverHost}:${config.serverPort}...", "INFO")
                
                val baseSocket = if (socketFactory == null) {
                    val s = Socket()
                    s.connect(InetSocketAddress(config.serverHost, config.serverPort), timeout)
                    s
                } else {
                    socketFactory.createSocket(config.serverHost, config.serverPort)
                }

                proxySocket = baseSocket
                val sniHost = if (config.sni.isNotEmpty()) config.sni else config.serverHost
                
                sslSocket = sslSocketFactory.createSocket(baseSocket, sniHost, config.serverPort, true) as SSLSocket
                
                // If there's a payload to send BEFORE SSH starts over pure SSL (like SNI bug)
                if (config.payload.isNotEmpty()) {
                    val payload = PayloadProcessor.process(config.payload, config.serverHost, config.serverPort, config.sni, config.cookies)
                    sslSocket?.outputStream?.write(payload.toByteArray())
                    sslSocket?.outputStream?.flush()
                }

                sslSocket?.startHandshake()
                
                inputStream = sslSocket?.inputStream
                outputStream = sslSocket?.outputStream
            }
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    private fun createTrustAllManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    private fun readLineFromStream(inputStream: InputStream): String {
        val sb = StringBuilder()
        var c: Int
        while (true) {
            c = inputStream.read()
            if (c == -1) break
            if (c == '\n'.code) {
                if (sb.isNotEmpty() && sb.last() == '\r') {
                    sb.deleteCharAt(sb.length - 1)
                }
                break
            }
            sb.append(c.toChar())
        }
        return sb.toString()
    }

    override fun getInputStream(): InputStream? = inputStream
    override fun getOutputStream(): OutputStream? = outputStream
    override fun getSocket(): Socket? = sslSocket ?: proxySocket

    override fun close() {
        try { inputStream?.close() } catch (e: Exception) {}
        try { outputStream?.close() } catch (e: Exception) {}
        try { sslSocket?.close() } catch (e: Exception) {}
        try { proxySocket?.close() } catch (e: Exception) {}
    }
}
