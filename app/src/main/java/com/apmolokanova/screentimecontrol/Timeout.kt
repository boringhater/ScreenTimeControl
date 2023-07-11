package com.apmolokanova.screentimecontrol

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey


@Entity(tableName = "timeouts", foreignKeys = [ForeignKey(
    entity = App::class,
    parentColumns = arrayOf("app_id"),
    childColumns = arrayOf("app_id"),
    onDelete = CASCADE
)])
data class Timeout(   //period - minutes, outDuration - minutes
    @PrimaryKey @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "is_on") var isOn: Boolean,
    @ColumnInfo(name = "period") var period: Int,
    @ColumnInfo(name = "out_duration") var outDuration: Int) {
    fun getPeriodHourMinute(): Pair<Int,Int> = Pair<Int,Int>(period/60,period%60)
    fun getDurationHourMinute(): Pair<Int,Int> = Pair<Int,Int>(outDuration/60,outDuration%60)

    fun periodHour(): Int = getHour(period)
    fun periodMinute(): Int = getMinute(period)

    fun outDurationHour(): Int = getHour(outDuration)
    fun outDurationMinute(): Int = getMinute(outDuration)

    fun periodInMillis(): Long = getMillis(period)
    fun outDurationInMillis(): Long = getMillis(outDuration)

    companion object{
        @JvmStatic
        fun getHour(fullTime: Int): Int = fullTime / 60
        @JvmStatic
        fun getMinute(fullTime: Int): Int = fullTime % 60
        @JvmStatic
        fun getMillis(minutes: Int): Long = minutes.toLong()*60000
    }
}