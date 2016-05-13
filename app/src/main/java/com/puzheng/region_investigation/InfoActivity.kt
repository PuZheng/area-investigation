package com.puzheng.region_investigation

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.puzheng.region_investigation.store.LogStore
import com.puzheng.region_investigation.store.POIStore
import com.puzheng.region_investigation.store.POITypeStore

class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findView<TextView>(R.id.textViewPOITypeDir).text = POITypeStore.with(this).dir.absolutePath
        findView<TextView>(R.id.textViewPOIDir).text = POIStore.dir.absolutePath
        findView<TextView>(R.id.textViewLogDir).text = LogStore.dir.absolutePath
        findView<TextView>(R.id.textViewUploadTo).text = ConfigStore.with(this).uploadBackend
    }
}
