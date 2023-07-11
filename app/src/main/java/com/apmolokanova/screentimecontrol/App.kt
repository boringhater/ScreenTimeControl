package com.apmolokanova.screentimecontrol
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "apps")
data class App(
    @PrimaryKey @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "limits_active") var limitsActive : Boolean,
    @ColumnInfo(name = "is_daily_limited") var isDailyLimited: Boolean,
    @ColumnInfo(name = "daily_limit") var dailyLimit: Int) {
    fun dailyLimitHours(): Int = dailyLimit/60;
    fun dailyLimitMinutes(): Int = dailyLimit%60;
    fun dailyLimitInMillis(): Int = dailyLimit*60*1000
}