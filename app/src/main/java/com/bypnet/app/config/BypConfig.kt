package com.bypnet.app.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

/**
 * BypNet configuration model.
 * Internally serialized via Gson, but stored in the proprietary
 * .byp binary format — NOT as plain JSON.
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
 * Serializer for the proprietary .byp binary file format.
 *
 * .byp file structure:
 * ┌──────────────────────────────────────────────────┐
 * │ Magic: "BYP!" (4 bytes)                         │
 * │ Format version: 0x01 (1 byte)                   │
 * │ Flags: 0x00=open, 0x01=locked (1 byte)          │
 * │ If locked:                                      │
 * │   Salt (16 bytes)                               │
 * │   IV   (16 bytes)                               │
 * │ Payload: GZIP-compressed data                   │
 * │   (AES-256-CBC encrypted before GZIP if locked) │
 * └──────────────────────────────────────────────────┘
 *
 * This is NOT JSON. Only BypNet can read/write .byp files.
 */
object BypConfigSerializer {

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    // .byp binary format constants
    private val MAGIC = byteArrayOf('B'.code.toByte(), 'Y'.code.toByte(), 'P'.code.toByte(), '!'.code.toByte())
    private const val FORMAT_VERSION: Byte = 0x01
    private const val FLAG_OPEN: Byte = 0x00
    private const val FLAG_LOCKED: Byte = 0x01

    // AES constants
    private const val AES_ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION_COUNT = 65536
    private const val IV_LENGTH = 16
    private const val SALT_LENGTH = 16

    /**
     * Serialize a BypConfig to .byp binary (unlocked).
     */
    fun toByp(config: BypConfig): ByteArray {
        val json = gson.toJson(config)
        val compressed = gzipCompress(json.toByteArray(Charsets.UTF_8))
        val out = java.io.ByteArrayOutputStream()
        out.write(MAGIC)
        out.write(FORMAT_VERSION.toInt())
        out.write(FLAG_OPEN.toInt())
        out.write(compressed)
        return out.toByteArray()
    }

    /**
     * Serialize a BypConfig to .byp binary (locked with password).
     */
    fun toLockedByp(config: BypConfig, password: String): ByteArray {
        val json = gson.toJson(config.copy(locked = true, lockPassword = ""))
        val plainBytes = json.toByteArray(Charsets.UTF_8)

        val salt = ByteArray(SALT_LENGTH).also { java.security.SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { java.security.SecureRandom().nextBytes(it) }
        val encrypted = aesEncrypt(plainBytes, password, salt, iv)
        val compressed = gzipCompress(encrypted)

        val out = java.io.ByteArrayOutputStream()
        out.write(MAGIC)
        out.write(FORMAT_VERSION.toInt())
        out.write(FLAG_LOCKED.toInt())
        out.write(salt)
        out.write(iv)
        out.write(compressed)
        return out.toByteArray()
    }

    /**
     * Deserialize a .byp binary file (unlocked).
     */
    fun fromByp(data: ByteArray): BypConfig? {
        return try {
            if (!isValidByp(data)) return null
            if (data[5] == FLAG_LOCKED) return null // Needs password
            val compressed = data.copyOfRange(6, data.size)
            val decompressed = gzipDecompress(compressed)
            gson.fromJson(String(decompressed, Charsets.UTF_8), BypConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deserialize a locked .byp binary file with password.
     */
    fun fromLockedByp(data: ByteArray, password: String): BypConfig? {
        return try {
            if (!isValidByp(data)) return null
            if (data[5] != FLAG_LOCKED) return fromByp(data)
            if (data.size < 6 + SALT_LENGTH + IV_LENGTH) return null

            val salt = data.copyOfRange(6, 6 + SALT_LENGTH)
            val iv = data.copyOfRange(6 + SALT_LENGTH, 6 + SALT_LENGTH + IV_LENGTH)
            val compressed = data.copyOfRange(6 + SALT_LENGTH + IV_LENGTH, data.size)
            val encrypted = gzipDecompress(compressed)
            val decrypted = aesDecrypt(encrypted, password, salt, iv)
            gson.fromJson(String(decrypted, Charsets.UTF_8), BypConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** Check if raw data is locked. */
    fun isLocked(data: ByteArray): Boolean {
        return isValidByp(data) && data[5] == FLAG_LOCKED
    }

    /** Check if raw data is a valid .byp file. */
    fun isValidByp(data: ByteArray): Boolean {
        return data.size >= 6 &&
            data[0] == MAGIC[0] && data[1] == MAGIC[1] &&
            data[2] == MAGIC[2] && data[3] == MAGIC[3] &&
            data[4] == FORMAT_VERSION
    }

    // ── GZIP ──

    private fun gzipCompress(data: ByteArray): ByteArray {
        val bos = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gzipDecompress(data: ByteArray): ByteArray {
        return java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(data)).use { it.readBytes() }
    }

    // ── AES-256-CBC ──

    private fun aesEncrypt(plain: ByteArray, password: String, salt: ByteArray, iv: ByteArray): ByteArray {
        val key = deriveKey(password, salt)
        val cipher = javax.crypto.Cipher.getInstance(AES_ALGORITHM)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, javax.crypto.spec.IvParameterSpec(iv))
        return cipher.doFinal(plain)
    }

    private fun aesDecrypt(encrypted: ByteArray, password: String, salt: ByteArray, iv: ByteArray): ByteArray {
        val key = deriveKey(password, salt)
        val cipher = javax.crypto.Cipher.getInstance(AES_ALGORITHM)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, javax.crypto.spec.IvParameterSpec(iv))
        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: String, salt: ByteArray): javax.crypto.spec.SecretKeySpec {
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = javax.crypto.SecretKeyFactory.getInstance(KEY_ALGORITHM)
        return javax.crypto.spec.SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
