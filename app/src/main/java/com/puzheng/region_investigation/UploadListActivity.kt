package com.puzheng.region_investigation

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.store.RegionStore
import nl.komponents.kovenant.all
import nl.komponents.kovenant.ui.successUi

class UploadListActivity : AppCompatActivity() {

    private var uploadService: UploadService? = null

    private val regionStore: RegionStore by lazy {
        RegionStore.with(this)
    }

    private val connection: ServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                uploadService = null
            }


            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                uploadService = (service as UploadService.LocalBinder).service
                Logger.v("upload service connected")
                uploadService!!.uploadList.successUi {
                    tasks ->
                    if (tasks != null && tasks.isNotEmpty()) {
                        Logger.v("add tasks $tasks")
                        val adapter = (recyclerView.adapter as UploadRecyclerViewAdapter)
                        // 将region加载进task
                        all(tasks.map {
                            regionStore.get(it.regionId)
                        }) successUi {
                            regions->
                            adapter.tasks = tasks.zip(regions, {
                                task, region ->
                                task.region = region
                                task
                            }).map {
                                Triple(it, 0L, 0L)
                            }
                            adapter.notifyDataSetChanged()
                        }
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
        Logger.init("UploadListActivity")
        setContentView(R.layout.activity_upload_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UploadRecyclerViewAdapter()
    }

    private val uploadServiceReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val resultCode = intent?.getIntExtra("resultCode", RESULT_CANCELED);
                if (resultCode == RESULT_OK) {
                    val sent = intent!!.getLongExtra("sent", 0L)
                    val total = intent!!.getLongExtra("total", 0L)
                    val regionId = intent!!.getLongExtra("regionId", 0L)
                    Logger.v("sent: $sent, total: $total, regionId: $regionId")
                    (recyclerView.adapter as UploadRecyclerViewAdapter).apply {
                        if (tasks != null && tasks!!.isNotEmpty()) {
                            // 删除掉已经完成的任务
                            var dropCnt = 0
                            for (task in tasks!!) {
                                if (task.first.regionId != regionId) {
                                    dropCnt++
                                } else {
                                    break
                                }
                            }
                            tasks = tasks!!.subList(dropCnt, tasks!!.size)
                            if (tasks!!.isNotEmpty()) {
                                tasks = listOf(Triple(tasks!![0].first, sent, total)) + tasks!!.subList(1, tasks!!.size)
                            }
                            notifyDataSetChanged()
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
