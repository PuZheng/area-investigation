package com.puzheng.region_investigation

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.RecyclerView
import com.orhanobut.logger.Logger
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
                Logger.v("service connected")
                uploadService!!.uploadList.successUi {
                    Logger.v(it.toString())
                    //                    (recyclerView.adapter as UploadRecyclerViewAdapter).apply {
                    //                        tasks = it?.map {
                    //                            Triple(it, 0, 0)
                    //                        }
                    //                        notifyDataSetChanged()
                    //                    }

                }
            }

        }
    }

    private val recyclerView: RecyclerView by lazy {
        findViewById(R.id.recyclerView) as RecyclerView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init("UploadListActivity")
        setContentView(R.layout.activity_upload_list)
        recyclerView.adapter = UploadRecyclerViewAdapter()
    }

    private val uploadServiceReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val resultCode = intent?.getIntExtra("resultCode", RESULT_CANCELED);
                if (resultCode == RESULT_OK) {
                    val sent = intent!!.getIntExtra("sent", 0)
                    val total = intent!!.getLongExtra("total", 0)
                    val regionId = intent!!.getLongExtra("regionId", 0L)
                    Logger.v("sent: $sent, total: $total, regionId: $regionId")
                    //                    (recyclerView.adapter as UploadRecyclerViewAdapter).apply {
                    //                        if (tasks != null && tasks!!.isNotEmpty())  {
                    //                            // 删除掉已经完成的任务
                    //                            var dropCnt = 0
                    //                            for (task in tasks!!) {
                    //                                if (task.first.id != regionId) {
                    //                                    dropCnt++
                    //                                } else {
                    //                                    break
                    //                                }
                    //                            }
                    //                            tasks = tasks!!.subList(dropCnt, tasks!!.size)
                    //                            if (tasks!!.isNotEmpty()) {
                    //                                tasks = listOf(Triple(tasks!![0].first, sent, total)) + tasks!!.subList(1, tasks!!.size)
                    //                                if (dropCnt == 0) {
                    //                                    notifyItemChanged(0)
                    //                                } else {
                    //                                    notifyDataSetChanged()
                    //                                }
                    //                            }
                    //                        }
                    //                    }

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
