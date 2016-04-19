package com.puzheng.region_investigation

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment

class ConfirmRemovePOIDialogFragment(val afterConfirm: () -> Unit) : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity).setTitle(R.string.confirm_remove_poi)
                .setPositiveButton(R.string.confirm, {
                    dialog, which ->
                    afterConfirm()
                }).setNegativeButton(R.string.cancel, null).create()
    }
}