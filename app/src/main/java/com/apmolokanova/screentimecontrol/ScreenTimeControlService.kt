package com.apmolokanova.screentimecontrol

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class ScreenTimeControlService : Service() {
    var window: OverlayWindow? = null;
    private var applicationControlTimer: Timer = Timer(TIMER_NAME, true)
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var notificationManager: NotificationManager
    private val runningJobs: MutableList<Job> = mutableListOf()


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        window = OverlayWindow(this)
        usageStatsManager =
            applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        applicationControlTimer.scheduleAtFixedRate(0, APPLICATION_CONTROL_PERIOD) {
            onTimerCheck()
        }
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(
            SERVICE_CHANNEL_ID,
            createNotification(getString(R.string.screen_time_is_limited))
        );
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.e("Service", "OnDestroy")
        window?.close()
        window = null
        applicationControlTimer.cancel()
        applicationControlTimer.purge()
        super.onDestroy()
    }

    fun createNotification(contentText: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0, Intent(this, MainActivity::class.java), 0
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "Service"
            val descriptionText = getString(R.string.service_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel("Service", name, importance)
            mChannel.description = descriptionText
            mChannel.setSound(null, null)
            notificationManager.createNotificationChannel(mChannel)
        }

        val builder = NotificationCompat.Builder(this, "Service") //build foreground notification
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(getString(R.string.screentimecontrol))
            .setContentText(contentText)
            .setContentIntent(contentIntent) //main activity pending intent
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        return builder.build();
    }

    private fun updateNotification(contentText: String) {
        val notification: Notification = createNotification(contentText)
        notificationManager.notify(SERVICE_CHANNEL_ID, notification)
    }

    private fun onTimerCheck() {
        runningJobs.add(CoroutineScope(Dispatchers.Default).launch {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay(System.currentTimeMillis()),
                System.currentTimeMillis() + 5000
            )
            if (stats.size < 1) {
                window!!.close()
                return@launch
            }
            stats.removeIf { it.packageName.contains("android.launcher") }
            val currentStats = stats.maxBy { it.lastTimeUsed}
            Log.e("timestamp", "last package: ${currentStats.packageName}")
            val currentApp = AppsRepository.getApp(currentStats.packageName)
            if (currentApp == null) {
                appNotControlled()
                return@launch
            }
            if (currentApp.limitsActive) { //app is limited
                //Check global limit
                if (AppsRepository.globalLimitActive.value!!) {
                    val todayScreenTime = todayScreenTimeMillis()
                    Log.e("Daily", "global screen time today: ${todayScreenTime}")
                    if (todayScreenTime >= AppsRepository.globalLimitTime.value!! * 60000) {
                        blockApp(getString(R.string.global_limit_exceeded),getString(R.string.global_limit_exceeded))
                        return@launch
                    } else {
                        if (!currentApp.isDailyLimited) {
                            val leftGlobalTime = AppsRepository.globalLimitTime.value!! * 60000 - todayScreenTime
                            updateNotification(
                                getString(
                                    R.string.time_left_for_app,
                                    currentApp.name,
                                    timeValToString(leftGlobalTime / 3600000),
                                    timeValToString((leftGlobalTime % 3600000)/60000)
                                )
                            )
                            return@launch
                        }
                    }
                }
                //check daily limit of the app
                if (currentApp.isDailyLimited) { //daily limit is active
                    if (currentStats.totalTimeInForeground >= currentApp.dailyLimitInMillis()) { //daily limit exceeded
                        blockApp(
                            getString(R.string.limit_for_app_exceeded, currentApp.name),
                            getString(R.string.limit_for_app_exceeded, currentApp.name)
                        )
                        return@launch
                    } else { //within daily limit
                        val minTimeLim: Int = if (AppsRepository.globalLimitActive.value!!) {
                            minOf(AppsRepository.globalLimitTime.value!!, currentApp.dailyLimit)
                        } else {
                            currentApp.dailyLimit
                        }

                        val timeLeft = minTimeLim - currentStats.totalTimeInForeground / 60000
                        unblockApp(getString(
                            R.string.time_left_for_app,
                            currentApp.name,
                            timeValToString(timeLeft / 60),
                            timeValToString(timeLeft % 60))
                        )
                    }
                }
                val currentTimeout = AppsRepository.getTimeout(currentStats.packageName)
                currentTimeout?.let {
                    if(currentTimeout.isOn) {
                        if ((currentStats.totalTimeInForeground / 60000).toInt() % currentTimeout.period == 0) {
                            val timeoutUntil = AppsRepository.timeoutUntil[currentStats.packageName]
                            if (timeoutUntil != null) {
                                if (timeoutUntil > System.currentTimeMillis()) {
                                    val timeoutLeft = timeoutUntil - System.currentTimeMillis()
                                    blockApp(
                                        getString(
                                            R.string.app_timeout_until,
                                            currentApp.name,
                                            timeValToString(timeoutLeft/3600000),
                                            timeValToString((timeoutLeft%3600000)/60000)
                                        ),
                                        getString(R.string.app_under_timeout)
                                    )
                                    return@launch
                                } else {
                                    AppsRepository.removeTimeoutTimestamp(currentStats.packageName)
                                    unblockApp(getString(R.string.screen_time_is_limited))
                                }
                            } else {
                                AppsRepository.addTimeoutTimestamp(
                                    currentStats.packageName,
                                    System.currentTimeMillis() + currentTimeout.outDurationInMillis()
                                )
                            }
                        }
                    }
                }
            } else {
                appNotControlled()
            }
        })
    }


    private fun startOfDay(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal[Calendar.HOUR_OF_DAY] = 0 //set hours to zero
        cal[Calendar.MINUTE] = 0 // set minutes to zero
        cal[Calendar.SECOND] = 0 //set seconds to zero
        return cal.timeInMillis
    }

    private fun appNotControlled() {
        updateNotification(getString(R.string.app_not_limited))
        window!!.close()
        runningJobs.forEach{it.cancel()}
    }

    private fun unblockApp(notificationText: String) {
        window!!.close()
        updateNotification(notificationText)
        runningJobs.forEach{it.cancel()}
    }

    private fun blockApp(windowText: String, notificationText: String) {
        CoroutineScope(Dispatchers.Main).launch {
            window!!.open(windowText)
            updateNotification(notificationText)
        }
    }


    private fun timeValToString(timeVal: Int): String = timeVal.toString().padStart(2, '0')
    private fun timeValToString(timeVal: Long): String = timeVal.toString().padStart(2, '0')

    private fun todayScreenTimeMillis(): Long {
        val curTime = System.currentTimeMillis()
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            curTime - (curTime % 86400000), //86400000 - число миллисекунд в часе
            curTime
        ).sumOf { stat -> stat.totalTimeInForeground }
    }

    companion object {
        private const val SERVICE_CHANNEL_ID: Int = 1
        private const val TIMER_NAME = "applicationControlTimer"
        private const val APPLICATION_CONTROL_PERIOD = 4000L
    }
}