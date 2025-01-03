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
package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera.config

import androidx.annotation.IntDef


/**
 * Created by Keval on 12-Nov-16.
 * Supported image format lists.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */
class CameraImageFormat private constructor() {
    init {
        throw RuntimeException("Cannot initialize CameraImageFormat.")
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(*[FORMAT_JPEG, FORMAT_PNG, FORMAT_WEBP])
    annotation class SupportedImageFormat
    companion object {
        /**
         * Image format for .jpg/.jpeg.
         */
        const val FORMAT_JPEG: Int = 849

        /**
         * Image format for .png.
         */
        const val FORMAT_PNG: Int = 545

        /**
         * Image format for .png.
         */
        const val FORMAT_WEBP: Int = 563
    }
}