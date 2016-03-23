package com.puzheng.area_investigation.model

import android.os.Environment

val isExternalStorageWritable: Boolean
    get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()

val isExternalStorageReadable: Boolean
    get() {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

