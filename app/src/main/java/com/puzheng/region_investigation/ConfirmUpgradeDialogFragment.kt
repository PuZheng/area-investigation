package com.puzheng.region_investigation

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class ConfirmUpgradeDialogFragment : AppCompatDialogFragment() {

    companion object {
        const private val CURRENT_VERSION = "CURRENT_VERSION"
        const private val LATEST_VERSION = "LATEST_VERSION"
        const private val PATH = "PATH"

        fun newInstance(currentVersion: String, latestVersion: String) = ConfirmUpgradeDialogFragment().apply {
            arguments = Bundle().apply {
                // if you see lint error, refer https://youtrack.jetbrains.com/issue/KT-12015
                putString(CURRENT_VERSION, currentVersion)
                putString(LATEST_VERSION, latestVersion)
            }
        }
    }

    lateinit private var currentVersion: String
    lateinit private var latestVersion: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            currentVersion = arguments.getString(CURRENT_VERSION)
            latestVersion = arguments.getString(LATEST_VERSION)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context).setMessage("发现新的版本$latestVersion(当前版本为$currentVersion), 是否需要升级?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, {
                    dialog, which ->
                    MaskDialogFragment.newInstance("下载新版本").let {
                        mask ->
                        val handler = Handler(Looper.getMainLooper())
                        mask.show(fragmentManager, "")
                        UpgradeUtil.with(context).download(latestVersion) {
                            downloaded, total ->
                            if (total != 0L) {
                                 handler.post {
                                    mask.progress(downloaded, total)
                                }
                            }
                        } successUi {
                            apkFile ->
                            // 这个fragment可能没有绑定
                            (activity ?: MyApplication.currentActivity).startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK; // without this flag android returned a intent error!
                            })
                        } failUi {
                            it.printStackTrace()
                            (activity ?: MyApplication.currentActivity).toast("下载失败: ${it.message}")
                        } alwaysUi {
                            mask.dismiss()
                        }
                    }
                }).create()
    }
}
