package com.puzheng.region_investigation

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager

class UploadListActivity : AppCompatActivity() {

    private var uploadService: UploadService? = null

    private val connection: ServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                uploadService = null
            }


            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                uploadService = (service as UploadService.LocalBinder).service
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_list)
    }

    private val uploadServiceReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val resultCode = intent?.getIntExtra("resultCode", RESULT_CANCELED);
                if (resultCode == RESULT_OK) {
                    val progress = intent?.getFloatExtra("progress", 0.0F);
                    toast(progress.toString())
                }
            }

        }

    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, UploadService::class.java), connection,
                Context.BIND_AUTO_CREATE)
        val filter = IntentFilter(UploadService.PROGRESS);
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadServiceReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        if (connection != null) {
            unbindService(connection)
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadServiceReceiver)
    }
}
