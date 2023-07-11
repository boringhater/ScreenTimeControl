package com.apmolokanova.screentimecontrol

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.apmolokanova.screentimecontrol.databinding.FragmentAppDetailsBinding
import kotlin.properties.Delegates

private const val ARG_APP_LIST_INDEX: String = "appListIndex"

class AppDetailsFragment : Fragment() {
    private var appListIndex by Delegates.notNull<Int>()
    private lateinit var appsViewModel: AppsViewModel
    private lateinit var detailedApp: App
    private var timeout: Timeout? = null
    private var _binding: FragmentAppDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            appListIndex = it.getInt(ARG_APP_LIST_INDEX)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAppDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appsViewModel = ViewModelProvider(requireActivity())[AppsViewModel::class.java]
        detailedApp = appsViewModel.appsList.value!![appListIndex]
        timeout = appsViewModel.timeoutsMap.value!![detailedApp.appId]

        binding.appIcon.setImageDrawable(requireActivity().packageManager.getApplicationIcon(detailedApp.appId))
        binding.appName.text = detailedApp.name

        binding.dailyLimitPicker.setIs24HourView(true)
        binding.dailyLimitPicker.hour = detailedApp.dailyLimitHours()
        binding.dailyLimitPicker.minute = detailedApp.dailyLimitMinutes()

        binding.timeoutPeriodPicker.setIs24HourView(true)
        binding.timeoutPeriodPicker.hour = timeout?.periodHour() ?: Timeout.getHour(AppsRepository.TIMEOUT_PERIOD_DEFAULT)
        binding.timeoutPeriodPicker.minute = timeout?.periodMinute() ?: Timeout.getMinute(AppsRepository.TIMEOUT_PERIOD_DEFAULT)

        binding.timeoutDurationPicker.setIs24HourView(true)
        binding.timeoutDurationPicker.hour = timeout?.outDurationHour() ?: Timeout.getHour(AppsRepository.TIMEOUT_OUT_DURATION_DEFAULT)
        binding.timeoutDurationPicker.minute = timeout?.outDurationMinute() ?: Timeout.getMinute(AppsRepository.TIMEOUT_OUT_DURATION_DEFAULT)

        binding.isOnAppSwitch.isChecked = detailedApp.limitsActive
        toggleAppSettingsEnabled()
        binding.isOnAppSwitch.setOnCheckedChangeListener { compoundButton, b ->
            detailedApp.limitsActive = b
            toggleAppSettingsEnabled()
        }

        binding.dailyLimitSwitch.isChecked = detailedApp.isDailyLimited
        binding.dailyLimitSwitch.setOnCheckedChangeListener { compoundButton, b ->
            detailedApp.isDailyLimited = b
            binding.dailyLimitPicker.isEnabled = detailedApp.limitsActive&&b
        }

        binding.timeoutSwitch.isChecked = appsViewModel.timeoutsMap.value?.get(detailedApp.appId)?.isOn ?: false
        binding.timeoutSwitch.setOnCheckedChangeListener { compoundButton, b ->
            if(timeout == null) {
                timeout = appsViewModel.createTimeout(detailedApp.appId,b)
            }
            timeout!!.isOn = b
            binding.timeoutPeriodPicker.isEnabled = detailedApp.limitsActive&&b
            binding.timeoutDurationPicker.isEnabled = detailedApp.limitsActive&&b
        }

        binding.dailyLimitPicker.setOnTimeChangedListener { timePicker, i, i2 ->
            detailedApp.dailyLimit = timePicker.hour*60 + timePicker.minute
        }

        binding.timeoutPeriodPicker.setOnTimeChangedListener { timePicker, i, i2 ->
            timeout!!.period = timePicker.hour*60 + timePicker.minute
        }

        binding.timeoutDurationPicker.setOnTimeChangedListener { timePicker, i, i2 ->
            timeout!!.outDuration = timePicker.hour*60 + timePicker.minute
        }
    }

    private fun toggleAppSettingsEnabled() {
        binding.dailyLimitSwitch.isEnabled = detailedApp.limitsActive
        binding.dailyLimitPicker.isEnabled = detailedApp.limitsActive && detailedApp.isDailyLimited

        binding.timeoutSwitch.isEnabled = detailedApp.limitsActive
        binding.timeoutPeriodPicker.isEnabled = detailedApp.limitsActive && timeout?.isOn ?: false
        binding.timeoutDurationPicker.isEnabled = detailedApp.limitsActive && timeout?.isOn ?: false
    }

    companion object {
        @JvmStatic
        fun newInstance(appListIndex: Int) =
            AppDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_APP_LIST_INDEX, appListIndex)
                }
            }
    }
}