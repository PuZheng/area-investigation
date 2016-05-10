package com.puzheng.region_investigation

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.RecyclerView
import nl.komponents.kovenant.ui.successUi

class UploadListActivity : AppCompatActivity() {

    private var uploadService: UploadService? = null

    private val connection: ServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                uploadService = null
            }


            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                uploadService = (service as UploadService.LocalBinder).service
                uploadService!!.uploadList.successUi {
                    (recyclerView.adapter as UploadRecyclerViewAdapter).apply {
                        tasks = it?.map {
                            Triple(it, 0, 0)
                        }
                        notifyDataSetChanged()
                    }

                }
            }

        }
    }

    private val recyclerView: RecyclerView by lazy {
        findViewById(R.id.recyclerView) as RecyclerView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_list)
        recyclerView.adapter = UploadRecyclerViewAdapter()
    }

    private val uploadServiceReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val resultCode = intent?.getIntExtra("resultCode", RESULT_CANCELED);
                if (resultCode == RESULT_OK) {
                    (recyclerView.adapter as UploadRecyclerViewAdapter).apply {
                        if (tasks != null && tasks!!.isNotEmpty())  {
                            val sent = intent!!.getIntExtra("sent", 0)
                            val total = intent!!.getIntExtra("total", 0)
                            val regionId = intent!!.getLongExtra("regionId", 0L)
                            var dropCnt = 0
                            for (task in tasks!!) {
                                if (task.first.id != regionId) {
                                    dropCnt++
                                } else {
                                    break
                                }
                            }
                            tasks = tasks!!.subList(dropCnt, tasks!!.size)
                            if (tasks!!.isNotEmpty()) {
                                tasks = listOf(Triple(tasks!![0].first, sent, total)) + tasks!!.subList(1, tasks!!.size)
                                notifyItemChanged(0)
                            }
                        }
                    }

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
