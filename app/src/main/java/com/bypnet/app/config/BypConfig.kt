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
 * Supports optional AES-256 encryption for locked configs.
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
     * Serialize a BypConfig to a locked (encrypted) .byp file string.
     * The output is a JSON wrapper: {"locked": true, "data": "<base64-aes-encrypted>"}
     */
    fun toLockedJson(config: BypConfig, password: String): String {
        val plainJson = toJson(config.copy(locked = true, lockPassword = ""))
        val encrypted = encrypt(plainJson, password)
        val wrapper = mapOf("locked" to true, "data" to encrypted)
        return gson.toJson(wrapper)
    }

    /**
     * Deserialize a JSON string to BypConfig.
     * Handles both plain and locked (encrypted) formats.
     */
    fun fromJson(json: String): BypConfig {
        return gson.fromJson(json, BypConfig::class.java)
    }

    /**
     * Deserialize a locked .byp file with a password.
     * Returns null if the password is wrong or decryption fails.
     */
    fun fromLockedJson(json: String, password: String): BypConfig? {
        return try {
            val wrapper = gson.fromJson(json, Map::class.java)
            val isLocked = wrapper["locked"] as? Boolean ?: false
            if (!isLocked) return fromJson(json)
            val encryptedData = wrapper["data"] as? String ?: return null
            val decrypted = decrypt(encryptedData, password)
            fromJson(decrypted)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a .byp JSON string represents a locked config.
     */
    fun isLocked(json: String): Boolean {
        return try {
            val wrapper = gson.fromJson(json, Map::class.java)
            wrapper["locked"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
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

    // ── AES-256 Encryption ──

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION_COUNT = 65536
    private const val IV_LENGTH = 16
    private const val SALT_LENGTH = 16

    private fun encrypt(plainText: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { java.security.SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { java.security.SecureRandom().nextBytes(it) }

        val keySpec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = javax.crypto.SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val secretKey = javax.crypto.spec.SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")

        val cipher = javax.crypto.Cipher.getInstance(ALGORITHM)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, javax.crypto.spec.IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Combine: salt + iv + encrypted → base64
        val combined = salt + iv + encrypted
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    private fun decrypt(base64Data: String, password: String): String {
        val combined = android.util.Base64.decode(base64Data, android.util.Base64.NO_WRAP)
        val salt = combined.copyOfRange(0, SALT_LENGTH)
        val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val encrypted = combined.copyOfRange(SALT_LENGTH + IV_LENGTH, combined.size)

        val keySpec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = javax.crypto.SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val secretKey = javax.crypto.spec.SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")

        val cipher = javax.crypto.Cipher.getInstance(ALGORITHM)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.IvParameterSpec(iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
