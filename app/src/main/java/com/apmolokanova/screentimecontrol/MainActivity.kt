package com.apmolokanova.screentimecontrol

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider


class MainActivity : AppCompatActivity() {
    private lateinit var appsViewModel: AppsViewModel
    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appsViewModel = ViewModelProvider(this)[AppsViewModel::class.java]
        serviceIntent =  Intent(this, ScreenTimeControlService::class.java)

        if (savedInstanceState == null) {
            appsViewModel.setCurrentPackages(this)
            appsViewModel.serviceRunning.observe(this, Observer {
                when(it){
                    true -> onScreenTimeControlServiceActive()
                    false -> onScreenTimeControlServiceStop()
                }
            }
            )
            lifecycle.addObserver(appsViewModel)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentFrame, MainFragment.newInstance())
                .commit()
        }

        appsViewModel.serviceRunning.observe(this, Observer { isRunning ->
            if(isRunning) {
                applicationContext.startService(serviceIntent)
                Log.e("MainActivity", "Service started")
            } else {
                applicationContext.stopService(serviceIntent)
                Log.e("MainActivity", "Service stopped")
            }
        })
    }

    fun onScreenTimeControlServiceActive() {
        applicationContext.startService(serviceIntent)
    }

    fun onScreenTimeControlServiceStop() {
        applicationContext.stopService(serviceIntent)
    }
}