package com.apmolokanova.screentimecontrol

import androidx.room.*

@Dao
interface AppDao {
    @Query("SELECT * FROM apps")
    fun getAllApps(): List<App>

    @Query("SELECT * FROM apps WHERE app_id = :appId")
    fun getAppById(appId: String): App?

    @Delete
    fun delete(app: App)

    @Query("DELETE FROM apps WHERE app_id NOT IN (:appIdList)")
    fun deleteNotIn(appIdList: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg apps: App)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(apps: List<App>)

    @Transaction
    fun saveApps(apps: List<App>){
        insertAll(apps)
    }
}