package com.puzheng.area_investigation.store

import android.content.Context
import android.os.Environment
import com.puzheng.area_investigation.model.isExternalStorageReadable
import com.puzheng.area_investigation.model.isExternalStorageWritable
import java.io.File


fun Context.openReadableFile(dir: String, filename: String) = File(
        (if (isExternalStorageReadable) getExternalFilesDir(Environment.DIRECTORY_PICTURES).absolutePath
        else filesDir.absolutePath) + dir, filename)

fun Context.openWritableFile(dir: String, filename: String): File {
    val absoluteDirPath = (if (isExternalStorageWritable) getExternalFilesDir(Environment.DIRECTORY_PICTURES).absolutePath
    else filesDir.absolutePath) + dir

    val absoluteDir = File(absoluteDirPath)
            if (!absoluteDir.isDirectory) {
                absoluteDir.mkdirs()
            }
    return File(absoluteDirPath, filename)
}