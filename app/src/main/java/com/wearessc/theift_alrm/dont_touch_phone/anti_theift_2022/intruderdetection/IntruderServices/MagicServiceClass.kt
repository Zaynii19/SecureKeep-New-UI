package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderServices

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx. exifinterface. media. ExifInterface
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.R
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.CameraConfig
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.CameraError
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.HiddenCameraService
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.HiddenCameraUtils
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.config.CameraFacing
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.config.CameraFocus
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.config.CameraImageFormat
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.config.CameraResolution
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.config.CameraRotation
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderSelfieActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.Properties
import java.util.Random
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MagicServiceClass : HiddenCameraService() {
    private var isEmail = false
    private lateinit var sharedPreferences: SharedPreferences
    private var userEmail = ""
    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        Log.d("MagicService", "onCreate: Camera Service Started")

        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)
        isEmail = sharedPreferences.getBoolean("EmailStatus", false)
        userEmail = sharedPreferences.getString("UserEmail", "") ?: ""

        Log.d("MagicService", "onCreate: EmailStatus: $isEmail")
        Log.d("MagicService", "onCreate: UserEmail: $userEmail")

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            if (HiddenCameraUtils.canOverDrawOtherApps(this)) {
                val cameraConfig = CameraConfig()
                    .getBuilder(this)
                    .setCameraFacing(CameraFacing.FRONT_FACING_CAMERA)
                    .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                    .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                    .setCameraFocus(CameraFocus.AUTO)
                    .setImageRotation(CameraRotation.ROTATION_270) // Set to portrait
                    .build()

                startCamera(cameraConfig)

                Handler(Looper.getMainLooper()).postDelayed({
                    takePicture()
                },100)

            } else {
                HiddenCameraUtils.openDrawOverPermissionSetting(this)
            }
        } else {
            Toast.makeText(this, "Camera permission not available", Toast.LENGTH_SHORT).show()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        // Intent to launch EnterPinActivity when the notification is clicked
        val intent = Intent(this, IntruderSelfieActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "camera_channel"
            val channelName = "Intruder Selfie Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, "camera_channel")
            .setContentIntent(pendingIntent) // Perform action when clicked
            .setContentTitle("Intruder Detected")
            .setContentText("Someone has tried to unlock your phone, Click to view intruder photo")
            .setSmallIcon(R.drawable.info) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true) // Automatically remove notification when clicked

        val notification = notificationBuilder.build()

        startForeground(1, notification)
    }

    // to save in hidden folders of local storage
    override fun onImageCapture(imageFile: File) {
        Log.d("MagicService", "onImageCapture: Taking Picture")

        val path = imageFile.path
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        var decodeFile = BitmapFactory.decodeFile(path, BitmapFactory.Options())

        try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)?.toInt() ?: 1
            val matrix = Matrix()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            }

            decodeFile = Bitmap.createBitmap(decodeFile, 0, 0, options.outWidth, options.outHeight, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fileDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return

        if (!fileDir.exists() && !fileDir.mkdirs()) {
            Log.i("MagicService", "Can't create directory to save the image")
            return
        }

        val fileName = "Image-" + Random().nextInt(10000) + ".jpg"
        val imageFileToSave = File(fileDir, fileName)

        Log.i("path", imageFileToSave.absolutePath)

        try {
            FileOutputStream(imageFileToSave).use { fos ->
                decodeFile.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
            }
            Log.d("MagicService", "onImageCapture: Image saved successfully: ${imageFileToSave.absolutePath}")

            if (isEmail){
                // Send the image via email
                sendEmailWithImageWithCoroutine(imageFile)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("MagicService", "onImageCapture: Error saving image")
        }

        stopSelf()
    }

    private fun sendEmailWithImageWithCoroutine(imageFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            sendEmailWithImage(imageFile)
        }
    }

    private fun sendEmailWithImage(imageFile: File) {
        try {
            // Log the size of the image file
            Log.d("MagicService", "Image file size: ${imageFile.length()} bytes")

            val props = Properties().apply {
                put("mail.smtp.host", "smtp.gmail.com") // Use appropriate SMTP host
                put("mail.smtp.port", "587")
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    val senderEmail = "zaynii1911491@gmail.com" // Sender email
                    val senderPassword = "eeyepbpiadbraobu" // Use your app password for Gmail
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            // Get the current timestamp
            val currentTime = System.currentTimeMillis()
            val timestamp = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()).format(currentTime)

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress("zaynii1911491@gmail.com")) // Email from
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail)) // Email to
                subject = "Intruder Alert"

                // Create a multipart message
                val multipart = MimeMultipart()

                // Create the body part for the text
                val textBodyPart = MimeBodyPart().apply {
                    setText("An intruder was detected trying to access your device at $timestamp.")
                }

                // Create the body part for the attachment
                val attachmentBodyPart = MimeBodyPart().apply {
                    attachFile(imageFile)
                }

                // Add both parts to the multipart
                multipart.addBodyPart(textBodyPart)
                multipart.addBodyPart(attachmentBodyPart)

                // Set the content of the message to the multipart
                setContent(multipart)
            }

            Transport.send(message)
            Log.d("MagicService", "Email sent successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MagicService", "Error sending email: ${e.message}")
        }
    }

    override fun onCameraError(@CameraError.CameraErrorCodes errorCode: Int) {
        when (errorCode) {
            CameraError.ERROR_CAMERA_OPEN_FAILED -> {
                Log.d("MagicService", "onCameraError: ERROR_CAMERA_OPEN_FAILED ${R.string.error_cannot_open}")
            }
            CameraError.ERROR_IMAGE_WRITE_FAILED -> {
                Log.d("MagicService", "onCameraError: ERROR_IMAGE_WRITE_FAILED ${R.string.error_cannot_write}")
            }
            CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE -> {
                Log.d("MagicService", "onCameraError: ERROR_CAMERA_PERMISSION_NOT_AVAILABLE ${R.string.error_cannot_get_permission}")
            }
            CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION -> {
                HiddenCameraUtils.openDrawOverPermissionSetting(this)
            }
            CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA -> {
                Log.d("MagicService", "onCameraError: ERROR_DOES_NOT_HAVE_FRONT_CAMERA ${R.string.error_not_having_camera}")
            }
        }
        stopSelf()
    }
}

