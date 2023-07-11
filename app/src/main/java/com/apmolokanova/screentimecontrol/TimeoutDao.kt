package com.apmolokanova.screentimecontrol

import androidx.room.*

@Dao
interface TimeoutDao {
    @Query("SELECT * FROM timeouts")
    fun getAllTimeouts(): List<Timeout>

    @Query("SELECT * FROM timeouts WHERE app_id = :appId")
    fun getTimeoutByAppId(appId: String): Timeout?

    @Delete
    fun delete(timeout: Timeout)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(timeouts: List<Timeout>)

    @Transaction
    fun saveTimeouts(timeouts: List<Timeout>) {
        insertAll(timeouts)
    }
}