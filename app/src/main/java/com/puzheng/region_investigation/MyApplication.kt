package com.puzheng.region_investigation

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.puzheng.region_investigation.store.LogStore
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import nl.komponents.kovenant.ui.successUi
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.*
import java.util.logging.Formatter

class MyApplication : Application(), OnPermissionGrantedListener {

    override fun onPermissionGranted(permission: String, requestCode: Int) {
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_FOR_LOG) {
            eventLogger.addHandler(FileHandler(LogStore.dir.absolutePath + "/%u.log", true).apply {
                formatter = EventLogFormatter()
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Configure Kovenant with standard dispatchers
        // suitable for an Android environment.
        // It's just convenience, you can still use
        // `Kovenant.configure { }` if you want to keep
        // matters in hand.
        startKovenant()
        MyApplication.context = applicationContext
        startService(Intent(this, UpgradePOITypeService::class.java))
        eventLogger = Logger.getLogger("com.puzheng.region_investigation.event")
        registerActivityLifecycleCallbacks(object: Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {

            }

            override fun onActivityStarted(activity: Activity?) {
            }

            override fun onActivityDestroyed(activity: Activity?) {
            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
            }

            override fun onActivityStopped(activity: Activity?) {
            }

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                MyApplication.currentActivity = activity as AppCompatActivity
                if (eventLogger.handlers.isEmpty()) {
                    (activity as AppCompatActivity).assertPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            REQUEST_WRITE_EXTERNAL_STORAGE_FOR_LOG) successUi {
                        onPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                REQUEST_WRITE_EXTERNAL_STORAGE_FOR_LOG)
                    }
                }
            }

            override fun onActivityResumed(activity: Activity?) {
            }

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
        lateinit var currentActivity: AppCompatActivity
        val REQUEST_WRITE_EXTERNAL_STORAGE_FOR_LOG = AtomicInteger().andDecrement
    }
}

private class EventLogFormatter: Formatter() {

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private val JSON_SEP = "------------------"


    override fun format(record: LogRecord?): String? {
        var s = "${record!!.level.name}: [${fmt.format(Date())}] - ${record.message}\n"
        if (record.parameters != null && record.parameters.size > 0) {
            s += JSON_SEP + "\n"
            s += (record.parameters[0] as JSONObject).toString(4) + "\n"
            s += JSON_SEP + "\n"
        }
        return s
    }

}