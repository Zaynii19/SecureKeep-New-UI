package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.chargingdetect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.MainActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.R
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.alarmsetup.EnterPinActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.databinding.ActivityChargeDetectBinding
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.settings.SettingActivity

class ChargeDetectActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityChargeDetectBinding.inflate(layoutInflater)
    }
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
    private var isChargingServiceRunning = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmPreferences: SharedPreferences

    // BroadcastReceiver to detect charging state changes
    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Register receiver for charging state changes
        registerReceiver(chargingReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_EXPORTED)

        // Retrieving selected attempts, alert status
        alarmPreferences = getSharedPreferences("PinAndService", MODE_PRIVATE)
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusCharge", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusCharge", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusCharge", false)

        isChargingServiceRunning = MainActivity.isServiceRunning(this, ChargingDetectionService::class.java)

        updateUI()

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.powerBtn.setOnClickListener {
            handlePowerButtonClick()
        }

        binding.switchBtnV.setOnClickListener {
            toggleVibration()
        }

        binding.switchBtnF.setOnClickListener {
            toggleFlash()
        }
    }

    private fun handlePowerButtonClick() {
        if (isChargerConnected()) {
            if (!isAlarmActive && !isChargingServiceRunning) {
                isAlarmActive = true

                object : CountDownTimer(3000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        binding.powerBtn.visibility = View.INVISIBLE
                        binding.activateText.visibility = View.INVISIBLE
                        binding.activateCount.visibility = View.VISIBLE
                        binding.activateCount.text = buildString {
                            append("Will be activated in\n")
                            append("\t\t\t\t00:")
                            append(millisUntilFinished / 1000)
                        }
                    }

                    override fun onFinish() {
                        binding.activateCount.visibility = View.INVISIBLE
                        binding.powerBtn.visibility = View.VISIBLE
                        binding.activateText.visibility = View.VISIBLE
                        Toast.makeText(this@ChargeDetectActivity, "Charging Detection Mode Activated", Toast.LENGTH_SHORT).show()
                        updateUI()
                        startChargingDetectionService()
                        // Storing alarm status value in shared preferences
                        sharedPreferences.edit().putBoolean("AlarmStatusCharge", isAlarmActive).apply()
                    }
                }.start()
            } else {
                stopChargingDetection()
            }
        } else {
            Toast.makeText(this@ChargeDetectActivity, "Connect Charger First", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleVibration() {
        isVibrate = !isVibrate
        binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
        Toast.makeText(this, if (isVibrate) "Vibration Enabled" else "Vibration Disabled", Toast.LENGTH_SHORT).show()

        // Storing vibrate status value in shared preferences
        sharedPreferences.edit().putBoolean("VibrateStatusCharge", isVibrate).apply()
    }

    private fun toggleFlash() {
        isFlash = !isFlash
        binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
        Toast.makeText(this, if (isFlash) "Flash Turned on" else "Flash Turned off", Toast.LENGTH_SHORT).show()

        // Storing flash status value in shared preferences
        sharedPreferences.edit().putBoolean("FlashStatusCharge", isFlash).apply()
    }

    private fun updateUI() {
        if (isChargerConnected()) {
            binding.powerBtn.setImageResource(if (isAlarmActive) R.drawable.power_off else R.drawable.power_on)
            binding.activateText.text = getString(if (isAlarmActive) R.string.tap_to_deactivate else R.string.tap_to_activate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.charger)
            binding.activateText.text = getString(R.string.please_connect_charger)
        }

        binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
        binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
    }

    private fun stopChargingDetection() {
        Toast.makeText(this, "Charging Detection Mode Deactivated", Toast.LENGTH_SHORT).show()
        binding.powerBtn.setImageResource(R.drawable.power_on)
        binding.activateText.text = getString(R.string.tap_to_activate)
        isAlarmActive = false

        isFlash = false
        binding.switchBtnF.setImageResource(R.drawable.switch_off)

        isVibrate = false
        binding.switchBtnV.setImageResource(R.drawable.switch_off)

        updateUI()

        // Storing alarm status value in shared preferences
        sharedPreferences.edit().apply {
            putBoolean("AlarmStatusCharge", isAlarmActive)
            putBoolean("FlashStatusCharge", isFlash)
            putBoolean("VibrateStatusCharge", isVibrate)
            apply()
        }

        stopChargingDetectionService()
    }

    override fun onResume() {
        super.onResume()
        val isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus", false)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusCharge", false)
        isFlash = alarmPreferences.getBoolean("FlashStatus", false)
        isVibrate = alarmPreferences.getBoolean("VibrateStatus", false)

        if (isAlarmServiceActive) {
            // Create a new intent with the necessary extras
            val intent = Intent(this, EnterPinActivity::class.java).apply {
                putExtra("Flash", isFlash)
                putExtra("Vibrate", isVibrate)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish() // Finish this activity to prevent returning to it
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chargingReceiver)
    }

    private fun isChargerConnected(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
    }

    private fun startChargingDetectionService() {
        isChargingServiceRunning = true
        val serviceIntent = Intent(this, ChargingDetectionService::class.java)
        serviceIntent.putExtra("Alarm", isAlarmActive)
        serviceIntent.putExtra("Flash", isFlash)
        serviceIntent.putExtra("Vibrate", isVibrate)
        startForegroundService(serviceIntent)
    }

    private fun stopChargingDetectionService() {
        isChargingServiceRunning = false
        val serviceIntent = Intent(this, ChargingDetectionService::class.java)
        stopService(serviceIntent)
    }
}
