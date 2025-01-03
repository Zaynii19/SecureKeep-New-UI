package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.chargingdetect

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.R
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.alarmsetup.AlarmService
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.alarmsetup.EnterPinActivity

class ChargingDetectionService : Service() {

    private var isAlarmTriggered = false
    private lateinit var powerManager: PowerManager
    private lateinit var activityManager: ActivityManager
    private var isVibrate = false
    private var isFlash = false
    private var isAlarmActive = false
    private val chargingReceiver = object : BroadcastReceiver() {
        private var wasPlugged = false
        override fun onReceive(context: Context, intent: Intent) {
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            if (isChargerConnected()) {
                if (!wasPlugged) {
                    wasPlugged = true
                    // Charger connected
                }
            } else if (plugged == 0 && wasPlugged) {
                wasPlugged = false
                // Charger disconnected
                triggerAlarm()
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        startForegroundService()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerReceiver(chargingReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        intent?.let {
            isVibrate = it.getBooleanExtra("Vibrate", false)
            isFlash = it.getBooleanExtra("Flash", false)
            isAlarmActive = it.getBooleanExtra("Alarm", false)
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "charging_detection_channel"
            val channelName = "Charging Detection Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notificationBuilder = NotificationCompat.Builder(this, "charging_detection_channel")
            .setContentTitle("Charging Detection Active")
            .setContentText("Monitoring Charging Connectivity...")
            .setSmallIcon(R.drawable.info) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
        startForeground(1, notificationBuilder.build())
    }

    private fun triggerAlarm() {
        if (!isAlarmTriggered) {
            isAlarmTriggered = true

            val isScreenOn = powerManager.isInteractive
            val appInForeground = isAppInForeground()

            if (isScreenOn && appInForeground) {
                // Start activity if the screen is on and app is in the foreground
                startActivity(Intent(this, EnterPinActivity::class.java).apply {
                    putExtra("Vibrate", isVibrate)
                    putExtra("Flash", isFlash)
                    putExtra("Alarm", isAlarmActive)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                // Start alarm service if the screen is off or app is closed
                startAlarmService()
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chargingReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun isChargerConnected(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
    }

    private fun isAppInForeground(): Boolean {
        val appProcesses = activityManager.runningAppProcesses
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return appProcess.processName == packageName
            }
        }
        return false
    }

    private fun startAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            putExtra("Vibrate", isVibrate)
            putExtra("Flash", isFlash)
            putExtra("Alarm", isAlarmActive)
        }
        startService(intent)
    }
}
