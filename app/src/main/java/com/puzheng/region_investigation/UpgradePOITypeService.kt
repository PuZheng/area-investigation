package com.puzheng.region_investigation

import android.app.Dialog
import android.app.IntentService
import android.content.Intent
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
        POITypeStore.with(MyApplication.context).upgrade() successUi {
            toBeUpgraded ->
            if (toBeUpgraded.isNotEmpty()) {
                val handler = Handler(Looper.getMainLooper())
                handler.post({
                    object : AppCompatDialogFragment() {
                        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                            return AlertDialog.Builder(MyApplication.currentActivity).setMessage("信息点类型模板已经更新")
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
