package com.puzheng.region_investigation

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment

class ConfirmUpgradeDialogFragment : AppCompatDialogFragment() {

    companion object {
        const private val CURRENT_VERSION = "CURRENT_VERSION"
        const private val LATEST_VERSION = "LATEST_VERSION"
        const private val PATH = "PATH"

        fun newInstance(currentVersion: String, latestVersion: String, path: String) = ConfirmUpgradeDialogFragment().apply {
            arguments = Bundle().apply {
                // if you see lint error, refer https://youtrack.jetbrains.com/issue/KT-12015
                putString(CURRENT_VERSION, currentVersion)
                putString(LATEST_VERSION, latestVersion)
                putString(PATH, path)
            }
        }
    }

    lateinit private var currentVersion: String
    lateinit private var latestVersion: String
    lateinit private var path: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            currentVersion = arguments.getString(CURRENT_VERSION)
            latestVersion = arguments.getString(LATEST_VERSION)
            path = arguments.getString(PATH)
        }

    }

    lateinit private var listener: OnFragmentInteractionListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    interface OnFragmentInteractionListener {
        fun onConfirmUpgrade(latestVersion: String, path: String);
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context).setMessage("发现新的版本$latestVersion(当前版本为$currentVersion), 是否需要升级?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, {
                    dialog, which ->
                    listener.onConfirmUpgrade(latestVersion, path)
                }).create()
    }
}
