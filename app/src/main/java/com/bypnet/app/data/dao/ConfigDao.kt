package com.bypnet.app.data.dao

import androidx.room.*
import com.bypnet.app.data.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {

    @Query("SELECT * FROM configs ORDER BY updatedAt DESC")
    fun getAllConfigs(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM configs WHERE id = :id")
    suspend fun getConfigById(id: Long): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity): Long

    @Update
    suspend fun updateConfig(config: ConfigEntity)

    @Delete
    suspend fun deleteConfig(config: ConfigEntity)

    @Query("DELETE FROM configs WHERE id = :id")
    suspend fun deleteConfigById(id: Long)

    @Query("SELECT COUNT(*) FROM configs")
    suspend fun getConfigCount(): Int
}
