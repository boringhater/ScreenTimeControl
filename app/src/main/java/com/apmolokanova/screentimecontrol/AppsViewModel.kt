package com.apmolokanova.screentimecontrol

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.TimePicker
import androidx.lifecycle.*

class AppsViewModel: ViewModel(), LifecycleEventObserver {
    //private lateinit var currentPackages: List<ApplicationInfo>
    val appsList: LiveData<MutableList<App>> = AppsRepository.appsList
    val timeoutsMap: LiveData<MutableMap<String, Timeout>> = AppsRepository.timeoutsMap
    val serviceRunning: LiveData<Boolean> = AppsRepository.serviceRunning
    val globalLimitActive: LiveData<Boolean> = AppsRepository.globalLimitActive
    val globalLimitTime: LiveData<Int> = AppsRepository.globalLimitTime

    fun setCurrentPackages(context: Context) {
        AppsRepository.initRepo(context)
    }

    fun createTimeout(appId: String,
                      isOn: Boolean,
                      period: Int = AppsRepository.TIMEOUT_PERIOD_DEFAULT,
                      outDuration: Int = AppsRepository.TIMEOUT_OUT_DURATION_DEFAULT): Timeout {
        if(timeoutsMap.value!![appId] != null) throw Exception("Timeout with appId \"$appId\" already exists.")
        val timeout =  Timeout(appId,isOn,period,outDuration)
        timeoutsMap.value!![appId] = timeout
        return timeout
    }

    fun serviceToggle() {
        AppsRepository.setServiceRunning(!serviceRunning.value!!)
    }

    fun globalLimitToggle() {
        AppsRepository.setGlobalLimitActive(!globalLimitActive.value!!)
    }

    fun onGlobalLimitChanged(timePicker: TimePicker) {
        AppsRepository.setGlobalTimeLimit(timePicker.hour*60 + timePicker.minute)
    }

    suspend fun getAppIcon(appId: String, context: Context): Drawable? {
        return AppsRepository.provideAppIcon(appId, context)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when(event) {
            Lifecycle.Event.ON_STOP -> {AppsRepository.preserveState(source as MainActivity)}
            else -> {}
        }
    }
}