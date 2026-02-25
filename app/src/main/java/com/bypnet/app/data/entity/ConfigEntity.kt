package com.bypnet.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configs")
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val protocol: String,
    val serverHost: String,
    val serverPort: Int,
    val username: String = "",
    val password: String = "",
    val sni: String = "",
    val payload: String = "",
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val primaryDns: String = "8.8.8.8",
    val secondaryDns: String = "8.8.4.4",
    val cookies: String = "",
    val extraConfig: String = "", // JSON for protocol-specific fields (V2Ray, WG, SS)
    val isLocked: Boolean = false,
    val lockPassword: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
