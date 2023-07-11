package com.apmolokanova.screentimecontrol

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView

private const val FRAGMENT_NAME = "MainFragment"

class MainFragment : Fragment() {
    private lateinit var appsViewModel: AppsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appsViewModel = ViewModelProvider(requireActivity())[AppsViewModel::class.java]

        val appsRecyclerView: RecyclerView = view.findViewById(R.id.appsRecyclerView)
        val adapter = AppsAdapter(requireContext())
        appsViewModel.appsList.observe(requireActivity()) { adapter.submitList(appsViewModel.appsList.value)}
        adapter.setOnItemClickListener(object : OnItemClickListener<App> {
            override fun onItemClick(item: App) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(com.google.android.material.R.anim.abc_tooltip_enter,
                        com.google.android.material.R.anim.abc_tooltip_exit)
                    .replace(R.id.fragmentFrame, AppDetailsFragment.newInstance(appsViewModel.appsList.value!!.indexOf(item)))
                    .addToBackStack(FRAGMENT_NAME)
                    .commit()
            }

        })
        appsRecyclerView.adapter = adapter

        view.findViewById<SearchView>(R.id.searchBar).setOnQueryTextListener(object:OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                adapter.submitList(filterByName(p0 ?: "",appsViewModel.appsList.value!!))
                return true
            }

        })
        val powerBtn = view.findViewById<ImageButton>(R.id.powerBtn)
        powerBtn.setOnClickListener{
            if(!usagePermissionGranted()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                return@setOnClickListener
            }
            if(Settings.canDrawOverlays(requireContext())) {
                appsViewModel.serviceToggle()
            } else {
                Toast.makeText(requireContext(),getString(R.string.app_requires_permission),Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        }
        appsViewModel.serviceRunning.observe(requireActivity()) { isRunning ->
            when (isRunning) {
                true -> powerBtn.drawable.setTint(resources.getColor(R.color.almond));
                false -> powerBtn.drawable.setTint(resources.getColor(R.color.tblack))
            }
        }
        val globalLimitBtn = view.findViewById<ImageButton>(R.id.globalLimitBtn)
        globalLimitBtn.setOnClickListener{
            appsViewModel.globalLimitToggle()
        }
        val globalLimitPicker = view.findViewById<TimePicker>(R.id.globalLimitPicker)
        globalLimitPicker.setIs24HourView(true)
        globalLimitPicker.hour = appsViewModel.globalLimitTime.value!!/60
        globalLimitPicker.minute = appsViewModel.globalLimitTime.value!!%60
        globalLimitPicker.setOnTimeChangedListener { timePicker, i, i2 ->
            appsViewModel.onGlobalLimitChanged(timePicker)
        }
        appsViewModel.globalLimitActive.observe(requireActivity()){ limitActive ->
            when(limitActive){
                true -> globalLimitBtn.drawable.setTint(resources.getColor(R.color.almond));
                false -> globalLimitBtn.drawable.setTint(resources.getColor(R.color.tblack))
            }
            globalLimitPicker.isEnabled = limitActive
        }
    }

    fun usagePermissionGranted() : Boolean{
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun filterByName(name: String, appList: List<App>): List<App> =
        appList.filter { it.name.lowercase().contains(name.lowercase()) }

    companion object {
        @JvmStatic
        fun newInstance() =
            MainFragment()
    }
}