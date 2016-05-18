package com.puzheng.region_investigation

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.store.LogStore
import com.puzheng.region_investigation.store.POIStore
import com.puzheng.region_investigation.store.POITypeStore
import java.io.File

class InfoActivity : AppCompatActivity() {


    private fun humanizePath(file: File) = File("存储卡", file.relativeTo(Environment.getExternalStoragePublicDirectory("")).path).path

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findView<TextView>(R.id.textViewPOITypeDir).text = humanizePath(POITypeStore.with(this).dir)
        findView<TextView>(R.id.textViewPOIDir).text = humanizePath(POIStore.dir)
        findView<TextView>(R.id.textViewLogDir).text = humanizePath(LogStore.dir)
        findView<TextView>(R.id.textViewUploadTo).text = ConfigStore.with(this).uploadBackend
    }
}
