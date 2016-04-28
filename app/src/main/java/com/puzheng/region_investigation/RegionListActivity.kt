package com.puzheng.region_investigation

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.RegionStore
import kotlinx.android.synthetic.main.activity_region_list.*
import kotlinx.android.synthetic.main.content_region_list.*
import nl.komponents.kovenant.ui.successUi
import java.util.*

class RegionListActivity : AppCompatActivity(),
        RegionListFragment.OnRegionListFragmentInteractionListener {


    private var actionMode: ActionMode? = null

    private val regionListFragment: RegionListFragment by lazy {
        findFragmentById<RegionListFragment>(R.id.fragmentRegionList)
    }


    override fun onLongClickItem(region: Region): Boolean {
        if (actionMode != null) {
            return false
        }

        actionMode = startSupportActionMode(object : ModalMultiSelectorCallback((fragmentRegionList as RegionListFragment).multiSelector) {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_trash ->
                        TrashAlertDialogFragment().show(supportFragmentManager, "")
                    R.id.action_upload ->
                        RegionStore.with(this@RegionListActivity).sync(regionListFragment.selectedRegions.map { it.id!! }) successUi {
                            regionListFragment.selectedRegions.forEach {
                                it.synced = Date()
                            }
                            regionListFragment.setupRegions()
                            toast("synced")
                        }
                }
                return false
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                super.onCreateActionMode(mode, menu)
                mode?.menuInflater?.inflate(R.menu.context_menu_region_list, menu);
                fab.visibility = View.GONE
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                multiSelector.clearSelections()
                multiSelector.isSelectable = false
                fab.visibility = View.VISIBLE
                actionMode = null;
            }

        });
        return true;
    }

    override fun onClickItem(region: Region) {
        startActivity(Intent(this, EditRegionActivity::class.java).apply {
            putExtra(TAG_REGION, region)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init("RegionListActivity")
        setContentView(R.layout.activity_region_list)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            startActivity(Intent(this, CreateRegionActivity::class.java))
        }
        Logger.i(listOf("username: ${intent.getStringExtra("USERNAME")}",
                "org name: ${intent.getStringExtra("ORG_NAME")}",
                "org code: ${intent.getStringExtra("ORG_CODE")}").joinToString())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_region_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
//            startActivity(Intent(this, ))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RegionListFragment.REQUEST_WRITE_EXTERNAL_STORAGE ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    (fragmentRegionList as RegionListFragment).onPermissionGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            RegionListFragment.REQUEST_WRITE_EXTERNAL_STORAGE)
                } else {
                    toast("why not fake some poi types?")
                }
        }
    }

    companion object {
        val TAG_REGION = "REGION"
    }
}

private class TrashAlertDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(R.string.warning).setMessage(R.string.trash_confirm_msg)
                .setPositiveButton(R.string.confirm, {
                    dialog, v ->
                    val fragment = (activity as RegionListActivity).fragmentRegionList as RegionListFragment
                    fragment.removeSelectedRegions()
                    fragment.multiSelector.clearSelections()
                }).setNegativeButton(R.string.cancel, null)
        // Create the AlertDialog object and return it
        return builder.create();
    }
}