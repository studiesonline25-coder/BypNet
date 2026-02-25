package com.bypnet.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cookies")
data class CookieEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val name: String,
    val value: String,
    val path: String = "/",
    val isSecure: Boolean = false,
    val isHttpOnly: Boolean = false,
    val expiresAt: Long = 0,
    val extractedAt: Long = System.currentTimeMillis()
)
