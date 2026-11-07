package com.example.securekeepNew.intruderDetection.intruderServices

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.securekeepNew.R
import com.example.securekeepNew.intruderDetection.IntruderSelfieActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MagicServiceClass : LifecycleService() {
    private var isEmail = false
    private lateinit var sharedPreferences: SharedPreferences
    private var userEmail = ""

    override fun onCreate() {
        super.onCreate()
        Log.d("MagicService", "onCreate: Camera Service Started")

        startForegroundService()

        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)
        isEmail = sharedPreferences.getBoolean("EmailStatus", false)
        userEmail = sharedPreferences.getString("UserEmail", "") ?: ""

        Log.d("MagicService", "onCreate: EmailStatus: $isEmail")
        Log.d("MagicService", "onCreate: UserEmail: $userEmail")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startCameraAndTakePhoto()
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val intent = Intent(this, IntruderSelfieActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "camera_channel"
        val channelName = "Intruder Selfie Service"
        val importance = NotificationManager.IMPORTANCE_LOW
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(notificationChannel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentIntent(pendingIntent)
            .setContentTitle("Intruder Detected")
            .setContentText("Someone has tried to unlock your phone, Click to view intruder photo")
            .setSmallIcon(R.drawable.info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)

        val notification = notificationBuilder.build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("MagicService", "Failed to start foreground service", e)
            stopSelf()
        }
    }

    private fun startCameraAndTakePhoto() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)

                takePhoto(imageCapture)
            } catch (e: Exception) {
                Log.e("MagicService", "Camera initialization failed: ${e.message}", e)
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(imageCapture: ImageCapture) {
        val fileDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (fileDir == null) {
            Log.e("MagicService", "Pictures directory not found.")
            stopSelf()
            return
        }

        if (!fileDir.exists() && !fileDir.mkdirs()) {
            Log.e("MagicService", "Failed to create directory: ${fileDir.absolutePath}")
            stopSelf()
            return
        }

        val fileName = "IMG-${System.currentTimeMillis()}.jpg"
        val photoFile = File(fileDir, fileName)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("MagicService", "Image saved successfully: ${photoFile.absolutePath}")
                    if (isEmail && userEmail.isNotEmpty()) {
                        sendEmailWithImageWithCoroutine(photoFile)
                    } else {
                        stopSelf()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("MagicService", "Photo capture failed: ${exception.message}", exception)
                    stopSelf()
                }
            }
        )
    }

    private fun sendEmailWithImageWithCoroutine(imageFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendEmailWithImage(imageFile)
            } finally {
                stopSelf()
            }
        }
    }

    // Using JavaMail
    private fun sendEmailWithImage(imageFile: File) {
        try {
            Log.d("MagicService", "Image file size: ${imageFile.length()} bytes")

            val props = Properties().apply {
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    val senderEmail = "zaynii1911491@gmail.com"
                    val senderPassword = "eeyepbpiadbraobu"
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            val currentTime = System.currentTimeMillis()
            val timestamp = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()).format(currentTime)

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress("zaynii1911491@gmail.com"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail))
                subject = "Intruder Alert"

                val multipart = MimeMultipart()

                val textBodyPart = MimeBodyPart().apply {
                    setText("An intruder was detected trying to access your device at $timestamp.")
                }

                val attachmentBodyPart = MimeBodyPart().apply {
                    attachFile(imageFile)
                }

                multipart.addBodyPart(textBodyPart)
                multipart.addBodyPart(attachmentBodyPart)

                setContent(multipart)
            }

            Transport.send(message)
            Log.d("MagicService", "Email sent successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MagicService", "Error sending email: ${e.message}")
        }
    }
}