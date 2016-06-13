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
        findView<TextView>(R.id.textViewPOITypeDir).text = POITypeStore.with(this).dir.humanizePath
        findView<TextView>(R.id.textViewPOIDir).text = POIStore.dir.humanizePath
        findView<TextView>(R.id.textViewLogDir).text = LogStore.dir.humanizePath
        findView<TextView>(R.id.textViewBackend).text = ConfigStore.with(this).backend
        AccountStore.with(this).account.let {
            findView<TextView>(R.id.textViewOrgName).text = it?.orgName
            findView<TextView>(R.id.textViewOrgCode).text = it?.orgCode
            findView<TextView>(R.id.textViewUsername).text = it?.username
        }
    }
}
