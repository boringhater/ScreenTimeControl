package com.apmolokanova.screentimecontrol

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AppsRepository {
    //in minutes
    const val DAILY_LIMIT_DEFAULT: Int = 60
    const val TIMEOUT_PERIOD_DEFAULT: Int = 30
    const val TIMEOUT_OUT_DURATION_DEFAULT: Int = 10
    private const val PACKAGE_NAME = "com.apmolokanova.screentimecontrol"
    private const val PREFS_NAME = "repoPrefs"
    private const val KEY_GLOBAL_LIMIT_TIME = "globalLimitTime"
    private const val KEY_GLOBAL_LIMIT_ACTIVE = "globalLimitActive"

    private val _appsList: MutableLiveData<MutableList<App>> = MutableLiveData(mutableListOf())
    val appsList: LiveData<MutableList<App>> = _appsList
    private  var _timeoutsMap: MutableLiveData<MutableMap<String,Timeout>> = MutableLiveData(mutableMapOf())
    val timeoutsMap : LiveData<MutableMap<String, Timeout>> = _timeoutsMap
    private val _serviceRunning: MutableLiveData<Boolean> = MutableLiveData(false)
    val serviceRunning: LiveData<Boolean> = _serviceRunning
    val timeoutUntil: MutableMap<String,Long> = mutableMapOf()
    private val _globalLimitActive: MutableLiveData<Boolean> = MutableLiveData(false)
    val globalLimitActive: LiveData<Boolean> = _globalLimitActive
    private val _globalLimitTime: MutableLiveData<Int> = MutableLiveData(DAILY_LIMIT_DEFAULT)
    val globalLimitTime : LiveData<Int> = _globalLimitTime

    lateinit var database: Database

    private val appImgCache: MutableMap<String, Drawable?> = mutableMapOf()

    fun initRepo(context: Context) {
        val packageManager = context.packageManager
        val currentPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _globalLimitTime.value = sharedPref.getInt(KEY_GLOBAL_LIMIT_TIME, DAILY_LIMIT_DEFAULT)
        _globalLimitActive.value = sharedPref.getBoolean(KEY_GLOBAL_LIMIT_ACTIVE,false)
        Log.e("Prefs", "globalLimitTime = ${globalLimitTime.value!!}")
        Log.e("Prefs", "globalLimitActive = ${globalLimitActive.value!!}")

        database = Database.getDatabase(context)

        val initAppsList =  currentPackages.filter {
            it.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
                    it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0 &&
                    it.packageName != PACKAGE_NAME
        }.map{
            App(
                it.packageName,
                packageManager.getApplicationLabel(it).toString(),
                false,
                false,
                DAILY_LIMIT_DEFAULT)
        }.sortedBy { it.name }.toMutableList()
        _appsList.value = initAppsList
        CoroutineScope(Dispatchers.IO).launch {
            database.appDao().deleteNotIn(initAppsList.map{it.appId}) //remove data of the apps deleted from user's device

            val dbAppData = database.appDao().getAllApps() //replace blank data with saved in db
            for(dbApp in dbAppData) {
                initAppsList.removeIf { blankApp-> blankApp.appId == dbApp.appId }
                initAppsList.add(dbApp)
            }
            _timeoutsMap.postValue(fetchTimeouts())
            _appsList.postValue(initAppsList)
        }
    }

    fun getApp(packageName: String): App? = appsList.value?.find { it.appId == packageName }

    fun getTimeout(packageName: String) : Timeout? = timeoutsMap.value?.get(packageName)

    fun removeTimeoutTimestamp(packageName: String) {
        timeoutUntil.remove(packageName)
    }

    fun addTimeoutTimestamp(packageName: String, timestamp: Long) {
        timeoutUntil[packageName] = timestamp
    }

    private fun fetchTimeouts() : MutableMap<String, Timeout> {
        val retTimeouts = mutableMapOf<String, Timeout>()
        CoroutineScope(Dispatchers.IO).launch{
            val dbTimeouts = database.timeoutDao().getAllTimeouts()
            for(tm in dbTimeouts) {
                retTimeouts[tm.appId] = Timeout(tm.appId, tm.isOn, tm.period, tm.outDuration)
            }
        }
        return retTimeouts
    }

    fun setServiceRunning(b: Boolean) {
        _serviceRunning.value = b
    }

    fun setGlobalLimitActive(b: Boolean) {
        _globalLimitActive.value = b
    }

    fun setGlobalTimeLimit(time: Int) {
        _globalLimitTime.value = time
    }

    suspend fun provideAppIcon(appId: String, context: Context): Drawable? = withContext(Dispatchers.IO) {
        if (!appImgCache.containsKey(appId)) {
            appImgCache[appId] = context.packageManager.getApplicationIcon(appId)
        }
        return@withContext appImgCache[appId]
    }

    fun timeoutsToDatabaseForm(): List<Timeout> {
        return timeoutsMap.value!!.map { Timeout(it.key, it.value.isOn,it.value.period,it.value.outDuration)}
    }

    fun preserveState(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            database.appDao().saveApps(appsList.value!!)
            database.timeoutDao().saveTimeouts(timeoutsToDatabaseForm())
        }
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt(KEY_GLOBAL_LIMIT_TIME, globalLimitTime.value!!)
            putBoolean(KEY_GLOBAL_LIMIT_ACTIVE, globalLimitActive.value!!)
            apply()
        }
    }
}