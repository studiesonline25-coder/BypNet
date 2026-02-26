package com.bypnet.app.config

import android.content.Context
import android.net.Uri
import com.bypnet.app.data.BypNetDatabase
import com.bypnet.app.data.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Manages BypNet configuration lifecycle:
 * - CRUD operations on local database
 * - Import/export .byp files
 * - Config encryption for locked configs
 */
class ConfigManager(private val context: Context) {

    private val database = BypNetDatabase.getInstance(context)
    private val configDao = database.configDao()

    /**
     * Get all saved configs as a reactive Flow.
     */
    fun getAllConfigs(): Flow<List<ConfigEntity>> {
        return configDao.getAllConfigs()
    }

    /**
     * Save a new config or update an existing one.
     */
    suspend fun saveConfig(entity: ConfigEntity): Long {
        return configDao.insertConfig(entity.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Delete a config by ID.
     */
    suspend fun deleteConfig(id: Long) {
        configDao.deleteConfigById(id)
    }

    /**
     * Import a .byp config file from a URI.
     * Reads raw bytes and parses the binary .byp format.
     * If the file is locked, a password must be provided.
     */
    suspend fun importConfig(uri: Uri, password: String? = null): BypConfig? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val data = inputStream.readBytes()
            inputStream.close()

            if (!BypConfigSerializer.isValidByp(data)) return null

            val config = if (BypConfigSerializer.isLocked(data)) {
                if (password.isNullOrEmpty()) return null // Caller must prompt for password
                BypConfigSerializer.fromLockedByp(data, password) ?: return null
            } else {
                BypConfigSerializer.fromByp(data) ?: return null
            }

            // Save to database
            val entity = configToEntity(config)
            configDao.insertConfig(entity)

            config
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Export a config to .byp binary format (unlocked).
     */
    suspend fun exportConfig(configId: Long): ByteArray? {
        val entity = configDao.getConfigById(configId) ?: return null
        val config = entityToConfig(entity)
        return BypConfigSerializer.toByp(config)
    }

    /**
     * Export a config as a locked .byp binary file.
     */
    suspend fun exportLockedConfig(configId: Long, password: String): ByteArray? {
        val entity = configDao.getConfigById(configId) ?: return null
        val config = entityToConfig(entity)
        return BypConfigSerializer.toLockedByp(config, password)
    }

    /**
     * Convert a ConfigEntity to a BypConfig.
     */
    fun entityToConfig(entity: ConfigEntity): BypConfig {
        return BypConfig(
            name = entity.name,
            protocol = entity.protocol,
            server = ServerConfig(entity.serverHost, entity.serverPort),
            auth = AuthConfig(entity.username, entity.password),
            payload = entity.payload,
            sni = entity.sni,
            dns = DnsConfigData(entity.primaryDns, entity.secondaryDns),
            cookies = entity.cookies,
            proxy = ProxyConfig(entity.proxyHost, entity.proxyPort),
            locked = entity.isLocked,
            lockPassword = entity.lockPassword
        )
    }

    /**
     * Convert a BypConfig to a ConfigEntity.
     */
    fun configToEntity(config: BypConfig): ConfigEntity {
        return ConfigEntity(
            name = config.name.ifEmpty { "Imported Config" },
            protocol = config.protocol,
            serverHost = config.server.host,
            serverPort = config.server.port,
            username = config.auth.username,
            password = config.auth.password,
            sni = config.sni,
            payload = config.payload,
            proxyHost = config.proxy.host,
            proxyPort = config.proxy.port,
            primaryDns = config.dns.primary,
            secondaryDns = config.dns.secondary,
            cookies = config.cookies,
            isLocked = config.locked,
            lockPassword = config.lockPassword
        )
    }

    /**
     * Get the config count.
     */
    suspend fun getConfigCount(): Int {
        return configDao.getConfigCount()
    }
}
