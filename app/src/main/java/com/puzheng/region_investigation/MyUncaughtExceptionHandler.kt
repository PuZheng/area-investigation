package com.puzheng.region_investigation

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException
import java.util.*

class MyUncaughtExceptionHandler(val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultUEH: Thread.UncaughtExceptionHandler

    init {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        var arr = e.stackTrace
        var report = e.toString() + "\n\n"
        report += "--------- (${Date()}) Stack trace ---------\n\n"
        for (i in arr.indices) {
            report += "    " + arr[i].toString() + "\n"
        }
        report += "-------------------------------\n\n"

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        report += "--------- Cause ---------\n\n"
        val cause = e.cause
        if (cause != null) {
            report += cause.toString() + "\n\n"
            arr = cause.stackTrace
            for (i in arr.indices) {
                report += "    " + arr[i].toString() + "\n"
            }
        }
        report += "-------------------------------\n\n"

        try {
            File(Environment.getExternalStoragePublicDirectory(context.packageName), "crash.txt").appendText(report)
        } catch (ioe: IOException) {
            // ...
        }

        defaultUEH.uncaughtException(t, e)
    }
}
