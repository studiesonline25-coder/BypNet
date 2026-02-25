package com.bypnet.app.data.dao

import androidx.room.*
import com.bypnet.app.data.entity.CookieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookieDao {

    @Query("SELECT * FROM cookies ORDER BY extractedAt DESC")
    fun getAllCookies(): Flow<List<CookieEntity>>

    @Query("SELECT * FROM cookies WHERE domain = :domain")
    suspend fun getCookiesByDomain(domain: String): List<CookieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookie(cookie: CookieEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookies(cookies: List<CookieEntity>)

    @Query("DELETE FROM cookies WHERE domain = :domain")
    suspend fun deleteCookiesByDomain(domain: String)

    @Query("DELETE FROM cookies")
    suspend fun deleteAllCookies()

    @Query("SELECT * FROM cookies WHERE domain = :domain")
    fun getCookiesByDomainFlow(domain: String): Flow<List<CookieEntity>>
}
