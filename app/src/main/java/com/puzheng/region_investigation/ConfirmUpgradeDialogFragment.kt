package com.puzheng.region_investigation

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment

class ConfirmUpgradeDialogFragment(val currentVersion: String, val latestVersion: String): AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context).setMessage("发现新的版本$latestVersion(当前版本为$currentVersion), 是否需要升级?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, {
                    dialog, which ->
                }).create()
    }
}
