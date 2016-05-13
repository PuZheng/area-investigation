package com.puzheng.region_investigation.store

import android.content.Context
import android.os.Environment
import com.puzheng.region_investigation.ConfigStore
import com.puzheng.region_investigation.MyApplication
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogStore private constructor(val context: Context) {
    companion object {

        const private val DEFAULT_LOG_FILE_TIME_SPAN = 3600 * 24

        val dir = File(Environment.getExternalStoragePublicDirectory(MyApplication.context.packageName),
                "logs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
        private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        fun with(context: Context) = LogStore(context)
    }

    private val logFile: File? = null
        get() {
            if (field == null) {
                field = File(dir, "logs.txt")
                if (!field.exists()) {
                    field.createNewFile()
                }
            }
            if (field.phasedOut) {
                field.archive()
                field = File(dir, "logs.txt").apply {
                    createNewFile()
                }
            }
            return field
        }

    fun log(type: String, message: String, detail: JSONObject? = null) {
        logFile!!.bufferedWriter().apply {
            write("[${fmt.format(Date())}]-$type-$message")
            newLine()
            if (detail != null) {
                write(detail.toString(4))
            }
            newLine()
            flush()
            close()
        }
    }

    // Mr. Extensions
    private fun File.archive() {

    }

    private val File.phasedOut: Boolean
        get() = true
//        get() = lastModified() < (24 * 3600) /
}