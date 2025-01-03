/*
 * Copyright 2017 Keval Patel.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.config.CameraResolution
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.config.CameraRotation
import java.io.IOException
import java.util.Collections
import kotlin.concurrent.Volatile

/**
 * Created by Keval on 10-Nov-16.
 * This surface view works as the fake preview for the camera.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */
@SuppressLint("ViewConstructor")
internal class CameraPreview(context: Context, cameraCallbacks: CameraCallbacks) :
    SurfaceView(context), SurfaceHolder.Callback {
    private val mCameraCallbacks: CameraCallbacks = cameraCallbacks

    private var mHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null

    private var mCameraConfig: CameraConfig? = null

    @Volatile
    var isSafeToTakePictureInternal: Boolean = false
        private set

    init {
        //Set surface holder
        initSurfaceView()
    }

    /**
     * Initialize the surface view holder.
     */
    private fun initSurfaceView() {
        val mHolder = holder
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }


    override fun onLayout(b: Boolean, i: Int, i1: Int, i2: Int, i3: Int) {
        //Do nothing
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        //Do nothing
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
        if (mCamera == null) {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
            return
        } else if (surfaceHolder.surface == null) {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
            return
        }

        try {
            mCamera!!.stopPreview()
        } catch (e: Exception) {
            // Ignore: tried to stop a non-existent preview
        }

        try {
            val parameters = mCamera!!.parameters
            val pictureSizes = parameters.supportedPictureSizes

            // Sort and select the appropriate resolution
            Collections.sort(pictureSizes, PictureSizeComparator())

            val cameraSize = when (mCameraConfig!!.resolution) {
                CameraResolution.HIGH_RESOLUTION -> pictureSizes[0]
                CameraResolution.MEDIUM_RESOLUTION -> pictureSizes[pictureSizes.size / 2]
                CameraResolution.LOW_RESOLUTION -> pictureSizes[pictureSizes.size - 1]
                else -> throw RuntimeException("Invalid camera resolution.")
            }

            // Verify that the selected size is supported
            if (pictureSizes.contains(cameraSize)) {
                parameters.setPictureSize(cameraSize.width, cameraSize.height)
            } else {
                mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
                Log.e("CameraPreview", "Selected picture size is not supported.")
                return
            }

            // Set the focus mode, if supported
            val supportedFocusModes = parameters.supportedFocusModes
            if (supportedFocusModes.contains(mCameraConfig!!.focusMode)) {
                parameters.focusMode = mCameraConfig!!.focusMode
            }

            mCamera!!.parameters = parameters
            mCamera!!.setPreviewDisplay(surfaceHolder)
            mCamera!!.startPreview()
            isSafeToTakePictureInternal = true
        } catch (e: IOException) {
            Log.e("CameraPreview", "Error setting camera preview: ${e.message}")
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
        } catch (e: RuntimeException) {
            Log.e("CameraPreview", "Failed to set picture size: ${e.message}")
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
        }
    }



    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Call stopPreview() to stop updating the preview surface.
        if (mCamera != null) mCamera!!.stopPreview()
    }

    /**
     * Initialize the camera and start the preview of the camera.
     *
     * @param cameraConfig camera config builder.
     */
    fun startCameraInternal(cameraConfig: CameraConfig) {
        mCameraConfig = cameraConfig

        if (safeCameraOpen(mCameraConfig!!.facing)) {
            if (mCamera != null) {
                requestLayout()

                try {
                    mCamera!!.setPreviewDisplay(mHolder)
                    mCamera!!.startPreview()
                } catch (e: IOException) {
                    e.printStackTrace()
                    mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
                }
            }
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
        }
    }

    private fun safeCameraOpen(id: Int): Boolean {
        var qOpened = false

        try {
            stopPreviewAndFreeCamera()

            mCamera = Camera.open(id)
            qOpened = (mCamera != null)
        } catch (e: Exception) {
            Log.e("CameraPreview", "failed to open Camera")
            e.printStackTrace()
        }

        return qOpened
    }

    fun takePictureInternal() {
        isSafeToTakePictureInternal = false
        if (mCamera != null) {
            mCamera!!.takePicture(
                null, null
            ) { bytes, _ ->
                Thread { //Convert byte array to bitmap
                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    //Rotate the bitmap
                    val rotatedBitmap: Bitmap?
                    if (mCameraConfig!!.imageRotation != CameraRotation.ROTATION_0) {
                        rotatedBitmap = HiddenCameraUtils.rotateBitmap(
                            bitmap,
                            mCameraConfig!!.imageRotation
                        )

                        bitmap = null
                    } else {
                        rotatedBitmap = bitmap
                    }

                    //Save image to the file.
                    if (HiddenCameraUtils.saveImageFromFile(
                            rotatedBitmap!!,
                            mCameraConfig!!.imageFile!!,
                            mCameraConfig!!.imageFormat
                        )
                    ) {
                        //Post image file to the main thread
                        Handler(Looper.getMainLooper()).post {
                            mCameraCallbacks.onImageCapture(
                                mCameraConfig!!.imageFile!!
                            )
                        }
                    } else {
                        //Post error to the main thread
                        Handler(Looper.getMainLooper()).post {
                            mCameraCallbacks.onCameraError(
                                CameraError.ERROR_IMAGE_WRITE_FAILED
                            )
                        }
                    }

                    mCamera!!.startPreview()
                    this@CameraPreview.isSafeToTakePictureInternal = true
                }.start()
            }
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED)
            isSafeToTakePictureInternal = true
        }
    }

    /**
     * When this function returns, mCamera will be null.
     */
    fun stopPreviewAndFreeCamera() {
        isSafeToTakePictureInternal = false
        if (mCamera != null) {
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
    }
}