package com.puzheng.region_investigation

import android.app.Application
import android.content.Context
import com.puzheng.region_investigation.store.LogStore
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Configure Kovenant with standard dispatchers
        // suitable for an Android environment.
        // It's just convenience, you can still use
        // `Kovenant.configure { }` if you want to keep
        // matters in hand.
        startKovenant()
        MyApplication.context = applicationContext
        eventLogger = Logger.getLogger("com.puzheng.region_investigation.event")
        eventLogger.addHandler(FileHandler(LogStore.dir.absolutePath + "/%u.log", true).apply {
            formatter = EventLogFormatter()
        })
    }

    override fun onTerminate() {
        super.onTerminate()
        // Dispose of the Kovenant thread pools.
        // For quicker shutdown you could use
        // `force=true`, which ignores all current
        // scheduled tasks
        stopKovenant()
    }

    companion object {
        lateinit var context: Context
        lateinit var eventLogger: Logger
    }
}

private class EventLogFormatter: Formatter() {

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private val JSON_SEP = "------------------"


    override fun format(record: LogRecord?): String? {
        var s = "${record!!.level.name}: [${fmt.format(Date())}] - ${record.message}${System.lineSeparator()}"
        if (record.parameters != null && record.parameters.size > 0) {
            s += JSON_SEP + System.lineSeparator()
            s += (record.parameters[0] as JSONObject).toString(4) + System.lineSeparator()
            s += JSON_SEP + System.lineSeparator()
        }
        return s
    }

}