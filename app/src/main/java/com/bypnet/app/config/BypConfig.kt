package com.bypnet.app.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

/**
 * BypNet configuration model for the .byp config format.
 * This represents a complete tunnel configuration that can be
 * exported/imported as a JSON file with .byp extension.
 */
data class BypConfig(
    @SerializedName("version")
    val version: Int = 1,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("protocol")
    val protocol: String = "SSH",

    @SerializedName("server")
    val server: ServerConfig = ServerConfig(),

    @SerializedName("auth")
    val auth: AuthConfig = AuthConfig(),

    @SerializedName("payload")
    val payload: String = "",

    @SerializedName("sni")
    val sni: String = "",

    @SerializedName("dns")
    val dns: DnsConfigData = DnsConfigData(),

    @SerializedName("cookies")
    val cookies: String = "",

    @SerializedName("proxy")
    val proxy: ProxyConfig = ProxyConfig(),

    @SerializedName("v2ray")
    val v2ray: V2RayConfig = V2RayConfig(),

    @SerializedName("wireguard")
    val wireguard: WireGuardConfig = WireGuardConfig(),

    @SerializedName("shadowsocks")
    val shadowsocks: ShadowsocksConfig = ShadowsocksConfig(),

    @SerializedName("locked")
    val locked: Boolean = false,

    @SerializedName("lockPassword")
    val lockPassword: String = ""
)

data class ServerConfig(
    @SerializedName("host") val host: String = "",
    @SerializedName("port") val port: Int = 443
)

data class AuthConfig(
    @SerializedName("username") val username: String = "",
    @SerializedName("password") val password: String = ""
)

data class DnsConfigData(
    @SerializedName("primary") val primary: String = "8.8.8.8",
    @SerializedName("secondary") val secondary: String = "8.8.4.4"
)

data class ProxyConfig(
    @SerializedName("host") val host: String = "",
    @SerializedName("port") val port: Int = 0
)

data class V2RayConfig(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("alterId") val alterId: Int = 0,
    @SerializedName("security") val security: String = "auto",
    @SerializedName("network") val network: String = "tcp",
    @SerializedName("headerType") val headerType: String = "none",
    @SerializedName("tls") val tls: Boolean = false,
    @SerializedName("wsPath") val wsPath: String = "/",
    @SerializedName("wsHost") val wsHost: String = ""
)

data class WireGuardConfig(
    @SerializedName("privateKey") val privateKey: String = "",
    @SerializedName("publicKey") val publicKey: String = "",
    @SerializedName("presharedKey") val presharedKey: String = "",
    @SerializedName("address") val address: String = "10.0.0.2/32",
    @SerializedName("allowedIps") val allowedIps: String = "0.0.0.0/0, ::/0",
    @SerializedName("mtu") val mtu: Int = 1420,
    @SerializedName("keepalive") val keepalive: Int = 25
)

data class ShadowsocksConfig(
    @SerializedName("method") val method: String = "aes-256-gcm",
    @SerializedName("plugin") val plugin: String = "",
    @SerializedName("pluginOpts") val pluginOpts: String = ""
)

/**
 * Utility object for serializing/deserializing .byp configs.
 */
object BypConfigSerializer {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    /**
     * Serialize a BypConfig to JSON string.
     */
    fun toJson(config: BypConfig): String {
        return gson.toJson(config)
    }

    /**
     * Deserialize a JSON string to BypConfig.
     */
    fun fromJson(json: String): BypConfig {
        return gson.fromJson(json, BypConfig::class.java)
    }

    /**
     * Validate a .byp config JSON string.
     */
    fun validate(json: String): Boolean {
        return try {
            val config = fromJson(json)
            config.version > 0 && config.server.host.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
