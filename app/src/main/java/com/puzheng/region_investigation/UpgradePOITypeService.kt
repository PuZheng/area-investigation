package com.puzheng.region_investigation

import android.app.Dialog
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.puzheng.region_investigation.store.POITypeStore
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi


class UpgradePOITypeService : IntentService("UPGRADE_POI_TYPE_SERVICE") {
    override fun onHandleIntent(intent: Intent) {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = manager.activeNetworkInfo
        if (info == null || !info.isAvailable) {
            return
        }
        POITypeStore.with(MyApplication.context).upgrade() successUi {
            val toBeUpgraded = it.first
            val toBeDeleted = it.second
            if (toBeUpgraded.isNotEmpty() || toBeDeleted.isNotEmpty()) {
                val handler = Handler(Looper.getMainLooper())
                handler.post({
                    object : AppCompatDialogFragment() {
                        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                            val msg = (if (toBeUpgraded.isNotEmpty()) {
                                "信息点类型(" + toBeUpgraded.map {
                                it["name"]
                                }.joinToString(",") + ")模板已经更新"
                            } else "") + (if (toBeDeleted.isNotEmpty()) {
                                "信息点类型(" + toBeDeleted.map { it.name }.joinToString(",") + ")模板已经删除" } else "")
                            return AlertDialog.Builder(MyApplication.currentActivity).setMessage(msg)
                                    .setPositiveButton("知道了", null)
                                    .create()
                        }
                    }.show(MyApplication.currentActivity.supportFragmentManager, "")
                })
            }
        } failUi {
            it.printStackTrace()
            toast("更新信息点模板失败!")
        }
    }
}
