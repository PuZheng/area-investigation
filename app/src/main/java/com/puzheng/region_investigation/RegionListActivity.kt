package com.puzheng.region_investigation

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.Account
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.AccountStore
import com.puzheng.region_investigation.store.RegionStore
import kotlinx.android.synthetic.main.activity_region_list.*
import kotlinx.android.synthetic.main.content_region_list.*
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.successUi
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RegionListActivity : AppCompatActivity(),
        RegionListFragment.OnRegionListFragmentInteractionListener {


    private var actionMode: ActionMode? = null

    private val regionListFragment: RegionListFragment by lazy {
        findFragmentById<RegionListFragment>(R.id.fragmentRegionList)
    }

    private var uploadService: UploadService? = null

    private val connection: ServiceConnection by lazy {
        object: ServiceConnection {
            override fun onServiceDisconnected(p0: ComponentName?) {
                throw UnsupportedOperationException()
            }

            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                Logger.v("upload service connected")
                uploadService = (p1 as UploadService.LocalBinder).service
            }
        }
    }

    override fun onLongClickItem(region: Region): Boolean {
        if (actionMode != null) {
            return false
        }
        actionMode = startSupportActionMode(object : ModalMultiSelectorCallback(
                (fragmentRegionList as RegionListFragment).multiSelector) {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_trash -> {
                        if (regionListFragment.selectedRegions.isNotEmpty()) {
                            TrashAlertDialogFragment().show(supportFragmentManager, "")
                        } else {
                            toast(R.string.select_at_least_one_region)
                        }
                    }
                    R.id.action_upload -> {
                        uploadService?.upload(regionListFragment.selectedRegions.map { it.id!! })
                    }
//                        RegionStore.with(this@RegionListActivity).sync(regionListFragment.selectedRegions.map { it.id!! }) successUi {
//                            regionListFragment.selectedRegions.forEach {
//                                it.synced = Date()
//                            }
//                            regionListFragment.setupRegions()
//                            toast("synced")
//                        }
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
        var username = intent.getStringExtra("USERNAME")
        var orgCode = intent.getStringExtra("ORG_CODE")
        var orgName = intent.getStringExtra("ORG_NAME")

        if (BuildConfig.DEBUG && username == null) {
            username = "fooUser"
            orgCode = "fooOrgCode"
            orgName = "fooOrgName"
        }
        Logger.i(listOf("username: $username",
                "org name: $orgName",
                "org code: $orgCode").joinToString())
        AccountStore.with(this).account = Account(username, orgCode, orgName)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this@RegionListActivity, UploadService::class.java), connection,
                Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (uploadService != null) {
            unbindService(connection)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_region_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this, InfoActivity::class.java))
            true
        }
        R.id.action_upload_list -> {
            startActivity(Intent(this, UploadListActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // 有两个OnPermissionGranted都要请求REQUEST_WRITE_EXTERNAL_STORAGE权限，那么只要赋予一次权限，就可以允许
        // 这两者都执行callback
        when (requestCode) {
            RegionListFragment.REQUEST_WRITE_EXTERNAL_STORAGE,
            MyApplication.REQUEST_WRITE_EXTERNAL_STORAGE_FOR_LOG ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    (fragmentRegionList as RegionListFragment).onPermissionGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            RegionListFragment.REQUEST_WRITE_EXTERNAL_STORAGE)
                    (application as MyApplication).onPermissionGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            MyApplication.REQUEST_WRITE_EXTERNAL_STORAGE_FOR_LOG)
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
                }).setNegativeButton(R.string.cancel, null)
        return builder.create();
    }
}