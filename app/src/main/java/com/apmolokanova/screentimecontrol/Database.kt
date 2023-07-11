package com.apmolokanova.screentimecontrol

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

@androidx.room.Database(entities = [App::class, Timeout::class], version = 1)
abstract class Database: RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun timeoutDao(): TimeoutDao

    companion object {
        @Volatile
        private var _instance: Database? = null
        fun getDatabase(context: Context): Database {
            if (_instance == null) {
                synchronized(this) {
                    _instance = Room.databaseBuilder(context,Database::class.java, "database").build()
                }
            }
            return _instance!!
        }
    }
}