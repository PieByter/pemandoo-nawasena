package com.nawasena.pemandoo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Landmark::class],
    version = 1,
    exportSchema = false
)
abstract class PemandooDatabase : RoomDatabase() {

    abstract fun landmarkDao(): LandmarkDao

    companion object {
        private const val DATABASE_NAME = "pemandoo.db"

        @Volatile
        private var instance: PemandooDatabase? = null

        fun get(context: Context): PemandooDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PemandooDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
