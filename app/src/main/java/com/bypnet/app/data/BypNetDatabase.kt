package com.bypnet.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bypnet.app.data.dao.ConfigDao
import com.bypnet.app.data.dao.CookieDao
import com.bypnet.app.data.entity.ConfigEntity
import com.bypnet.app.data.entity.CookieEntity

@Database(
    entities = [ConfigEntity::class, CookieEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BypNetDatabase : RoomDatabase() {

    abstract fun configDao(): ConfigDao
    abstract fun cookieDao(): CookieDao

    companion object {
        @Volatile
        private var INSTANCE: BypNetDatabase? = null

        fun getInstance(context: Context): BypNetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BypNetDatabase::class.java,
                    "bypnet_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
