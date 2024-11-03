package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.overchargedetection

import android.content.Intent
import android.content.SharedPreferences
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
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.databinding.ActivityOverChargeBinding
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.settings.SettingActivity

class OverChargeActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityOverChargeBinding.inflate(layoutInflater)
    }
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
    private var isBatteryServiceRunning = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieving selected attempts, alert status
        alarmPreferences = getSharedPreferences("PinAndService", MODE_PRIVATE)
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusOverCharge", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusOverCharge", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusOverCharge", false)

        isBatteryServiceRunning = MainActivity.isServiceRunning(this@OverChargeActivity, BatteryDetectionService::class.java)

        updateUI()

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.powerBtn.setOnClickListener {
            if (!isAlarmActive && !isBatteryServiceRunning) {
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
                        Toast.makeText(this@OverChargeActivity, "Battery Detection Mode Activated", Toast.LENGTH_SHORT).show()
                        updateUI()
                        startBatteryDetectionService()
                        // Storing alarm status value in shared preferences
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("AlarmStatusOverCharge", isAlarmActive)
                        editor.apply()
                    }
                }.start()
            } else {
                stopBatteryDetection()
            }
        }

        binding.switchBtnV.setOnClickListener {
            isVibrate = !isVibrate
            binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isVibrate) "Vibration Enabled" else "Vibration Disabled", Toast.LENGTH_SHORT).show()

            // Storing vibrate status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("VibrateStatusOverCharge", isVibrate)
            editor.apply()
        }

        binding.switchBtnF.setOnClickListener {
            isFlash = !isFlash
            binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isFlash) "Flash Turned on" else "Flash Turned off", Toast.LENGTH_SHORT).show()

            // Storing flash status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("FlashStatusOverCharge", isFlash)
            editor.apply()
        }
    }

    private fun updateUI() {
        if (isAlarmActive) {
            binding.powerBtn.setImageResource(R.drawable.power_off)
            binding.activateText.text = getString(R.string.tap_to_deactivate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.power_on)
            binding.activateText.text = getString(R.string.tap_to_activate)
        }

        binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
        binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
    }

    override fun onResume() {
        super.onResume()
        val isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus", false)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusOverCharge", false)
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

    private fun stopBatteryDetection() {
        Toast.makeText(this@OverChargeActivity, "Battery Detection Mode Deactivated", Toast.LENGTH_SHORT).show()
        binding.powerBtn.setImageResource(R.drawable.power_on)
        binding.activateText.text = getString(R.string.tap_to_activate)
        isAlarmActive = false

        isFlash = false
        binding.switchBtnF.setImageResource(R.drawable.switch_off)

        isVibrate = false
        binding.switchBtnV.setImageResource(R.drawable.switch_off)

        updateUI()

        // Storing alarm status value in shared preferences
        val editor = sharedPreferences.edit()
        editor.putBoolean("AlarmStatusOverCharge", isAlarmActive)
        editor.putBoolean("FlashStatusOverCharge", isFlash)
        editor.putBoolean("VibrateStatusTouch", isVibrate)
        editor.apply()

        stopBatteryDetectionService()
    }

    private fun startBatteryDetectionService() {
        isBatteryServiceRunning = true
        val serviceIntent = Intent(this, BatteryDetectionService::class.java)
        serviceIntent.putExtra("Alarm", isAlarmActive)
        serviceIntent.putExtra("Flash", isFlash)
        serviceIntent.putExtra("Vibrate", isVibrate)
        startForegroundService(serviceIntent)
    }

    private fun stopBatteryDetectionService() {
        isBatteryServiceRunning = false
        val serviceIntent = Intent(this, BatteryDetectionService::class.java)
        stopService(serviceIntent)
    }
}
