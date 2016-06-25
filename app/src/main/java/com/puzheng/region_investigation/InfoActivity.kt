package com.puzheng.region_investigation

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.store.AccountStore
import com.puzheng.region_investigation.store.LogStore
import com.puzheng.region_investigation.store.POIStore
import com.puzheng.region_investigation.store.POITypeStore
import java.io.File

class InfoActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fun findTextViewById(id: Int) = findViewById(id) as TextView
        findTextViewById(R.id.textViewPOITypeDir).text = POITypeStore.with(this).dir.humanizePath
        findTextViewById(R.id.textViewPOIDir).text = POIStore.with(this).dir.humanizePath
        findTextViewById(R.id.textViewLogDir).text = LogStore.dir.humanizePath
        findTextViewById(R.id.textViewBackend).text = ConfigStore.with(this).backend
        findTextViewById(R.id.textViewOfflineMapData).text = "存储卡/autonavi/data/vmap"
        AccountStore.with(this).account.let {
            findTextViewById(R.id.textViewOrgName).text = it?.orgName
            findTextViewById(R.id.textViewOrgCode).text = it?.orgCode
            findTextViewById(R.id.textViewUsername).text = it?.username
        }
    }
}
