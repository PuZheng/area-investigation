package com.puzheng.area_investigation

import android.content.Context
import android.os.Environment
import com.puzheng.area_investigation.isExternalStorageReadable
import com.puzheng.area_investigation.isExternalStorageWritable
import java.io.File

fun Context.openReadableFile(dir: String, type: String? = null) = File(
        (if (isExternalStorageReadable) getExternalFilesDir(type).absolutePath
        else filesDir.absolutePath) + dir)

fun Context.openReadableFile(dir: String, filename: String, type: String? = null) = File(
        (if (isExternalStorageReadable) getExternalFilesDir(type).absolutePath
        else filesDir.absolutePath) + dir, filename)

fun Context.openWritableFile(dir: String, filename: String, type: String? = null): File {
    val absoluteDirPath = (if (isExternalStorageWritable) getExternalFilesDir(type).absolutePath
    else filesDir.absolutePath) + dir

    val absoluteDir = File(absoluteDirPath)
            if (!absoluteDir.isDirectory) {
                absoluteDir.mkdirs()
            }

    return File(absoluteDirPath, filename)
}